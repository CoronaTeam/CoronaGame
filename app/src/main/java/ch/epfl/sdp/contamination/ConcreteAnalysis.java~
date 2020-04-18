package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import ch.epfl.sdp.Callback;

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

        private float calculateCarrierInfectionProbability(Map<Carrier, Integer> suspectedContacts, float cumulativeSocialTime) {
            float updatedProbability = me.getIllnessProbability();

            for (Map.Entry<Carrier, Integer> c : suspectedContacts.entrySet()) {
                // MODEL: Being close to a person for more than WINDOW_FOR_INFECTION_DETECTION implies becoming infected
                if (c.getValue() > WINDOW_FOR_INFECTION_DETECTION) {
                    me.evolveInfection(InfectionStatus.INFECTED);
                    break;
                } else {

                    // Calculate new weights for probabilities
                    float newWeight = c.getValue() / cumulativeSocialTime;
                    float oldWeight = 1 - newWeight;

                    // Updates the probability given the new contribution by Carrier c.getKey()
                    updatedProbability = oldWeight * updatedProbability +
                            newWeight * c.getKey().getIllnessProbability() * TRANSMISSION_FACTOR;
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

        private void modelInfectionEvolution(Map<Carrier, Integer> suspectedContacts) {

            switch (me.getInfectionStatus()) {
                case INFECTED:
                    // MODEL: infected people should update their status when they become healthy again
                    break;
                case IMMUNE:
                    // No matter what, I will remain so
                    break;
                default:
                    float cumulativeSocialTime = 0;
                    for (int cTime : suspectedContacts.values()) {
                        cumulativeSocialTime += cTime;
                    }

                    float updatedProbability = calculateCarrierInfectionProbability(suspectedContacts, cumulativeSocialTime);
                    updateCarrierInfectionProbability(updatedProbability);
            }
        }

        private Map<Carrier, Integer> identifySuspectContacts(Map<? extends Carrier, Integer> aroundMe) {
            Map<Carrier, Integer> contactDuration = new HashMap<>();

            for (Map.Entry<? extends Carrier, Integer> person : aroundMe.entrySet()) {
                switch (person.getKey().getInfectionStatus()) {
                    case INFECTED:
                    case UNKNOWN:
                    case HEALTHY_CARRIER:
                        int timeCloseBy = person.getValue() * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION; // Add discretized time slice
                        contactDuration.put(person.getKey(), timeCloseBy);
                        break;
                    default:
                        // Do nothing: this carrier does not affect my status
                }
            }

            return contactDuration;
        }

        @Override
        public void updateInfectionPredictions(Location location, Date startTime, Callback<Void> callback) {

            Date now = new Date(System.currentTimeMillis());
            receiver.getUserNearbyDuring(location, startTime, now, aroundMe -> {
                modelInfectionEvolution(identifySuspectContacts(aroundMe));
                callback.onCallback(null);
            });

            //Method 1 : (synchrone)
//        int badMeetings = receiver.getAndResetSickNeighbors(me.getUniqueId());
//        updateCarrierInfectionProbability(me.getIllnessProbability() + badMeetings*TRANSMISSION_FACTOR);

            //method 2 : (asynchrone)
            receiver.getNumberOfSickNeighbors(me.getUniqueId(), res -> {
                float badMeetings = 0;
                if(!((Map)(res)).isEmpty()){
                    badMeetings =  ((float) (((HashMap) (res)).get(publicAlertAttribute)));
                }
                updateCarrierInfectionProbability(me.getIllnessProbability() + badMeetings * TRANSMISSION_FACTOR);
                cachedSender.resetSickAlerts(me.getUniqueId());
            });
        }

        @Override
        public Carrier getCarrier() {
            return me;
        }

        @Override
        public boolean updateStatus(InfectionStatus stat) {
            if(stat != me.getInfectionStatus()){
                float previousIllnessProbability = me.getIllnessProbability();
                me.evolveInfection(stat);
                if(stat == InfectionStatus.INFECTED) {
                    //Now, retrieve all user that have been nearby the last UNINTENTIONAL_CONTAGION_TIME milliseconds

                    //1: retrieve your own last positions
                    SortedMap<Date,Location> lastPositions = cachedSender.getLastPositions();

                    //2: Ask firebase who was there

                    Set<String> userIds = new HashSet<>();
                    lastPositions.forEach((date,location) -> receiver.getUserNearby(location,date,around->{
                        around.forEach(neighbor -> {
                            if(neighbor.getInfectionStatus()!= InfectionStatus.INFECTED){ // only non-infected users need to be informed
                                userIds.add(neighbor.getUniqueId()); //won't add someone already in the set
                            }
                        });
                    }));
                    //Tell those user that they have been close to you
                    //TODO: discuss whether considering only the previous Illness probability is good
                    userIds.forEach(u -> cachedSender.sendAlert(u,previousIllnessProbability));
                }
                return true;
            }else{
                return false;
            }
        }

    }