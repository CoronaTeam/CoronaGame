package ch.epfl.sdp.contamination;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import ch.epfl.sdp.Account;

import static ch.epfl.sdp.contamination.CachingDataSender.privateUserFolder;
import static ch.epfl.sdp.contamination.CachingDataSender.publicUserFolder;
import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;

public class ConcreteDataReceiver implements DataReceiver {


    private GridFirestoreInteractor interactor;

    public ConcreteDataReceiver(GridFirestoreInteractor gridInteractor) {
        this.interactor = gridInteractor;
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public CompletableFuture<Set<Carrier>> getUserNearby(Location location, Date date) {

        // TODO: In this function, use Neighbor instead than Carrier and set uniqueId too

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
        return interactor.getTimes(location)
                .thenApply(stringMapMap -> filterValidTimes(startDate.getTime(), endDate.getTime(), stringMapMap))
                .thenApply(filtered -> {
                    // TODO: Debug code
                    if (filtered.size() > 0) {
                        Log.e("FILTERED_TIMES", Long.toString(filtered.iterator().next()));
                    } else {

                    }
                    return filtered;
                })
                .thenCompose(validTimes -> {
                    List<CompletableFuture<Map<String, Map<String, Object>>>> metDuringSlices = new ArrayList<>();

                    validTimes.forEach(tm -> metDuringSlices.add(interactor.gridRead(location, tm)));

                    return CompletableFuture.allOf(metDuringSlices.toArray(new CompletableFuture[metDuringSlices.size()]))
                            .thenApply(ignoredVoid -> {
                                Stream<Map<String, Map<String, Object>>> results = metDuringSlices.stream().map(ft -> ft.join());

                                Map<Carrier, Integer> metDuringInterval = new HashMap<>();

                                results.forEach(res -> {
                                    for (Map.Entry<String, Map<String, Object>> doc : res.entrySet()) {
                                        Carrier c = new Neighbor(
                                                Enum.valueOf(Carrier.InfectionStatus.class,
                                                        (String) doc.getValue().get("infectionStatus")),
                                                ((float) ((double) doc.getValue().get("illnessProbability"))),
                                                (String) doc.getValue().get("uniqueId"));

                                        // TODO: Use Neighbor when appropriate

                                        // TODO: Debug log
                                        Log.e("MET_DURING_INTERVAL", c.getInfectionStatus() + ", " + c.getIllnessProbability());

                                        int numberOfMeetings = 1;
                                        if (metDuringInterval.containsKey(c)) {
                                            numberOfMeetings += metDuringInterval.get(c);
                                        }
                                        metDuringInterval.put(c, numberOfMeetings);
                                    }
                                });

                                return metDuringInterval;
                            });
                })
                .exceptionally(exception -> Collections.emptyMap());
    }

    @Override
    public CompletableFuture<Location> getMyLastLocation(Account account) {
        return interactor.readLastLocation(account).thenApply(result -> {
            if (result.entrySet().iterator().hasNext()) {
                GeoPoint geoPoint = (GeoPoint)result.get(GEOPOINT_TAG);
                Location location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(geoPoint.getLatitude());
                location.setLongitude(geoPoint.getLongitude());
                return location;
            }else{
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, Object>> getNumberOfSickNeighbors(String userId){
        DocumentReference ref = documentReference(publicUserFolder, userId);
        return interactor.readDocument(ref);
    }
    public CompletableFuture<Map<String, Object>> getRecoveryCounter(String userId){
        return interactor.readDocument(documentReference(privateUserFolder, userId));
    }
}
