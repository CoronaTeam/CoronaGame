package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.identity.fragment.AccountFragment;

import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.HISTORY_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.HISTORY_POSITIONS_DOC;
import static ch.epfl.sdp.firestore.FirestoreLabels.INFECTION_STATUS_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMESTAMP_TAG;
import static ch.epfl.sdp.identity.AuthenticationManager.getActivity;

/**
 * Implementation of a DataSender with a cache
 */
public class ConcreteDataSender implements DataSender {

    private GridFirestoreInteractor gridInteractor;
    public ConcreteDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }
    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
        location = DataSender.roundLocation(location);
        try {
            storeLocationToCache(location, time);
        }
        finally {


            CompletableFuture<Void> historyFuture, lastPositionsFuture, gridWriteFuture;


            Map<String, Object> element = new HashMap<>();
            element.put(GEOPOINT_TAG, new GeoPoint(
                    location.getLatitude(),
                    location.getLongitude()
            ));
            element.put(TIMESTAMP_TAG, time.getTime());
            element.put(INFECTION_STATUS_TAG, carrier.getInfectionStatus());

            historyFuture = gridInteractor.writeDocumentWithID(documentReference(
                    HISTORY_COLL + "/" + carrier.getUniqueId() + "/" + HISTORY_POSITIONS_DOC, "TS" + time.getTime()), element);

            lastPositionsFuture = gridInteractor.writeDocumentWithID(
                    documentReference(LAST_POSITIONS_COLL, AccountFragment.getAccount(getActivity()).getId()), element);

            gridWriteFuture = gridInteractor.gridWrite(location, String.valueOf(time.getTime()), carrier);

            return CompletableFuture.allOf(historyFuture, lastPositionsFuture, gridWriteFuture);
        }
    }

    @VisibleForTesting
    public void storeLocationToCache(Location location, Date date){
        PositionHistoryManager.refreshLastPositions(date,location);
    }
}
