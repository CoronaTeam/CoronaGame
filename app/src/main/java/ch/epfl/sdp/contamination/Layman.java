package ch.epfl.sdp.contamination;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;

/**
 * Concrete instance of ObservableCarrier.
 * Laymen can locally store and retrieve the history of their infection probabilities
 * They are also able to notify Observers about probability or status transitions.
 * In case of a transition, Observers receive as argument of update() an Optional<Float> that:
 *   - if the STATUS changed, contains the previous infection probability
 *   - if only the PROBABILITY changed, is None
 */
public class Layman extends ObservableCarrier {

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
        if (probability < 0 || 1 < probability) {
            return false;
        }

        // Include this update into the history
        infectionHistory.write(new TreeMap<>(Collections.singletonMap(when, probability)));

        infectedWithProbability = probability;
        return true;
    }

    @Override
    public InfectionStatus getInfectionStatus() {
        return myStatus;
    }

    @Override
    public boolean evolveInfection(Date when, InfectionStatus newStatus, float newProbability) {

        Optional<Float> previousProbability;

        // Detect status transition and save previous probability
        if (newStatus != myStatus) {
            previousProbability = Optional.of(infectedWithProbability);
        } else {
            previousProbability = Optional.empty();
        }

        // If the probability is valid, update it and the status and notify observers
        if (validateAndSetProbability(when, newProbability)) {

            myStatus = newStatus;

            // Broadcast the update
            setChanged();

            notifyObservers(previousProbability);

            return true;
        }

        return false;
    }

    @Override
    public float getIllnessProbability() {
        return infectedWithProbability;
    }

    @Override
    public boolean setIllnessProbability(Date when, float probability) {
        if (validateAndSetProbability(when, probability)) {
            setChanged();

            // Broadcast the update (status has NOT changed)
            notifyObservers(Optional.empty());
            return true;
        }

        return false;
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

    @Override
    public void deleteLocalProbabilityHistory() {
        evolveInfection(new Date(), HEALTHY, 0f);

        // Delete current manager
        infectionHistory.delete();

        // Create a new one
        infectionHistory = initStorageManager(uniqueID);
    }
}
