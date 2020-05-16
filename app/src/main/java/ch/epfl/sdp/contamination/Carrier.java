package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.Map;

/**
 * A Carrier is an entity with a modifiable health status
 */
public interface Carrier {

    /**
     * Represents the possible stages of infection:
     * HEALTHY:         Healthy carrier, not infected, not suspected to be ill, not immune
     * INFECTED:        Definitely ill and contagious
     * UNKNOWN:         Ill/healthy with some probability (no clues, bad luck :( )
     */
    enum InfectionStatus {
        HEALTHY,
        INFECTED,
        UNKNOWN
    }

    /**
     * Returns a unique identifier of the carrier:
     * This identifier should NOT be the account ID
     * since it's only used to distinguish between
     * different carriers
     * @return
     */
    String getUniqueId();

    /**
     * Returns the stage of the infection
     * @return
     */
    InfectionStatus getInfectionStatus();

    /**
     * Modify the status and the probability of infection of the carrier, at the time 'when'
     * If the change is successful, Observers are notified
     * @param when
     * @param newStatus
     * @return the outcome of the transition. If false, the old status & probability are kept
     * (and Observers are NOT notified)
     */
    boolean evolveInfection(Date when, InfectionStatus newStatus, float newProbability);


    /**
     * Returns the probability that the Carrier is ill, if his status is UNKNOWN
     */
    float getIllnessProbability();

    /**
     * Updates the probability of infection (without changing the status) of the carrier, at the time 'when'
     * @param newProbability
     * @return the outcome of the transition. If false, nothing is changed and Observers are NOT
     * notified
     */
    boolean setIllnessProbability(Date when, float newProbability);

    /**
     * Retrieves the evolution of infection probability for the carrier,
     * starting from the date 'since'
     * @param since
     * @return a Map containing, for each date, the probability of being infected
     */
    Map<Date, Float> getIllnessProbabilityHistory(Date since);

    void deleteLocalProbabilityHistory();
}
