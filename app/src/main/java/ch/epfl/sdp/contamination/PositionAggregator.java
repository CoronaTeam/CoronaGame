package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Calendar;
import java.util.Date;
/*
A positionAggregator should take the average value of multiple points in space during a given interval in time.
Then, is rounds this average to the nearest meter (i.e. rounding latitude/longitude to 5 decimals), and sends this location along with the last time to a DataSender.
 */
public interface PositionAggregator {
    int WINDOW_FOR_LOCATION_AGGREGATION = 300000; //[ms] This is the frequency with which the (mean) position will be uploaded. actual : 5min

    /**
     *
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

    /**
     * Every WINDOW_FOR_LOCATION_AGGREGATION time, the PositionAggregator should send the mean value of the
     * saved positions to the DataSender. This method estimates whether the PositionAggregator should send that mean,
     * or if it just returns without doing anything.
     */
    void update();
}