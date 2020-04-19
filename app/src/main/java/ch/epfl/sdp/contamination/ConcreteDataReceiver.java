package ch.epfl.sdp.contamination;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.firestore.QueryHandler;

class ConcreteDataReceiver implements DataReceiver {

    private GridFirestoreInteractor interactor;

    ConcreteDataReceiver(GridFirestoreInteractor gridInteractor) {
        this.interactor = gridInteractor;
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public CompletableFuture<Set<Carrier>> getUserNearby(Location location, Date date) {

        return interactor.gridRead(location, date.getTime()).thenApply(stringMapMap -> {
            Set<Carrier> carriers = new HashSet<>();
            for (Map.Entry<String, Map<String, Object>> doc : stringMapMap.entrySet()) {
                carriers.add((Carrier) doc);
                carriers.add(new Layman(Enum.valueOf(Carrier.InfectionStatus.class,
                        (String) doc.getValue().get("infectionStatus")),
                        ((float) ((double) doc.getValue().get(
                                "illnessProbability")))));
            }
            return carriers;
        }).exceptionally(exception -> Collections.emptySet());
    }

    private Set<Long> filterValidTimes(long startDate, long endDate, Map<String, Map<String, Object>> snapshot) {
        Set<Long> validTimes = new HashSet<>();
        for (Map.Entry<String, Map<String, Object>> q : snapshot.entrySet()) {
            long time = Long.decode((String) q.getValue().get("Time"));
            if (startDate <= time && time <= endDate) {
                validTimes.add(time);
            }
        }
        return validTimes;
    }

    @Override
    public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {

        Set<Carrier> carriers = new HashSet<>();

        interactor.getTimes(location).thenApply(stringMapMap -> {
            Set<Long> validTimes = filterValidTimes(startDate.getTime(), endDate.getTime(), stringMapMap);

            Map<Carrier, Integer> metDuringInterval = new ConcurrentHashMap<>();

            AtomicInteger done = new AtomicInteger();

            QueryHandler updateFromTimeSlice = new SliceQueryHandle(validTimes, metDuringInterval, done, callback);

            for (long t : validTimes) {
                interactor.gridRead(location, t);
            //TODO: updateFromTimeSlice
            }

            // If there are not valid times, just start the callback with an empty map
            if (validTimes.isEmpty()) {
                callback.onCallback(metDuringInterval);
            }

        }).exceptionally(exception -> Collections.emptyMap());
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

    private class SliceQueryHandle implements QueryHandler<QuerySnapshot> {

        private Map<Carrier, Integer> metDuringInterval;
        private AtomicInteger done;
        private Set<Long> validTimes;
        private Callback<Map<? extends Carrier, Integer>> callback;

        SliceQueryHandle(Set<Long> validTimes, Map<Carrier, Integer> metDuringInterval, AtomicInteger done, Callback<Map<? extends Carrier, Integer>> callback) {
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
                        Enum.valueOf(Carrier.InfectionStatus.class, (String) q.get("infectionStatus")),
                        ((float) ((double) q.get("illnessProbability"))));

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
}
