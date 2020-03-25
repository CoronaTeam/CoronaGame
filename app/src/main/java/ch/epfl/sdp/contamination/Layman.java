package ch.epfl.sdp.contamination;

public class Layman implements Carrier {
    private InfectionStatus myStatus;

    private float infectedWithProbability;

    Layman(InfectionStatus initialStatus) {
        myStatus = initialStatus;
        if (initialStatus == InfectionStatus.INFECTED) {
            infectedWithProbability = 1;
        } else {
            infectedWithProbability = 0;
        }
    }

    @Override
    public InfectionStatus getInfectionStatus() {
        return myStatus;
    }

    @Override
    public boolean evolveInfection(InfectionStatus newStatus) {
        myStatus = newStatus;
        if (newStatus == InfectionStatus.INFECTED) {
            infectedWithProbability = 1;
        }
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
                return infectedWithProbability;
        }
        return 0;
    }

    @Override
    public boolean setIllnessProbability(float probability) {
        if (probability < 0 || 1 <= probability) {
            return false;
        }

        if (myStatus != InfectionStatus.HEALTHY_CARRIER && myStatus != InfectionStatus.UNKNOWN) {
            return false;
        }

        infectedWithProbability = probability;

        return true;
    }
}
