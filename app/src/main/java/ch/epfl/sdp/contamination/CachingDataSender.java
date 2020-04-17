package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.SortedMap;

public interface CachingDataSender {
    int EXPAND_FACTOR = 100000; //determines the GPS coordinates precision
    String publicUserFolder = "publicUser/";
    String publicAlertAttribute = "recentlySickMeetingCounter";

    static Location RoundAndExpandLocation(Location l){
        int a = (int)(0.5 + l.getLatitude()*EXPAND_FACTOR);
        int b = (int)(0.5 + l.getLongitude()*EXPAND_FACTOR);
        l.setLongitude(b);
        l.setLatitude(a);
        return l;
    }
    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     */
    void registerLocation(Carrier carrier, Location location, Date time);
    /**
     * Notifies a user he has been close to an infected person
     */
    void sendAlert(String userId);

    void resetSickAlerts(String userId);
    /**
     *
     * @return: locations and times of a given user for a given amount of time
     */
    SortedMap<Date, Location> getLastPositions();
}
