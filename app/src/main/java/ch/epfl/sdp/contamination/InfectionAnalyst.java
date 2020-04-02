package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;

import ch.epfl.sdp.Callback;

public interface InfectionAnalyst {
    int RADIUS = 3;//maximum radius up to which infection may happen
    int WINDOW_FOR_INFECTION_DETECTION = 1200000; //[ms] Window of time during which a user should not meet a person to much to stay fit. actual : 20

    // MODEL: Being ill with a probability higher than this means becoming marked as INFECTED
    float CERTAINTY_APPROXIMATION_THRESHOLD = 0.9f;

    // MODEL: Being ill with a probability lower that this means becoming marked as HEALTHY
    float ABSENCE_APPROXIMATION_THRESHOLD = 0.1f;

    // MODEL: This parameter models the contagiousness of the disease
    float TRANSMISSION_FACTOR = 0.05f;

    /**
     * Updates the infection probability after staying at 'location' starting from startTime
     * @param startTime
     */
    void updateInfectionPredictions(Location location, Date startTime, Callback<Void> callback);

    /**
     * Returns the instance of the Carrier whose status is modified by the Infection Analyst
     * @return
     */
    Carrier getCarrier();
}