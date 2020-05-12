package ch.epfl.sdp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public class PositionRecord {
    private Timestamp timestamp;
    private GeoPoint geoPoint;

    public PositionRecord(Timestamp timestamp, GeoPoint geoPoint) {
        this.timestamp = timestamp;
        this.geoPoint = geoPoint;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public String calculateID() {
        // TODO: Modify this!!!
        return "SS" + timestamp.getSeconds();
    }
}