package ch.epfl.sdp.contamination;

public interface Carrier {

    /**
     * Represents the possible stages of infection:
     * HEALTHY:         Healthy carrier, not infected, not suspected to be ill, not immune
     * INFECTED:        Definitely ill and contagious
     * IMMUNE:          Healthy carrier, either healed or already immune for other reasons
     * UNKNOWN:         Ill/healthy with some probability (no clues, bad luck :( )
     */
    enum InfectionStatus {
        HEALTHY,
        INFECTED,
        IMMUNE,
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
     * Returns the probability that the Carrier is ill, if his status is UNKNOWN
     */
    float getIllnessProbability();

    /**
     * Updates the probability that the Carrier is ill, if his status is UNKNOWN
     * Returns false if:
     *  - probability < 0 or >= 1
     *  - status is != UNKNOWN
     */
    boolean setIllnessProbability(float probability);

}
