package ch.epfl.sdp.contamination;

import java.util.Date;

public interface InfectionAnalyst {
    int RADIUS = 3;//maximum radius up to which infection may happen
    int WINDOW_FOR_INFECTION_DETECTION = 1200000; //[ms] Window of time during which a user should not meet a person to much to stay fit. actual : 20 min
    /**
     * Returns the probability of infection during WINDOW_FOR_INFECTION_DETECTION amount of time, starting at startTime
     * @param startTime
     */
    void getProbabilityOfInfection(Date startTime);
}
