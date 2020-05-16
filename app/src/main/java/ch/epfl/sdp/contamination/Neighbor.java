package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.Map;

/**
 * Neighbor is a simple type of Carrier, who has a FIXED status (hence also without status history)
 *
 * It's useful to represent the information on people I met, downloaded from Firestore
 */
public class Neighbor implements Carrier {

    private float infectionProbability;
    private InfectionStatus infectionStatus;
    private String uniqueId;

    Neighbor(InfectionStatus st, float prob, String uid) {
        infectionStatus = st;
        infectionProbability = prob;
        uniqueId = uid;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public InfectionStatus getInfectionStatus() {
        return infectionStatus;
    }

    @Override
    public float getIllnessProbability() {
        return infectionProbability;
    }

    @Override
    public boolean evolveInfection(Date when, InfectionStatus newStatus, float newProbability) {
        throw new UnsupportedOperationException("I am a simple Neighbor, my status cannot be modified");
    }

    @Override
    public boolean setIllnessProbability(Date when, float probability) {
        throw new UnsupportedOperationException("I am a simple Neighbor, my status cannot be modified");
    }

    @Override
    public Map<Date, Float> getIllnessProbabilityHistory(Date since) {
        throw new UnsupportedOperationException("I am a simple Neighbor, I don't keep the history of my status");
    }

    @Override
    public void deleteLocalProbabilityHistory() {
        throw new UnsupportedOperationException("I am a simple Neighbor, I don't keep the history of my status");
    }
}
