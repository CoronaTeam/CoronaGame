package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.Set;

/**
 * TimeMap : Will match each (location,date) pair to a list of user
 * This will actually never be implemented, but good for thought for the DataReceiver, or firebase Manager
 */
public abstract interface TimeMap {
    void addVisit(Date date ,Location location, String userID);

    /**
     * Given a date and a location, returns every user that have been at this point in time
     * @param date
     * @param location
     * @return
     */
    Set<String> getVisits(Date date,Location location);
    /**
    Returns the visits without the visit made by a specific user
     **/
    Set<String> getOtherVisits(Date date,Location location, String userID);
}
