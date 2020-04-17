package ch.epfl.sdp.contamination;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;
import static ch.epfl.sdp.contamination.CachingDataSender.publicUserFolder;

class ConcreteDataReceiver implements DataReceiver {

    private GridFirestoreInteractor interactor;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    ConcreteDataReceiver(GridFirestoreInteractor gridInteractor) {
        this.interactor = gridInteractor;
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback) {

        QueryHandler nearbyHandler = new QueryHandler<QuerySnapshot>() {

            @Override
            public void onSuccess(QuerySnapshot snapshot) {

                Set<Carrier> carriers = new HashSet<>();

                for (QueryDocumentSnapshot q : snapshot) {
                    carriers.add(new Layman(Enum.valueOf(Carrier.InfectionStatus.class,(String) q.get("infectionStatus")), ((float)((double)q.get("illnessProbability")))));
                }

                callback.onCallback(carriers);
            }

            @Override
            public void onFailure() {

                callback.onCallback(Collections.EMPTY_SET);
            }
        };

        interactor.read(location, date.getTime(), nearbyHandler);
    }

    private Set<Long> filterValidTimes(long startDate, long endDate, QuerySnapshot snapshot) {
        Set<Long> validTimes = new HashSet<>();

        for (QueryDocumentSnapshot q : snapshot) {
            long time = Long.decode((String)q.get("Time"));
            if (startDate <= time && time <= endDate) {
                validTimes.add(time);
            }
        }

        return validTimes;
    }

    private class SliceQueryHandle implements QueryHandler<QuerySnapshot> {

        private Map<Carrier, Integer> metDuringInterval;
        private AtomicInteger done;
        private Set<Long> validTimes;
        private Callback<Map<? extends Carrier, Integer>> callback;

        SliceQueryHandle(Set<Long> validTimes, Map<Carrier, Integer> metDuringInterval, AtomicInteger done, Callback<Map<? extends Carrier, Integer>> callback){
            this.metDuringInterval = metDuringInterval;
            this.done = done;
            this.validTimes = validTimes;
            this.callback = callback;
        }

        private void launchCallback() {
            int size = validTimes.size();
            boolean elected = true;

            done.incrementAndGet();
            if (done.get() == size) {
                while (!done.compareAndSet(size, 0)) {
                    elected = (done.get() != 0);
                }
                if (elected) {
                    callback.onCallback(metDuringInterval);
                }
            }
        }

        @Override
        public void onSuccess(QuerySnapshot snapshot) {
            for (QueryDocumentSnapshot q : snapshot) {

                Carrier c = new Layman(
                        Enum.valueOf(Carrier.InfectionStatus.class,(String) q.get("infectionStatus")),
                        ((float)((double)q.get("illnessProbability"))));

                int numberOfMeetings = 1;
                if (metDuringInterval.containsKey(c)) {
                    numberOfMeetings += metDuringInterval.get(c);
                }
                metDuringInterval.put(c, numberOfMeetings);
            }

            launchCallback();
        }

        @Override
        public void onFailure() {
            // Do nothing
        }
    }

    @Override
    public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {

        Set<Carrier> carriers = new HashSet<>();

        interactor.getTimes(location, new QueryHandler<QuerySnapshot>() {

            @Override
            public void onSuccess(QuerySnapshot snapshot) {

                Set<Long> validTimes = filterValidTimes(startDate.getTime(), endDate.getTime(), snapshot);

                Map<Carrier, Integer> metDuringInterval = new ConcurrentHashMap<>();

                AtomicInteger done = new AtomicInteger();

                QueryHandler updateFromTimeSlice = new SliceQueryHandle(validTimes, metDuringInterval, done, callback);

                for (long t : validTimes) {
                    interactor.read(location, t, updateFromTimeSlice);
                }

                // If there are not valid times, just start the callback with an empty map
                if (validTimes.isEmpty()) {
                    callback.onCallback(metDuringInterval);
                }
            }

            @Override
            public void onFailure() {
                callback.onCallback(Collections.EMPTY_MAP);
            }
        });

    }

    @Override
    public void getMyLastLocation(Account account, Callback<Location> callback) {
        interactor.readLastLocation(account, new Callback<QuerySnapshot>() {
            @Override
            public void onCallback(QuerySnapshot snapshot) {
                if (snapshot.iterator().hasNext()) {
                    GeoPoint geoPoint = (GeoPoint) snapshot.iterator().next().get("geoPoint");
                    Location location = new Location(LocationManager.GPS_PROVIDER);
                    location.setLatitude(geoPoint.getLatitude());
                    location.setLongitude(geoPoint.getLongitude());
                    callback.onCallback(location);
                }
            }
        });
    }
//    //USELESS
//    private void resetSickNeighbors(String userId){
//        DocumentReference ref = FirestoreInteractor.documentReference(publicUserFolder,userId);
//        ref.update(publicAlertAttribute, 0);
//    }
//    //USELESS
//    public int getAndResetSickNeighbors(String userId){//,Callback<Map<String,Object>> callback){
//        AtomicInteger temp = new AtomicInteger(-1);
//        final HashMap<String,Object> map;
//
//            interactor.readDocument(publicUserFolder, userId, res -> {
//                    if(!((HashMap)(res)).isEmpty()){
//                        temp.set((int) ((long) (((HashMap) (res)).get(publicAlertAttribute))));
//                    }else{
//                        temp.set(0);
//                    }
//                }
//             );
//
//        try{
//            Thread.sleep(5000);
//        }catch (InterruptedException e){
//
//        }
//        resetSickNeighbors(userId);
//        try{
//            Thread.sleep(5000);
//        }catch (InterruptedException e){
//
//        }
//        return temp.get();
//    }



    public void getSickNeighbors(String userId,Callback callback){
        interactor.readDocument(publicUserFolder, userId, callback);
    }
//    public Future<Integer> getAndResetSickNeighbors(String userId){//,Callback<Map<String,Object>> callback){
//        AtomicInteger temp = new AtomicInteger(-1);
//
//        String path = "publicPlayers/";
//        Future<Integer> futur = executor.submit(()->{interactor.readDocument(path,userId,res ->
//                return (int)res);
////                temp.set((int)res));
////        return temp.get();
//        });
//        interactor.readDocument(path,userId,res ->
//                temp.set((int)res));
//        DocumentReference ref = FirestoreInteractor.documentReference(path,userId);
//        ref.update("lastMetPerson", FieldValue.delete());
//        return temp.get();
//    }
//    public int getAndResetSickNeighbors(String userId,Callback<Integer> callback){
//        AtomicInteger temp = new AtomicInteger(-1);
//
//        interactor.readDocument(publicUserFolder,userId,callback);
//        DocumentReference ref = FirestoreInteractor.documentReference(publicUserFolder,userId);
//        ref.update(publicAlertAttribute, FieldValue.delete());
//        return temp.get();
//    }
}
