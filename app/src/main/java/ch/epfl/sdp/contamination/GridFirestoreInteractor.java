package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

public class GridFirestoreInteractor extends ConcreteFirestoreInteractor{

    // MODEL: Round the location to the 5th decimal digit
    public static final int COORDINATE_PRECISION = 100000;

    public GridFirestoreInteractor() {
        super();
    }
    String getGridId(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        int idLatitude = (int) latitude * COORDINATE_PRECISION;
        int idLongitude = (int) longitude * COORDINATE_PRECISION;

        return String.format("Grid#%d#%d", idLatitude, idLongitude);
    }

    public void getTimes(Location location, QueryHandler handler) {
        String path = "LiveGrid/" + getGridId(location) + "/Times";
        super.readCollection(path, handler);
    }

    public void readLastLocation(Account account, Callback<Map<String, Object>> callback) {
        super.readDocument("LastPositions", account.getId(), callback);
    }

    public void read(Location location, long time, QueryHandler handler) {
        String path = "LiveGrid/" + getGridId(location) + "/" + time;
        super.readCollection(path, handler);
    }

    public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
        Map<String, Object> timeMap = new HashMap<>();
        timeMap.put("Time", time);
        // LiveGrid/[location] must be updated only if the time has been successfully inserted in the list
        super.writeDocumentWithID("LiveGrid/" + getGridId(location) + "/Times", time, timeMap, s ->
                        super.writeDocument("LiveGrid/" + getGridId(location) + "/" + time, carrier, success, failure),
                failure);
    }
}
