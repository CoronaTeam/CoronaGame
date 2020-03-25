package ch.epfl.sdp.contamination;

public class Layman implements Carrier {
    private InfectionStatus myStatus;

    Layman(InfectionStatus initialStatus) {
        myStatus = initialStatus;
    }

    @Override
    public InfectionStatus getInfectionStatus() {
        return myStatus;
    }

    @Override
    public boolean evolveInfection(InfectionStatus newStatus) {
        myStatus = newStatus;
        return true;
    }

    @Override
    public float getIllnessProbability() {
        switch (myStatus) {
            case HEALTHY_CARRIER:
            case INFECTED:
                return 1;
            case UNKNOWN:
                // Only useful case: the infection hits the 10% of the population overall
                return 0.1f;
        }
        return 0;
    }
}
