package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import ch.epfl.sdp.fragment.AccountFragment;

import static ch.epfl.sdp.AuthenticationManager.getActivity;
import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.INFECTION_STATUS_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_DOC;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMESTAMP_TAG;

/**
 * Thread-safe implementation of a DataSender with a cache
 */
public class ConcreteCachingDataSender implements CachingDataSender {

    SortedMap<Date, Location> lastPositions;
    private GridFirestoreInteractor gridInteractor;

    private ReentrantLock lock;

    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
        this.lastPositions = new TreeMap<>();

        lock = new ReentrantLock();
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {

        lock.lock();

        CompletableFuture<Void> lastPositionsFuture, gridWriteFuture;

        try {
            refreshLastPositions(time, location);

            Map<String, Object> element = new HashMap<>();
            element.put(GEOPOINT_TAG, new GeoPoint(
                    location.getLatitude(),
                    location.getLongitude()
            ));
            element.put(TIMESTAMP_TAG, time.getTime());
            element.put(INFECTION_STATUS_TAG, carrier.getInfectionStatus());

            lastPositionsFuture = gridInteractor.writeDocumentWithID(
                    documentReference(LAST_POSITIONS_DOC, AccountFragment.getAccount(getActivity()).getId()), element);

            gridWriteFuture = gridInteractor.gridWrite(location, String.valueOf(time.getTime()), carrier);
        } finally {
            lock.unlock();
        }

        return CompletableFuture.allOf(lastPositionsFuture, gridWriteFuture);
    }

    // Removes every locations older than UNINTENTIONAL_CONTAGION_TIME ms and adds a new position
    private void refreshLastPositions(Date time, Location location) {

        Date oldestDate = new Date(time.getTime() - MAX_CACHE_ENTRY_AGE);
        lastPositions.headMap(oldestDate).clear();
        if (location != null) {
            lastPositions.put(time, location);
        }
    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {

        lock.lock();

        SortedMap<Date, Location> copyOfLastPositions;
        try {
            // Return a copy of the cache to avoid conflicts
            copyOfLastPositions = new TreeMap<>(lastPositions.tailMap(new Date(System.currentTimeMillis() - MAX_CACHE_ENTRY_AGE)));
        } finally {
            lock.unlock();
        }
        return copyOfLastPositions;
    }
}
