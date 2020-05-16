package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public interface InfectionAnalyst {

    // MODEL: Staying close to an infected person for a time period longer than this
    // implies to be considered INFECTED (with probability =1)
    int WINDOW_FOR_INFECTION_DETECTION = 1200000; //[ms]

    // MODEL: Being ill with a probability higher than this means becoming marked as INFECTED
    float CERTAINTY_APPROXIMATION_THRESHOLD = 0.9f;

    // MODEL: Being ill with a probability lower that this means becoming marked as HEALTHY
    float ABSENCE_APPROXIMATION_THRESHOLD = 0.1f;

    // MODEL: This parameter makes the recorded infection probability decrease after each update
    // Since some time has elapsed, there is a lower probability that the user is infected
    float PROBABILITY_HISTORY_RETENTION_FACTOR = .95f;

    // MODEL: This parameter models the contagiousness of the disease
    float TRANSMISSION_FACTOR = .1f;

    //MODEL: This parameters models how long we are contagious before we remark our illness
    int UNINTENTIONAL_CONTAGION_TIME = 86400000; //[ms] actual : 24 hours

    //MODEL: This parameter models the immunity gain by a person who has been cured against the disease, 0 = 100% immune, 1 = 0% immune
    float IMMUNITY_FACTOR = 0.3f;

    /**
     * Updates the infection probability after staying at 'location' starting from startTime until 'endTime'
     * @param startTime
     * @param endTime
     */
    CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime);

    /**
     * Returns the instance of the Carrier whose status is modified by the Infection Analyst
     * @return
     */
    ObservableCarrier getCarrier();
}