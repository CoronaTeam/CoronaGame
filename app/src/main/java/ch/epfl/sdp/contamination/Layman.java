package ch.epfl.sdp.contamination;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

/**
 * THREAD-SAFE implementation of ObservableCarrier.
 * Laymen can locally store and retrieve the history of their infection probabilities
 * They are also able to notify Observers about probability or status transitions.
 * In case of a transition, Observers receive as argument of update() an Optional<Float> that:
 * - if the STATUS changed, contains the previous infection probability
 * - if only the PROBABILITY changed, is None
 */
public class Layman extends ObservableCarrier {

    private ReentrantLock lock;

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

    public Layman(InfectionStatus initialStatus, String uniqueID) {
        this(initialStatus, initialStatus == InfectionStatus.INFECTED ? 1 : 0, uniqueID);
    }

    public Layman(InfectionStatus initialStatus, float infectedWithProbability, String uniqueID) {
        this.myStatus = initialStatus;
        this.uniqueID = uniqueID;

        lock = new ReentrantLock();

        this.infectionHistory = initStorageManager(uniqueID);

        validateAndSetProbability(new Date(), infectedWithProbability);
    }

    private StorageManager<Date, Float> openStorageManager(String fileId) {

        return new ConcreteManager<>(
                CoronaGame.getContext(),
                fileId + ".csv",
                date -> {
                    try {
                        return CoronaGame.dateFormat.parse(date);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date'. Example of data found: " + date);
                    }
                },
                Float::valueOf
        );
    }

    // Load previous probabilities history
    private StorageManager<Date, Float> initStorageManager(String fileId) {

        StorageManager<Date, Float> cm = openStorageManager(fileId);

        if (!cm.isReadable()) {
            cm.delete();
            cm = openStorageManager(fileId);
        }

        return cm;
    }

    private boolean validateAndSetProbability(Date when, float probability) {
        if (probability < 0 || 1 < probability) {
            return false;
        }
        if(when == null){
            when = new Date();
        }
        // Include this update into the history
        //TODO :@Matteo what is wrong with those lines
        TreeMap toWrite;
        try{
            toWrite = new TreeMap<>(Collections.singletonMap(when, probability));
            infectionHistory.write(toWrite);
        }catch(NullPointerException e){
            //TODO:[LOG]
            System.out.println("Tree map creating is laggy : TEST : "+when+" probability");
        }

//        try {

//        } catch (NullPointerException e) {
//            System.out.println("I,"+uniqueID+", failed with "+e.toString()+ " in TEST :");
//            e.printStackTrace();
//        }
        infectedWithProbability = probability;
        return true;
    }

    @Override
    public InfectionStatus getInfectionStatus() {

        // Only get status if all the updates are completed
        lock.lock();

        InfectionStatus result = myStatus;

        lock.unlock();

        return result;
    }

    @Override
    public boolean evolveInfection(Date when, InfectionStatus newStatus, float newProbability) {

        lock.lock();

        boolean result = false;

        try {
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

                result = true;
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public float getIllnessProbability() {

        lock.lock();

        float result = infectedWithProbability;

        lock.unlock();

        return result;
    }

    @Override
    public boolean setIllnessProbability(Date when, float probability) {

        lock.lock();

        boolean result = false;

        try {
            if (validateAndSetProbability(when, probability)) {
                setChanged();

                // Broadcast the update (status has NOT changed)
                notifyObservers(Optional.empty());
                result = true;
            }
        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public Map<Date, Float> getIllnessProbabilityHistory(Date since) {

        lock.lock();

        SortedMap<Date, Float> result;

        try {
            result = infectionHistory.filter((date, prob) -> date.after(since));
        } finally {
            lock.unlock();
        }

        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        lock.lock();

        boolean result = (obj instanceof Layman) &&
                ((Layman) obj).uniqueID.equals(this.uniqueID) &&
                ((Layman) obj).myStatus == this.myStatus &&
                ((Layman) obj).infectedWithProbability == this.infectedWithProbability;

        lock.unlock();

        return result;
    }

    @NonNull
    @Override
    public String toString() {

        lock.lock();

        String result = String.format("#%s: %s (p=%f)", uniqueID, myStatus, infectedWithProbability);

        lock.unlock();

        return result;
    }

    @Override
    public int hashCode() {

        lock.lock();

        int result = Objects.hash(uniqueID, myStatus, infectedWithProbability);

        lock.unlock();

        return result;
    }

    @Override
    public String getUniqueId() {

        // No need to lock, since uniqueID cannot be changed after object creation
        return uniqueID;
    }

    @Override
    protected void finalize() throws Throwable {

        lock.lock();

        super.finalize();
        infectionHistory.close();

        lock.unlock();
    }

    @Override
    public void deleteLocalProbabilityHistory() {

        lock.lock();

        try {
            // Delete current manager
            infectionHistory.delete();

            // Create a new one
            infectionHistory = initStorageManager(uniqueID);

            infectionHistory.read();
        } finally {
            lock.unlock();
        }

    }
}
