package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.fragment.AccountFragment;

import static ch.epfl.sdp.AuthenticationManager.getActivity;
import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;

public class ConcreteCachingDataSender implements CachingDataSender {
    SortedMap<Date, Location> lastPositions;
    private GridFirestoreInteractor gridInteractor;
    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
        this.lastPositions = new TreeMap<>();
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
        refreshLastPositions(time, location);
        Map<String, Object> element = new HashMap<>();
        element.put("geoPoint", new GeoPoint(location.getLatitude(), location.getLongitude()));
        element.put("timeStamp", time.getTime());
        element.put("infectionStatus", carrier.getInfectionStatus());
        CompletableFuture<Void> future1 = gridInteractor.writeDocumentWithID(
                documentReference("LastPositions", AccountFragment.getAccount(getActivity()).getId()),
                element);
        CompletableFuture<Void> future2 = gridInteractor.gridWrite(location, String.valueOf(time.getTime()), carrier);
        return CompletableFuture.allOf(future1, future2);
    }

    /**
     * removes every locations older than UNINTENTIONAL_CONTAGION_TIME ms and adds a new position
     */
    private void refreshLastPositions(Date time, Location location) {
        Date oldestDate = new Date(time.getTime() - MAX_CACHE_ENTRY_AGE);
        lastPositions.headMap(oldestDate).clear();
        if (location != null) {
            lastPositions.put(time, location);
        }
    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {
        refreshLastPositions(new Date(System.currentTimeMillis()), null);
        return Collections.unmodifiableSortedMap(lastPositions);
    }
}
