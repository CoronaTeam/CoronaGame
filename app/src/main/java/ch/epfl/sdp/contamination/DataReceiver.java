package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;

public interface DataReceiver {
    /**
     * Calls the callback with a Set containing the Carriers found at 'location' at time 'time'
     * @param location
     * @param date
     * @param callback
     */
    void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback);

    /**
     *  Calls the callback with a Map of Carriers and the number of (different) times they appear at that spot
     * @param location
     * @param startDate
     * @param endDate : endDate (inclusive)
     * @param callback
     */
    void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback);

    /**
     *
     * @param date
     * @return : location of the user using the app, at a given time
     */
    void getMyLastLocation(Account account, Callback<Location> callback);
}