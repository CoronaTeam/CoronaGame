package ch.epfl.sdp.contamination;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import static ch.epfl.sdp.contamination.CachingDataSender.privateRecoveryCounter;
import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;

public class ConcreteAnalysis implements InfectionAnalyst {

    private final Carrier me;
    private final DataReceiver receiver;
    private final CachingDataSender cachedSender;

    public ConcreteAnalysis(Carrier me, DataReceiver receiver, CachingDataSender dataSender) {
        this.me = me;
        this.receiver = receiver;
        this.cachedSender = dataSender;
    }

    private void updateCarrierInfectionProbability(Date when, float updatedProbability) {
        if (updatedProbability > CERTAINTY_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly ill, then I will be marked as INFECTED
            me.evolveInfection(when, INFECTED);
        } else if (updatedProbability < ABSENCE_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly healthy, then I will be marked as HEALTHY
            me.evolveInfection(when, InfectionStatus.HEALTHY);
        } else {
            me.evolveInfection(when, InfectionStatus.UNKNOWN);
        }
        me.setIllnessProbability(when, updatedProbability);
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

    private float calculateCarrierInfectionProbability(Date when, Map<Carrier, Integer> suspectedContacts) {
        int cumulativeSocialTime = 0;

        for (Integer meetingLength : suspectedContacts.values()) {
            cumulativeSocialTime += meetingLength;
        }

        float updatedProbability = me.getIllnessProbability();

        for (Map.Entry<Carrier, Integer> c : suspectedContacts.entrySet()) {
            // MODEL: Being close to a person for more than WINDOW_FOR_INFECTION_DETECTION implies becoming infected
            if (c.getValue() > WINDOW_FOR_INFECTION_DETECTION) {
                me.evolveInfection(when, InfectionStatus.INFECTED);
                updatedProbability = 1;
                break;
            } else {

                float newWeight = c.getValue().floatValue() / cumulativeSocialTime;
                float oldWeight = .5f;

                // Calculate new weights for probabilities

                // MODEL: Bias probability towards newer values
                // newWeight *= 4f;

                Log.e("INFECTION_PROCESS", Float.toString(oldWeight) + ", " + Float.toString(newWeight));

                Log.e("INFECTION_PROCESS", "oldContribution:" + Float.toString(oldWeight * updatedProbability) + ", newContribution:" + Float.toString(newWeight * c.getKey().getIllnessProbability() * TRANSMISSION_FACTOR));

                // Updates the probability given the new contribution by Carrier c.getKey()
                updatedProbability = oldWeight * updatedProbability +
                        newWeight * c.getKey().getIllnessProbability() * TRANSMISSION_FACTOR;

                updatedProbability = Math.min(updatedProbability, 1f);
                 /*

                float inversePoint = 0;

                if (updatedProbability > 0f) {
                    inversePoint = - 1f / TRANSMISSION_FACTOR * (float) log(1 - updatedProbability) / updatedProbability;
                }

                inversePoint = oldWeight * inversePoint +
                        newWeight * c.getKey().getIllnessProbability();

                updatedProbability = 1f / (1 + (float) exp(- TRANSMISSION_FACTOR * inversePoint));
                 */
            }
        }

        return updatedProbability;
    }

    private Map<Carrier, Integer> identifySuspectContacts(Map<? extends Carrier, Integer> aroundMe) {

        assert aroundMe != null;

        for (Carrier imThere : aroundMe.keySet()) {
            Log.e("AROUND_ME", imThere.getUniqueId() + ": " + imThere.getInfectionStatus() + ", " + imThere.getIllnessProbability());
        }

        Map<Carrier, Integer> contactsWithDuration = new HashMap<>();


        // TODO: Exclude Me from dangerous people
        for (Map.Entry<? extends Carrier, Integer> person : aroundMe.entrySet()) {
            if (!person.getKey().getUniqueId().equals(me.getUniqueId())) {
                // MODEL: Each meeting is assumed to last for a fixed amount of time
                int timeCloseBy = person.getValue() * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION;
                contactsWithDuration.put(person.getKey(), timeCloseBy);
            }
        }

        Log.e("IDENTIFY_SUSPECTS", Integer.toString(contactsWithDuration.size()) + " suspects");

        return contactsWithDuration;
    }

    private float getFactor(int recoveryCounter) {
        return (float) (Math.pow(InfectionAnalyst.IMMUNITY_FACTOR, recoveryCounter) * TRANSMISSION_FACTOR);
    }

    /**
     * this Method will now return the number of 100% sick person we met
     *
     * @param location
     * @param startTime
     * @return
     */
    @Override
    public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {

        // TODO: DISABLED this just for debug

        CompletableFuture<Integer> recoveryCounter =
                receiver.getRecoveryCounter(me.getUniqueId())
                        .thenApply(rc -> (int) (rc.getOrDefault(privateRecoveryCounter, 0)));

        // TODO: Remove these, DEBUG only !!!!!!!!
        Date fakeStartTime = new Date(startTime.getTime() - 40000);
        Date fakeEndTime = new Date(endTime.getTime() + 4000);
        CompletableFuture<Map<Carrier, Integer>> peopleAroundMe =
                receiver.getUserNearbyDuring(location, startTime, endTime);

        Log.e("PEOPLE_AROUND_ME", "Searching for neighbors...");
        Log.e("PEOPLE_AROUND_ME", "Neighbours found (" + peopleAroundMe.join().size() + ")");

        CompletableFuture<Float> badMeetingCoefficient =
                receiver.getNumberOfSickNeighbors(me.getUniqueId())
                .thenApply(res -> (float) (res.getOrDefault(publicAlertAttribute, 0f)));

        return CompletableFuture.allOf(recoveryCounter, peopleAroundMe, badMeetingCoefficient)
                .thenRun(() -> dispatchModelUpdates(endTime, recoveryCounter.join(), identifySuspectContacts(peopleAroundMe.join()), badMeetingCoefficient.join()))
                .thenApply(v -> countMeetingsWithInfected(peopleAroundMe.join()));

/*
        updateCarrierInfectionProbability(endTime, 1f);
        return CompletableFuture.completedFuture(0);
 */

    }

    private void dispatchModelUpdates(Date when, int recoveryCounter, Map<Carrier, Integer> suspectContacts, float badMeetingsCoefficient) {
        updateCarrierInfectionProbability(
                when,
                Math.min(calculateCarrierInfectionProbability(when, suspectContacts) + badMeetingsCoefficient * getFactor(recoveryCounter), 1f)
        );
        cachedSender.resetSickAlerts(me.getUniqueId());
    }

    @Override
    public Carrier getCarrier() {
        return me;
    }

    @Override
    public boolean updateStatus(InfectionStatus newStatus) {

        if (newStatus == me.getInfectionStatus()) {
            return false;
        }

        float previousIllnessProbability = me.getIllnessProbability();
        me.evolveInfection(newStatus);

        if (newStatus == INFECTED) {
            // TODO: Do we really need AsyncTask
            AsyncTask.execute(() -> {
                //1: retrieve your own last positions
                SortedMap<Date, Location> lastPositions = cachedSender.getLastPositions();

                //2: Ask firebase who was there
                Set<String> userIds = new HashSet<>();
                lastPositions.forEach((date, location) -> receiver.getUserNearby(location, date).thenAccept(around -> {
                    around.forEach(neighbor -> {
                        if (neighbor.getInfectionStatus() != INFECTED) { // only non-infected users need to be informed
                            userIds.add(neighbor.getUniqueId()); //won't add someone already in the set
                        }
                    });
                }));

                //Tell those user that they have been close to you
                //TODO: discuss whether considering only the previous Illness probability is good
                userIds.forEach(u -> cachedSender.sendAlert(u, previousIllnessProbability));
            });
        }

        return true;
    }
}
   