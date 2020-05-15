package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.Map;

/**
 * Neighbor is a simple type of Carrier, without the ability to be observed or
 * to retain its infection history
 *
 * It's useful to represent neighbors downloaded from Firestore
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
    public boolean evolveInfection(InfectionStatus newStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean evolveInfection(Date when, InfectionStatus newStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getIllnessProbability() {
        return infectionProbability;
    }

    @Override
    public boolean setIllnessProbability(float probability) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setIllnessProbability(Date when, float probability) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Date, Float> getIllnessProbabilityHistory(Date since) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteLocalProbabilityHistory() {
        throw new UnsupportedOperationException();
    }
}
