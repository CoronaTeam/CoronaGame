package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.SortedMap;

public interface AggregationCache {
    /**
     *
     * @return: locations and times of a given user for a given amount of time
     */
    SortedMap<Date, Location> getLastPositions();
}
