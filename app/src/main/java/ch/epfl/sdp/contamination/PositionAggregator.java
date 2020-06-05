package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Calendar;
import java.util.Date;

import static ch.epfl.sdp.CoronaGame.getDemoSpeedup;

/**
 * A positionAggregator should take the average value of multiple points in space during a given interval in time.
 * Then, is rounds this average to the nearest meter (i.e. rounding latitude/longitude to ex. 5 decimals), and sends this location along with the last time to a DataSender.
 **/
public interface PositionAggregator {
    int WINDOW_FOR_LOCATION_AGGREGATION = 30_000 / getDemoSpeedup(); // [ms] This is the frequency with
    // which the (mean) position will be uploaded. actual : 2min30
    int MAXIMAL_NUMBER_OF_LOCATIONS_PER_AGGREGATION = 2; //WINDOW_FOR_LOCATION_AGGREGATION/10000; // default : 1 location every 10 seconds

    static Date getWindowForDate(Date date) {
        long time = date.getTime();
        long roundedTime = time - time % WINDOW_FOR_LOCATION_AGGREGATION; // drop part not multiple of WINDOW_FOR_LOCATION_AGGREGATION
        date.setTime(roundedTime);
        return date;
    }

    /**
     * Adds a position to the position list. Every WINDOW_FOR_LOCATION_AGGREGATION time, it should send the mean value of the
     * positions to the DataSender
     *
     * @param location : current location of the phone
     * @param date     :  time at which the location has been reported
     */
    void addPosition(Location location, Date date);

    default void addPosition(Location location) {
        addPosition(location, Calendar.getInstance().getTime());
    }

    void updateToOffline();

    void updateToOnline();
}