package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.Carrier;

public interface DataReceiver {
    /**
     * Calls the callback with a Set containing the Carriers found at 'location' at time 'time'
     *
     * @param location
     * @param date
     */
    CompletableFuture<Set<Carrier>> getUserNearby(Location location, Date date);

    /**
     * Calls the callback with a Map of Carriers and the number of (different) times they appear at that spot
     *
     * @param location
     * @param startDate
     * @param endDate   : endDate (inclusive)
     */
    CompletableFuture<Map<Carrier, Integer>> getUserNearbyDuring(Location location,
                                                                 Date startDate, Date endDate);

    /**
     * @return : last location of the user using the app
     */
    CompletableFuture<Location> getMyLastLocation(String accountId);


//    int getAndResetSickNeighbors(String userId);//,Callback<Map<String,Object>> callback);

    /**
     * @param userId
     * @return # of sick neighbors met yesterday
     */
    CompletableFuture<Map<String, Object>> getNumberOfSickNeighbors(String userId);

    /**
     * @param #      of times the given user has recovered from his sickness
     * @param userId
     * @return
     */
    CompletableFuture<Map<String, Object>> getRecoveryCounter(String userId);

}
