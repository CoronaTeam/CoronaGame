package ch.epfl.sdp.contamination;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.databaseIO.DataSender;
import ch.epfl.sdp.contamination.databaseIO.DataReceiver;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;
import static ch.epfl.sdp.firestore.FirestoreLabels.privateRecoveryCounter;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicAlertAttribute;
import static ch.epfl.sdp.storage.PositionHistoryManager.getLastPositions;


// TODO: @Ulysse, @Adrien, @Kevin, @Lucas, @Lucie: general info on ConcreteAnalysis

/**
 * Concrete implementation of InfectionAnalyst, which can be observed
 * It models the evolution of the disease.
 * A few general remarks about it:
 * - The distance between 2 Carriers is the main parameter that determines how infection probabilities
 * are updated
 * - If the Carrier does not meet anyone, his probability of being infected slowly decreases over
 * time
 * - The only exception to the above rule is when the Carrier is marked as infected (because of
 * infection probability exceeding a certain threshold or because he declares his infection)
 * Then, his status does NOT evolve until he marks himself as healthy
 */
public class ConcreteAnalysis implements InfectionAnalyst, Observer {

    private final ObservableCarrier me;
    private final DataReceiver receiver;

    public ConcreteAnalysis(ObservableCarrier carrier, DataReceiver receiver) {
        this.me = carrier;
        this.receiver = receiver;

        // Register as Carrier observer
        this.me.addObserver(this);
    }

    private void evolveCarrierStatus(Date when, float updatedProbability) {
        if (updatedProbability > CERTAINTY_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly ill, then I will be marked as INFECTED
            me.evolveInfection(when, INFECTED, updatedProbability);

        } else if (updatedProbability < ABSENCE_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly healthy, then I will be marked as HEALTHY
            me.evolveInfection(when, HEALTHY, updatedProbability);

        } else {
            me.evolveInfection(when, UNKNOWN, updatedProbability);
        }
    }

    private int countMeetingsWithInfected(Map<? extends Carrier, Integer> aroundMe) {
        int infectedMet = 0;

        for (Carrier c : aroundMe.keySet()) {
            if (c.getInfectionStatus() == INFECTED) {
                infectedMet++;
            }
        }

        return infectedMet;
    }

    private int calculateCumulativeSocialTime(Map<Carrier, Integer> suspectedContacts) {
        int cumulativeSocialTime = 0;
        for (Integer meetingLength : suspectedContacts.values()) {
            cumulativeSocialTime += meetingLength;
        }

        return cumulativeSocialTime;
    }

    private float calculateIncreaseAfterMeeting(int cumulativeSocialTime, Map.Entry<Carrier, Integer> meeting) {
        // Weight the addition of infection probability according to the relative
        float newWeight = meeting.getValue().floatValue() / cumulativeSocialTime;

        // TODO: [LOG]
        Log.e("INFECTION_PROCESS", "newWeight: " + newWeight);
        Log.e("INFECTION_PROCESS", "newContribution:" + newWeight * meeting.getKey().getIllnessProbability() * TRANSMISSION_FACTOR);

        return newWeight * meeting.getKey().getIllnessProbability() * TRANSMISSION_FACTOR;
    }

    private float determineUpdatedInfectionProbability(Date when, Map<Carrier, Integer> suspectedContacts) {

        // MODEL: If the Carrier is INFECTED, his/her probability & status should NOT be changed
        if (me.getInfectionStatus() == INFECTED) {
            return me.getIllnessProbability();
        }

        int cumulativeSocialTime = calculateCumulativeSocialTime(suspectedContacts);

        float increaseInProbability = 0f;

        for (Map.Entry<Carrier, Integer> c : suspectedContacts.entrySet()) {
            if (c.getKey().getInfectionStatus() == INFECTED && c.getValue() > WINDOW_FOR_INFECTION_DETECTION) {
                // MODEL: Being close to a person for more than WINDOW_FOR_INFECTION_DETECTION implies becoming infected
                return 1f;

            } else {
                // Updates the probability given the new contribution by Carrier c.getKey()
                increaseInProbability += calculateIncreaseAfterMeeting(cumulativeSocialTime, c);
            }
        }

        float updatedProbability = PROBABILITY_HISTORY_RETENTION_FACTOR * me.getIllnessProbability() + increaseInProbability;
        updatedProbability = Math.min(updatedProbability, 1f);

        return updatedProbability;
    }

