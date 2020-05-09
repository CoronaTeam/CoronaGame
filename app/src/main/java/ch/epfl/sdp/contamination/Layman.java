package ch.epfl.sdp.contamination;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

public class Layman implements Carrier {

    private InfectionStatus myStatus;
    // Every update of infectedWithProbability must happen through setInfectionProbability()
    private float infectedWithProbability;
    private StorageManager<Date, Float> infectionHistory;
    // TODO: Properly set the uniqueID (!!)
    private String uniqueID;

    /**
     * @param initialStatus
     * @param infectedWithProbability
     * @param uniqueID
     */
    public Layman(InfectionStatus initialStatus, float infectedWithProbability, String uniqueID) {
        DateFormat format = new SimpleDateFormat("E MMM dd hh:mm:ss zzz yyyy", Locale.FRANCE);

        this.infectionHistory = new ConcreteManager<>(
                CoronaGame.getContext(),
                uniqueID + ".csv",
                date -> {
                    try {
                        return format.parse(date);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date'");
                    }
                },
                Float::valueOf
        );

        this.myStatus = initialStatus;
        setIllnessProbability(infectedWithProbability);
        this.uniqueID = uniqueID;
    }

    // TODO: Properly set uniqueID (also modify equalsTo and hashCode!!)
    public Layman(InfectionStatus initialStatus) {
        this(initialStatus, initialStatus == InfectionStatus.INFECTED ? 1 : 0);
    }

    public Layman(InfectionStatus initialStatus, float infectedWithProbability) {
        this(initialStatus, infectedWithProbability, "__NOT_UNIQUE_NOW");
    }

    public Layman(InfectionStatus initialStatus, String uniqueID) {
        this(initialStatus, initialStatus == InfectionStatus.INFECTED ? 1 : 0, uniqueID);
    }

    public Layman() {
    }

    @Override
    public InfectionStatus getInfectionStatus() {
        return myStatus;
    }

    @Override
    public boolean evolveInfection(InfectionStatus newStatus) {
        myStatus = newStatus;
        if (newStatus == InfectionStatus.INFECTED) {
            setIllnessProbability(1);
        } else if (newStatus == InfectionStatus.HEALTHY) {
            setIllnessProbability(0);
        }
        return true;
    }

    @Override
    public float getIllnessProbability() {
        if (myStatus == InfectionStatus.INFECTED) {
            return 1;
        }
        // Only useful case: the infection hits the 10% of the population overall
        return infectedWithProbability;
    }

    @Override
    public boolean setIllnessProbability(float probability) {
        if (probability < 0 || 1 <= probability) {
            return false;
        }

        if (myStatus == InfectionStatus.INFECTED) {
            return false;
        }

        // Include this update into the history
        infectionHistory.write(Collections.singletonMap(new Date(), probability));

        infectedWithProbability = probability;

        return true;
    }

    @Override
    public Map<Date, Float> getIllnessProbabilityHistory(Date since) {
        return infectionHistory.filter((date, prob) -> date.after(since));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return (obj instanceof Layman) &&
                ((Layman) obj).uniqueID.equals(this.uniqueID) &&
                ((Layman) obj).myStatus == this.myStatus &&
                ((Layman) obj).infectedWithProbability == this.infectedWithProbability;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.FRANCE, "#%s: %s (p=%f)", uniqueID, myStatus,
                infectedWithProbability);
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        infectionHistory.close();
    }

    ///Getters Needed for the conversion from Object to Map<String, Object> during the Firebase
    // Upload
    public InfectionStatus getMyStatus() {
        return myStatus;
    }
}
