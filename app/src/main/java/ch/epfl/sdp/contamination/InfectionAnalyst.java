package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;

public interface InfectionAnalyst {
    int RADIUS = 3;//maximum radius up to which infection may happen
    int WINDOW_FOR_INFECTION_DETECTION = 1200000; //[ms] Window of time during which a user should not meet a person to much to stay fit. actual : 20

    // MODEL: Being ill with a probability higher than this means becoming marked as INFECTED
    float CERTAINTY_APPROXIMATION_THRESHOLD = 0.9f;

    // MODEL: This parameter models the contagiousness of the disease
    float TRANSMISSION_FACTOR = 0.05f;

    /**
     * Returns the probability of infection during WINDOW_FOR_INFECTION_DETECTION amount of time, starting at startTime
     * @param startTime
     */
    void updateInfectionPredictions(Location location, Date startTime);
}