package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.fragment.AccountFragment;

import static ch.epfl.sdp.AuthenticationManager.getActivity;

public class ConcreteCachingDataSender implements CachingDataSender {
    private GridFirestoreInteractor gridInteractor;
    SortedMap<Date, Location> lastPositions;
    // Default success listener
    private OnSuccessListener successListener = o -> {
    };

    // Default Failure listener
    private OnFailureListener failureListener = e -> {
    };

    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
        this.lastPositions = new TreeMap<>();
    }

    public ConcreteCachingDataSender setOnSuccessListener(OnSuccessListener successListener) {
        this.successListener = successListener;
        return this;
    }

    public ConcreteCachingDataSender setOnFailureListener(OnFailureListener failureListener) {
        this.failureListener = failureListener;
        return this;
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public void registerLocation(Carrier carrier, Location location, Date time) {
        registerLocation(carrier, location, time, successListener, failureListener);
    }

    @Override
    public void registerLocation(Carrier carrier,
                                 Location location,
                                 Date time,
                                 OnSuccessListener successListener,
                                 OnFailureListener failureListener) {
        refreshLastPositions(time, location);

        Map<String, Object> element = new HashMap<>();
        element.put("geoPoint", new GeoPoint(location.getLatitude(), location.getLongitude()));
        element.put("timeStamp", time.getTime());
        element.put("infectionStatus", carrier.getInfectionStatus());
        gridInteractor.writeDocumentWithID("LastPositions", AccountFragment.getAccount(getActivity()).getId(),
                element, s -> {
                    gridInteractor.write(location, String.valueOf(time.getTime()), carrier, successListener, failureListener);
                },
                failureListener);
    }

    /**
     * removes every locations older than UNINTENTIONAL_CONTAGION_TIME ms and adds a new position
     */
    private void refreshLastPositions(Date time, Location location) {
        Date oldestDate = new Date(time.getTime() - InfectionAnalyst.UNINTENTIONAL_CONTAGION_TIME);
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
