package ch.epfl.sdp.contamination;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class Layman implements Carrier {

    private InfectionStatus myStatus;
    private float infectedWithProbability;

    // TODO: Properly set the uniqueID (!!)
    private String uniqueID;

    // TODO: Add another field to distinguish among Carrier (also modify equalsTo and hashCode!!)
    public Layman(InfectionStatus initialStatus) {
        myStatus = initialStatus;
        if (initialStatus == InfectionStatus.INFECTED) {
            infectedWithProbability = 1;
        } else {
            infectedWithProbability = 0;
        }

        uniqueID = "__NOT_UNIQUE_NOW";
    }

    public Layman(InfectionStatus infectionStatus, float infectedWithProbability) {
        this.myStatus = infectionStatus;
        this.infectedWithProbability = infectedWithProbability;

        uniqueID = "__NOT_UNIQUE_NOW";
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

    @Override
    public boolean equals(@Nullable Object obj) {
        return (obj instanceof Layman) &&
                ((Layman)obj).myStatus == this.myStatus &&
                ((Layman)obj).infectedWithProbability == this.infectedWithProbability;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("#%s: %s (p=%f)", uniqueID, myStatus, infectedWithProbability);
    }

    // TODO: If uniqueID is properly assigned, its hash can be the hash of the carrier
    @Override
    public int hashCode() {
        return Objects.hash(uniqueID, myStatus, infectedWithProbability);
    }

    @Override
    public String getUniqueId() {
        return uniqueID;
    }
}
