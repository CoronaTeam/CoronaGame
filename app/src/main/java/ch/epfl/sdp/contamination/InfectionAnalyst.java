package ch.epfl.sdp.contamination;

import java.util.Date;

public interface InfectionAnalyst {
    int radius = 3;//maximum radius up to which infection may happen
    /**
     * Returns the probability of infection during WINDOW_FOR_LOCATION_AGGREGATION amount of time, starting at startTime
     * @param startTime
     */
    void getProbabilityOfInfection(Date startTime);
}
