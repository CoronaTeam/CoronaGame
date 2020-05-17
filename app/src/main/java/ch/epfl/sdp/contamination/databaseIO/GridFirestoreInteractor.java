package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.identity.Account;

import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.LIVE_GRID_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMES_LIST_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.UNIXTIME_TAG;

public class GridFirestoreInteractor extends ConcreteFirestoreInteractor {

    // MODEL: Round the location to the 3th decimal digit
    public static final int COORDINATE_PRECISION = 1000;

    public GridFirestoreInteractor() {
        super();
    }

    String getGridId(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        int idLatitude = (int) (latitude * COORDINATE_PRECISION);
        int idLongitude = (int) (longitude * COORDINATE_PRECISION);

        return String.format("Grid#%d#%d", idLatitude, idLongitude);
    }

    public CompletableFuture<Map<String, Map<String, Object>>> getTimes(Location location) {
        String path = LIVE_GRID_COLL + "/" + getGridId(location) + "/" + TIMES_LIST_COLL;
        return readCollection(collectionReference(path));
    }

    public CompletableFuture<Map<String, Object>> readLastLocation(Account account) {
        return readDocument(documentReference(LAST_POSITIONS_COLL, account.getId()));
    }


    public CompletableFuture<Map<String, Map<String, Object>>> gridRead(Location location, long time) {
        String path = LIVE_GRID_COLL + "/" + getGridId(location) + "/" + time;
        return readCollection(collectionReference(path));
    }

    public CompletableFuture<Void> gridWrite(Location location, String time, Carrier carrier) {
        Map<String, Object> timeMap = new HashMap<>();
        timeMap.put(UNIXTIME_TAG, time);

        // TODO: [LOG]
        Log.e("POSITION_UPLOAD", getGridId(location));

        return writeDocumentWithID(
                    documentReference(LIVE_GRID_COLL + "/" + getGridId(location) + "/" + TIMES_LIST_COLL, time), timeMap)
                .thenRun(() -> writeDocument(collectionReference(
                        LIVE_GRID_COLL + "/" + getGridId(location) + "/" + time), carrier));
    }
}
