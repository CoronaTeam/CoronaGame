package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;

/**
 * TODO: doc
 */
public class GridFirestoreInteractor extends ConcreteFirestoreInteractor {

    // MODEL: Round the location to the 5th decimal digit
    public static final int COORDINATE_PRECISION = 100000;

    public GridFirestoreInteractor() {
        super();
    }

    private String getGridId(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        int idLatitude = (int) latitude * COORDINATE_PRECISION;
        int idLongitude = (int) longitude * COORDINATE_PRECISION;

        return String.format(Locale.FRANCE, "Grid#%d#%d", idLatitude, idLongitude);
    }

    /**
     * @param location
     * @return
     */
    public CompletableFuture<Map<String, Map<String, Object>>> getTimes(Location location) {
        String path = "LiveGrid/" + getGridId(location) + "/Times";
        return readCollection(collectionReference(path));
    }

    /**
     * @param account
     * @return
     */
    public CompletableFuture<Map<String, Object>> readLastLocation(Account account) {
        return readDocument(documentReference("LastPositions", account.getId()));
    }


    /**
     * @param location
     * @param time
     * @return
     */
    public CompletableFuture<Map<String, Map<String, Object>>> gridRead(Location location, long time) {
        String path = "LiveGrid/" + getGridId(location) + "/" + time;
        return readCollection(collectionReference(path));
    }

    /**
     * @param location
     * @param time
     * @param carrier
     * @return
     */
    public CompletableFuture<Void> gridWrite(Location location, String time, Carrier carrier) {
        Map<String, Object> timeMap = new HashMap<>();
        timeMap.put("Time", time);

        return writeDocumentWithID(documentReference("LiveGrid/" + getGridId(location)
                + "/Times", time), timeMap)
                .thenRun(() -> writeDocument(collectionReference(
                        "LiveGrid/" + getGridId(location) + "/" + time), carrier));
    }
}