    private Map<Carrier, Integer> identifySuspectContacts(Map<? extends Carrier, Integer> aroundMe) {

        assert aroundMe != null;

        for (Carrier imThere : aroundMe.keySet()) {
            // TODO: [LOG]
            Log.e("AROUND_ME", imThere.getUniqueId() + ": " + imThere.getInfectionStatus() + ", " + imThere.getIllnessProbability());
        }

        Map<Carrier, Integer> contactsWithDuration = new HashMap<>();


        // TODO: Exclude Me from dangerous people
        for (Map.Entry<? extends Carrier, Integer> person : aroundMe.entrySet()) {
            if (!person.getKey().getUniqueId().equals(me.getUniqueId())) {
                // MODEL: Meeting periods are considered to be multiples of WINDOW_FOR_LOCATION_AGGREGATION
                int timeCloseBy = person.getValue() * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION;
                contactsWithDuration.put(person.getKey(), timeCloseBy);
            }
        }

        // TODO: [LOG]
        Log.e("IDENTIFY_SUSPECTS", contactsWithDuration.size() + " suspects");

        return contactsWithDuration;
    }

    private float getFactor(int recoveryCounter) {
        return (float) (Math.pow(InfectionAnalyst.IMMUNITY_FACTOR, recoveryCounter) * TRANSMISSION_FACTOR);
    }

    /**
     * Recalculate the probability that the Carrier is infected
     *
     * @param location
     * @param startTime
     * @return
     */
    @Override
    public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {

        CompletableFuture<Integer> recoveryCounter =
                receiver.getRecoveryCounter(me.getUniqueId())
                        .thenApply(rc -> (int) (rc.getOrDefault(privateRecoveryCounter, 0)));

        CompletableFuture<Map<Carrier, Integer>> peopleAroundMe =
                receiver.getUserNearbyDuring(location, startTime, endTime);

        // TODO: [LOG]
        Log.e("PEOPLE_AROUND_ME", "Searching for neighbors...");
        //Log.e("PEOPLE_AROUND_ME", "Neighbours found (" + peopleAroundMe.join().size() + ")");

        CompletableFuture<Float> badMeetingCoefficient =
                receiver.getNumberOfSickNeighbors(me.getUniqueId())
                        .thenApply(res -> (float) (res.getOrDefault(publicAlertAttribute, 0f)));

        return CompletableFuture.allOf(recoveryCounter, peopleAroundMe, badMeetingCoefficient)
                .thenRun(() -> dispatchModelUpdates(endTime, recoveryCounter.join(), identifySuspectContacts(peopleAroundMe.join()), badMeetingCoefficient.join()))
                .thenApply(v -> countMeetingsWithInfected(peopleAroundMe.join()));
    }

    private void dispatchModelUpdates(Date when, int recoveryCounter, Map<Carrier, Integer> suspectContacts, float badMeetingsCoefficient) {
        evolveCarrierStatus(
                when,
                Math.min(determineUpdatedInfectionProbability(when, suspectContacts) + badMeetingsCoefficient * getFactor(recoveryCounter), 1f)
        );
        DataSender.resetSickAlerts(me.getUniqueId());
    }

    @Override
    public ObservableCarrier getCarrier() {
        return me;
    }

    private void notifyNeighborsOfInfection(float previousIllnessProbability) {
        //1: retrieve your own last positions
        SortedMap<Date, Location> lastPositions = getLastPositions();

        //2: Ask firebase who was there
        Set<String> userIds = new HashSet<>();
        lastPositions.forEach((date, location) -> receiver.getUserNearby(location, date).thenAccept(around -> {
            around.forEach(neighbor -> {
                // Inform once only non-INFECTED users
                if (neighbor.getInfectionStatus() != INFECTED) {
                    userIds.add(neighbor.getUniqueId());
                }
            });
        }));

        // Tell those user that they have been close to you
        // TODO: @Lucas discuss whether considering only the previous Illness probability is good
        userIds.forEach(u -> notifyNeighborsOfMyInfection(u,previousIllnessProbability));
    }

    public void notifyNeighborsOfMyInfection(String u, float previousIllnessProbability){

        //TODO: [LOG]
        System.out.println("TEST GETLASTPOSITION IN");
        DataSender.sendAlert(u, previousIllnessProbability);
    }

    @Override
    public void update(Observable o, Object arg) {
        Optional<Float> probabilityIfStatusChanged = (Optional<Float>) arg;

        // TODO: @Lucas, before alerts where sent when updateStatus() was called. That missed
        // changes made directly to the Carrier. Hence, now the Carrier notify ConcreteAnalysis (calls update())
        // when its status change. To adapt tests, you have then to directly modify 'me' status

        if (probabilityIfStatusChanged.isPresent() && me.getInfectionStatus() == INFECTED) {
            // If there was a STATUS TRANSITION and the user is now INFECTED, then send alerts
            AsyncTask.execute(() -> notifyNeighborsOfInfection(probabilityIfStatusChanged.get()));
        }
    }
}
   