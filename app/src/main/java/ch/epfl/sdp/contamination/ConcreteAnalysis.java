package ch.epfl.sdp.contamination;

import android.location.Location;
import android.util.Pair;

import java.util.Collections;
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

public class ConcreteAnalysis implements InfectionAnalyst {

    private final Carrier me;
    private final DataReceiver receiver;
    private final CachingDataSender cachedSender;

    public ConcreteAnalysis(Carrier me, DataReceiver receiver, CachingDataSender dataSender) {
        this.me = me;
        this.receiver = receiver;
        this.cachedSender = dataSender;
    }

    private float calculateCarrierInfectionProbability(Map<Carrier, Integer> suspectedContacts, float cumulativeSocialTime, int recoveryCounter) {
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
                        newWeight * c.getKey().getIllnessProbability() * getFactor(recoveryCounter);
            }
        }

        return updatedProbability;
    }

    private void updateCarrierInfectionProbability(float updatedProbability) {
        if (updatedProbability > CERTAINTY_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly ill, then I will be marked as INFECTED
            me.evolveInfection(InfectionStatus.INFECTED);
        } else if (updatedProbability < ABSENCE_APPROXIMATION_THRESHOLD) {
            // MODEL: If I'm almost certainly healthy, then I will be marked as HEALTHY
            me.evolveInfection(InfectionStatus.HEALTHY);
        } else {
            me.evolveInfection(InfectionStatus.UNKNOWN);
        }
        me.setIllnessProbability(updatedProbability);
    }

    private void modelInfectionEvolution(Map<Carrier, Integer> suspectedContacts,
                                         int recoveryCounter) {

        switch (me.getInfectionStatus()) {
            case INFECTED:
                // MODEL: infected people should update their status when they become healthy again
                break;
            default:
                float cumulativeSocialTime = 0;
                for (int cTime : suspectedContacts.values()) {
                    cumulativeSocialTime += cTime;
                }

                float updatedProbability = calculateCarrierInfectionProbability(suspectedContacts, cumulativeSocialTime, recoveryCounter);
                updateCarrierInfectionProbability(updatedProbability);
        }
    }

    private Pair<Map<Carrier, Integer>, Integer> identifySuspectContacts_countInfected(Map<? extends Carrier, Integer> aroundMe) {
        if (aroundMe == null) {
            return new Pair(Collections.emptyMap(), 0);
        }
        Map<Carrier, Integer> contactDuration = new HashMap<>();
        int infectionCounter = 0;
        for (Map.Entry<? extends Carrier, Integer> person : aroundMe.entrySet()) {
            switch (person.getKey().getInfectionStatus()) {
                case INFECTED:
                    infectionCounter += 1;
                case UNKNOWN:
                    int timeCloseBy = person.getValue() * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION; // Add discretized time slice
                    contactDuration.put(person.getKey(), timeCloseBy);
                    break;
                default:
                    // Do nothing: this carrier does not affect my status
            }
        }

        return new Pair<Map<Carrier, Integer>, Integer>(contactDuration, infectionCounter);
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
    public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime) {
        Date now = new Date(System.currentTimeMillis());

        CompletableFuture<Integer> counterFuture = getCounterCompletableFuture();

        CompletableFuture<Pair<Map<Carrier, Integer>, Integer>> suspicionsFuture =
                getSuspiciousCompletableFuture(location, startTime, now);

        CompletableFuture<Void> badMeetingsFuture = getBadMeetingCompletableFuture(counterFuture);

        return counterFuture
                .thenCombine(suspicionsFuture, (counter, suspicions) -> {
                    modelInfectionEvolution(suspicions.first, counter);
                    return suspicions.second;
                }).thenCombine(badMeetingsFuture, ((integer, aVoid) -> integer));
    }

    private CompletableFuture<Integer> getCounterCompletableFuture() {
        return receiver.getRecoveryCounter(me.getUniqueId()).thenApply(recoveryCounter -> {
            int recoveryCounter1 = 0;
            if (!recoveryCounter.isEmpty()) {
                recoveryCounter1 = (int) recoveryCounter.get(privateRecoveryCounter);
            }
            return recoveryCounter1;
        });
    }

    private CompletableFuture<Void> getBadMeetingCompletableFuture(CompletableFuture<Integer> counterFuture) {
        return receiver.getNumberOfSickNeighbors(me.getUniqueId()).thenAcceptBoth(counterFuture,
                (res, counter) -> {
                    float badMeetings = 0;
                    if (!res.isEmpty()) {
                        badMeetings = (float) (res.get(publicAlertAttribute));
                    }
                    if (badMeetings != 0) {
                        updateCarrierInfectionProbability(Math.min(me.getIllnessProbability()
                                + badMeetings * getFactor(counter), 1f));
                        cachedSender.resetSickAlerts(me.getUniqueId());
                    }
                });
    }

    private CompletableFuture<Pair<Map<Carrier, Integer>, Integer>> getSuspiciousCompletableFuture(Location location, Date startTime, Date now) {
        return receiver.getUserNearbyDuring(location, startTime, now).thenApply(aroundMe -> {
            Pair<Map<Carrier, Integer>, Integer> suspicions;
            suspicions = identifySuspectContacts_countInfected(aroundMe);
            return suspicions;
        });
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
            if (stat == InfectionStatus.INFECTED) {
                //Now, retrieve all user that have been nearby the last UNINTENTIONAL_CONTAGION_TIME milliseconds

                //1: retrieve your own last positions
                SortedMap<Date, Location> lastPositions = cachedSender.getLastPositions();

                //2: Ask firebase who was there

                Set<String> userIds = new HashSet<>();
                lastPositions.forEach((date, location) -> receiver.getUserNearby(location, date).thenAccept(around -> {
                    around.forEach(neighbor -> {
                        if (neighbor.getInfectionStatus() != InfectionStatus.INFECTED) { // only non-infected users need to be informed
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
   