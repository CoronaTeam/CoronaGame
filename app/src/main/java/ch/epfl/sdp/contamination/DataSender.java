package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;

public interface DataSender {
    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     */
    void sendALocationToFirebase(Location location, Date time);
}
