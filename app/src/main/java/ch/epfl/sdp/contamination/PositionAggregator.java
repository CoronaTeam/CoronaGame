package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Calendar;
import java.util.Date;

/**
 * A positionAggregator should take the average value of multiple points in space during a given interval in time.
 * Then, is rounds this average to the nearest meter (i.e. rounding latitude/longitude to ex. 5 decimals), and sends this location along with the last time to a DataSender.
 **/
public interface PositionAggregator {
    // TODO: Changed for debug, restore values 300000 & 10
    int WINDOW_FOR_LOCATION_AGGREGATION = 5000; // [ms] This is the frequency with which the (mean) position will be uploaded. actual : 5min
    int MAXIMAL_NUMBER_OF_LOCATIONS_PER_AGGREGATION = 1; // 100 locations per aggregation. Before release choose a big number
    /**
     *
     * Adds a position to the position list. Every WINDOW_FOR_LOCATION_AGGREGATION time, it should send the mean value of the
     * positions to the DataSender
     * @param location : current location of the phone
     * @param date :  time at which the location has been reported
     */
    void addPosition(Location location, Date date);
    default void addPosition(Location location) {
        addPosition(location, Calendar.getInstance().getTime());
    }
    static Date getWindowForDate(Date date) {
        long time = date.getTime();
        long roundedTime = time - time % WINDOW_FOR_LOCATION_AGGREGATION; // drop part not multiple of WINDOW_FOR_LOCATION_AGGREGATION
        date.setTime(roundedTime);
        return date;
    }
    void updateToOffline();
    void updateToOnline();
}