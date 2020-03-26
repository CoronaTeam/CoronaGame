package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import ch.epfl.sdp.Callback;

public interface DataReceiver {
    /**
     * Basically communicates with the firebase and returns the users who have been at a given location, at a given date.
     * @param location
     * @param date
     * @return A list of UserID
     */
    void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback);

    /**
     *  User at a location, during a given time
     * @param location
     * @param startDate
     * @param endDate : endDate (inclusive)
     * @return
     */
    void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback);

    /**
     *
     * @param date
     * @return : location of the user using the app, at a given time
     */
    Location getMyLocationAtTime(Date date);
}