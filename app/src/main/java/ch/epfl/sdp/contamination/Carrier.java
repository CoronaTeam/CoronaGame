package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.Map;

public interface Carrier {

    /**
     * Returns a unique identifier of the carrier:
     * This identifier should NOT be the account ID
     * since it's only used to distinguish between
     * different carriers
     *
     * @return a unique identifier of the carrier
     */
    String getUniqueId();

    /**
     * Returns the stage of the infection
     *
     * @return stage of the infection
     */
    InfectionStatus getInfectionStatus();

    /**
     * Modify the infection status.
     * This method should only be called by someone 100% sure about the actual status.
     *
     * @param newStatus
     * @return
     */
    boolean evolveInfection(InfectionStatus newStatus);

    /**
     * @return the probability that the Carrier is ill, if his status is UNKNOWN
     */
    float getIllnessProbability();

    /**
     * Updates the probability that the Carrier is ill, if his status is UNKNOWN
     *
     * @param probability
     * @return false if:
     * - probability < 0 or >= 1
     * - status is != UNKNOWN
     * true otherwise
     */
    boolean setIllnessProbability(float probability);

    /**
     * Retrieves the evolution of infection probability for the carrier,
     * starting from the date 'since'
     *
     * @param since starting date
     * @return a Map containing, for each date, the probability of being infected
     */
    Map<Date, Float> getIllnessProbabilityHistory(Date since);

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
}
