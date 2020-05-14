package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.Map;

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
     * Modify the infection status
     * @param newStatus
     * @return
     */
    boolean evolveInfection(InfectionStatus newStatus);

    /**
     * Modify the infection status,
     * for a past time 'when'
     * @param newStatus
     * @return
     */
    boolean evolveInfection(Date when, InfectionStatus newStatus);


    /**
     * Returns the probability that the Carrier is ill, if his status is UNKNOWN
     */
    float getIllnessProbability();

    /**
     * Updates the probability that the Carrier is ill
     * Returns false if:
     *  - probability < 0 or >= 1
     */
    boolean setIllnessProbability(float probability);

    /**
     * Updates the probability that the Carrier is ill
     * for a past time 'when'
     * Returns false if:
     *  - probability < 0 or >= 1
     */
    boolean setIllnessProbability(Date when, float probability);

    /**
     * Retrieves the evolution of infection probability for the carrier,
     * starting from the date 'since'
     * @param since
     * @return a Map containing, for each date, the probability of being infected
     */
    Map<Date, Float> getIllnessProbabilityHistory(Date since);

    void deleteLocalProbabilityHistory();
}
