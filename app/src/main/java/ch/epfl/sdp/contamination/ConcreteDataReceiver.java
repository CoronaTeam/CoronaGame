package ch.epfl.sdp.contamination;

import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;

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
    public CompletableFuture<Map<Carrier, Integer>> getUserNearbyDuring(Location location,
                                                                       Date startDate, Date endDate) {
        return interactor.getTimes(location).thenApply(stringMapMap -> {
            Set<Long> validTimes = filterValidTimes(startDate.getTime(), endDate.getTime(), stringMapMap);
            Map<Carrier, Integer> metDuringInterval = new ConcurrentHashMap<>();
            AtomicInteger done = new AtomicInteger();

            for (long t : validTimes) {
                interactor.gridRead(location, t).thenApply(result -> {
                    for (Map.Entry<String, Map<String, Object>> doc : result.entrySet()) {
                        Carrier c = new Layman(
                                Enum.valueOf(Carrier.InfectionStatus.class,
                                        (String) doc.getValue().get("infectionStatus")),
                                ((float) ((double) doc.getValue().get("illnessProbability"))));

                        int numberOfMeetings = 1;
                        if (metDuringInterval.containsKey(c)) {
                            numberOfMeetings += metDuringInterval.get(c);
                        }
                        metDuringInterval.put(c, numberOfMeetings);
                    }

                    int size = validTimes.size();
                    boolean elected = true;

                    done.incrementAndGet();
                    if (done.get() == size) {
                        while (!done.compareAndSet(size, 0)) {
                            elected = (done.get() != 0);
                        }
                        if (elected) {
                            return metDuringInterval;
                        }
                    }
                    return null;
                });
            }

            // If there are not valid times, just start the callback with an empty map
            if (validTimes.isEmpty()) {
                return metDuringInterval;
            }else {
                return new HashMap<Carrier, Integer>();
            }
        }).exceptionally(exception -> Collections.emptyMap());
    }

    @Override
    public CompletableFuture<Location> getMyLastLocation(Account account) {
        return interactor.readLastLocation(account).thenApply(result -> {
            if (result.entrySet().iterator().hasNext()) {
                GeoPoint geoPoint = (GeoPoint)result.get("geoPoint");
                Location location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(geoPoint.getLatitude());
                location.setLongitude(geoPoint.getLongitude());
                return location;
            }else{
                return null;
            }
        });
    }
}
