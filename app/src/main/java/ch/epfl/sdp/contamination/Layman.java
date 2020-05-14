package ch.epfl.sdp.contamination;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

public class Layman extends Observable implements Carrier {

    private InfectionStatus myStatus;
    // Every update of infectedWithProbability must happen through setInfectionProbability()
    private float infectedWithProbability;

    private StorageManager<Date, Float> infectionHistory;

    private String uniqueID;

    public Layman() {
    }

    public Layman(InfectionStatus initialStatus) {
        this(initialStatus, initialStatus == InfectionStatus.INFECTED ? 1 : 0);
    }

    public Layman(InfectionStatus initialStatus, float infectedWithProbability) {
        this(initialStatus, infectedWithProbability, "__NOT_UNIQUE_NOW");
    }

    public Layman(InfectionStatus initialStatus, String uniqueID){
        this(initialStatus, initialStatus == InfectionStatus.INFECTED ? 1 : 0,uniqueID);
    }

    private StorageManager<Date, Float> initStorageManager(String fileId) {
        return new ConcreteManager<>(
                CoronaGame.getContext(),
                fileId + ".csv",
                date -> {
                    try {
                        return CoronaGame.dateFormat.parse(date);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date'");
                    }
                },
                Float::valueOf
        );
    }

    public Layman(InfectionStatus initialStatus, float infectedWithProbability, String uniqueID) {
        this.myStatus = initialStatus;
        this.uniqueID = uniqueID;

        this.infectionHistory = initStorageManager(uniqueID);
        validateAndSetProbability(new Date(), infectedWithProbability);
    }

    private boolean validateAndSetProbability(Date when, float probability) {
        if (probability < 0 || 1 <= probability) {
            return false;
        }

        if (myStatus == InfectionStatus.INFECTED) {
            return false;
        }

        // Include this update into the history
        infectionHistory.write(Collections.singletonMap(when, probability));

        infectedWithProbability = probability;
        return true;
    }

    @Override
    public InfectionStatus getInfectionStatus() {
        return myStatus;
    }

    /**
     * This method should only be called by someone 100% sure about the actual status.
     * @param newStatus
     * @return
     */
    @Override
    public boolean evolveInfection(InfectionStatus newStatus) {
        return evolveInfection(new Date(), newStatus);
    }

    @Override
    public boolean evolveInfection(Date when, InfectionStatus newStatus) {
        myStatus = newStatus;
        if (newStatus == InfectionStatus.INFECTED) {
            validateAndSetProbability(when, 1);
        }else if(newStatus == InfectionStatus.HEALTHY){
            validateAndSetProbability(when, 0);
        }

        // Broadcast the update
        setChanged();
        notifyObservers();

        return true;
    }

    @Override
    public float getIllnessProbability() {
        switch (myStatus) {
            case INFECTED:
                return 1;
            default:
                // Only useful case: the infection hits the 10% of the population overall
                return infectedWithProbability;
        }
    }

    @Override
    public boolean setIllnessProbability(float probability) {
        return setIllnessProbability(new Date(), probability);
    }

    @Override
    public boolean setIllnessProbability(Date when, float probability) {
        if (validateAndSetProbability(when, probability)) {
            setChanged();
            // Broadcast the update
            notifyObservers();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Map<Date, Float> getIllnessProbabilityHistory(Date since) {
        return infectionHistory.filter((date, prob) -> date.after(since));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return (obj instanceof Layman) &&
                ((Layman)obj).uniqueID.equals(this.uniqueID) &&
                ((Layman)obj).myStatus == this.myStatus &&
                ((Layman)obj).infectedWithProbability == this.infectedWithProbability;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("#%s: %s (p=%f)", uniqueID, myStatus, infectedWithProbability);
    }
    
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

    ///Getters Needed for the conversion from Object to Map<String, Object> during the Fierbase
    // Upload
    public InfectionStatus getMyStatus() {
        return myStatus;
    }

    @Override
    public void deleteLocalProbabilityHistory() {
        setIllnessProbability(0f);
        evolveInfection(InfectionStatus.HEALTHY);

        // Delete current manager
        infectionHistory.delete();

        // Create a new one
        infectionHistory = initStorageManager(uniqueID);
    }
}
