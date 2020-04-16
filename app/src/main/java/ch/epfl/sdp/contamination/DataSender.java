package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Date;

public interface DataSender {
    int EXPAND_FACTOR = 100000; //determines the GPS coordinates precision
    static Location RoundAndExpandLocation(Location l){
        int a = (int)(0.5 + l.getLatitude()*EXPAND_FACTOR);
        int b = (int)(0.5 + l.getLongitude()*EXPAND_FACTOR);
        l.setLongitude(b);
        l.setLatitude(a);
        return l;
    }

    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     *   The default callback is executed after this operation
     * @param carrier : the carrier present at location
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     */
    void registerLocation(Carrier carrier, Location location, Date time);

    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     *   Call the appropriate listener depending on the result of the operation
     * @param carrier : the carrier present at location
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     * @param successListener : listener called in case of success
     * @param failureListener: listener called in case of failure
     */
    void registerLocation(Carrier carrier,
                          Location location,
                          Date time,
                          OnSuccessListener successListener,
                          OnFailureListener failureListener);
}
