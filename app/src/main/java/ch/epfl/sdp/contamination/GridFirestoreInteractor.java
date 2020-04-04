package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.QueryHandler;

public class GridFirestoreInteractor {

    // MODEL: Round the location to the 5th decimal digit
    static final int COORDINATE_PRECISION = 100000;

    private FirebaseFirestore fs;

    public GridFirestoreInteractor(FirebaseFirestore firebaseFirestore) {
        this.fs = firebaseFirestore;
    }

    String getGridId(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        int idLatitude = (int) latitude * COORDINATE_PRECISION;
        int idLongitude = (int) longitude * COORDINATE_PRECISION;

        return String.format("Grid#%d#%d", idLatitude, idLongitude);
    }

    public void getTimes(Location location, QueryHandler handler) {
        fs.collection("LiveGrid/" + getGridId(location) + "/Times")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handler.onSuccess(task.getResult());
                    } else {
                        handler.onFailure();
                    }
                });
    }

    public void readLastLocation(Account account, Callback callback) {
        fs.collection("LastPositions")
                .document(account.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onCallback(task.getResult());
                    } else {
                        callback.onCallback(task.getException());
                    }
                });
    }

    public void read(Location location, long time, QueryHandler handler) {
        fs.collection("LiveGrid/" + getGridId(location) + "/" + time)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handler.onSuccess(task.getResult());
                    } else {
                        handler.onFailure();
                    }
                });
    }

    public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
        Map<String, String> timeMap = new HashMap<>();
        timeMap.put("Time", time);
        fs.collection("LiveGrid/" + getGridId(location) + "/Times")
                .document(time)
                .set(timeMap);
        fs.collection("LiveGrid/" + getGridId(location) + "/" + time)
                .add(carrier)
                .addOnSuccessListener(success)
                .addOnFailureListener(failure);
    }
}
