package ch.epfl.sdp.contamination;

import android.location.Location;

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
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;

public class ConcreteAnalysis implements InfectionAnalyst {

    private final Carrier me;
    private final DataReceiver receiver;
    private final CachingDataSender cachedSender;

    public ConcreteAnalysis(Carrier me, DataReceiver receiver, CachingDataSender dataSender) {
        this.me = me;
        this.receiver = receiver;
        this.cachedSender = dataSender;
    }

    private void updateCarrierInfectionProbability(float updatedProbability) {
        if (updatedProbability > CERTAINTY_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly ill, then I will be marked as INFECTED
            me.evolveInfection(INFECTED);
        } else if (updatedProbability < ABSENCE_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly healthy, then I will be marked as HEALTHY
            me.evolveInfection(InfectionStatus.HEALTHY);
        } else {
            me.evolveInfection(InfectionStatus.UNKNOWN);
        }
        me.setIllnessProbability(updatedProbability);
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

    private float calculateCarrierInfectionProbability(Map<Carrier, Integer> suspectedContacts) {
        int cumulativeSocialTime = 0;

        for (Integer meetingLength : suspectedContacts.values()) {
            cumulativeSocialTime += meetingLength;
        }

        float updatedProbability = me.getIllnessProbability();

        for (Map.Entry<Carrier, Integer> c : suspectedContacts.entrySet()) {
            // MODEL: Being close to a person for more than WINDOW_FOR_INFECTION_DETECTION implies becoming infected
            if (c.getValue() > WINDOW_FOR_INFECTION_DETECTION) {
                me.evolveInfection(InfectionStatus.INFECTED);
                updatedProbability = 1;
                break;
            } else {

                // Calculate new weights for probabilities
                float newWeight = c.getValue() / cumulativeSocialTime;
                float oldWeight = 1 - newWeight;

                // Updates the probability given the new contribution by Carrier c.getKey()
                updatedProbability = oldWeight * updatedProbability +
                        newWeight * c.getKey().getIllnessProbability();
            }
        }

        return updatedProbability;
    }

    private Map<Carrier, Integer> identifySuspectContacts(Map<? extends Carrier, Integer> aroundMe) {

        assert aroundMe != null;

        Map<Carrier, Integer> contactsWithDuration = new HashMap<>();

        for (Map.Entry<? extends Carrier, Integer> person : aroundMe.entrySet()) {
            if (person.getKey().getInfectionStatus() == UNKNOWN) {
                // MODEL: Each meeting is assumed to last for a fixed amount of time
                int timeCloseBy = person.getValue() * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION;
                contactsWithDuration.put(person.getKey(), timeCloseBy);
            }
        }

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

        CompletableFuture<Integer> recoveryCounter =
                receiver.getRecoveryCounter(me.getUniqueId())
                        .thenApply(rc -> (int) (rc.getOrDefault(privateRecoveryCounter, 0)));

        CompletableFuture<Map<Carrier, Integer>> peopleAroundMe =
                receiver.getUserNearbyDuring(location, startTime, endTime);

        CompletableFuture<Float> badMeetingCoefficient =
                receiver.getNumberOfSickNeighbors(me.getUniqueId())
                .thenApply(res -> (float) (res.getOrDefault(publicAlertAttribute, 0)));

        return CompletableFuture.allOf(recoveryCounter, peopleAroundMe, badMeetingCoefficient)
                .thenRun(() -> dispatchModelUpdates(recoveryCounter.join(), identifySuspectContacts(peopleAroundMe.join()), badMeetingCoefficient.join()))
                .thenApply(v -> countMeetingsWithInfected(peopleAroundMe.join()));
    }

    private void dispatchModelUpdates(int recoveryCounter, Map<Carrier, Integer> suspectContacts, float badMeetingsCoefficient) {
        // TODO: @Lucas, I don't understand why you put this condition, the model must be always updated
        if (badMeetingsCoefficient > 0) {
            updateCarrierInfectionProbability(
                    Math.min(calculateCarrierInfectionProbability(suspectContacts) + badMeetingsCoefficient * getFactor(recoveryCounter), 1f));
            cachedSender.resetSickAlerts(me.getUniqueId());
        }
    }

    @Override
    public Carrier getCarrier() {
        return me;
    }

    @Override
    public boolean updateStatus(InfectionStatus stat) {
        if (stat != me.getInfectionStatus()) {
            float previousIllnessProbability = me.getIllnessProbability();
            me.evolveInfection(stat);
            if (stat == INFECTED) {
                //Now, retrieve all user that have been nearby the last UNINTENTIONAL_CONTAGION_TIME milliseconds

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
            }
            return true;
        } else {
            return false;
        }
    }
}
   