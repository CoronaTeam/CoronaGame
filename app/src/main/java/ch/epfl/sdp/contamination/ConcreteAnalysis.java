package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus;

public class ConcreteAnalysis implements InfectionAnalyst {

    private Carrier me = new Layman(InfectionStatus.HEALTHY);

    private DataReceiver receiver = new ConcreteReceiver();

    private Map<Carrier, Integer> trackMyContacts(Date startTime, Date endTime) {

        Map<Carrier, Integer> contactDuration = new HashMap<>();

        Set<? extends Carrier> aroundMe = receiver.getUserNearbyDuring(receiver.getMyLocationAtTime(startTime), startTime, endTime);

        for (Carrier person : aroundMe) {
            switch (person.getInfectionStatus()) {
                case INFECTED:
                case UNKNOWN:
                case HEALTHY_CARRIER:
                    int timeCloseBy = 0;
                    if (contactDuration.containsKey(person)) {
                        timeCloseBy = contactDuration.get(person);
                    }
                    timeCloseBy += PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION; // Add discretized time slice
                    contactDuration.put(person, timeCloseBy);
                    break;
                default:
                    // Do nothing: this carrier does not affect my status
            }
        }

        return contactDuration;
    }

    @Override
    public void updateInfectionPredictions(Date startTime) {

        Date now = new Date(System.currentTimeMillis());

        switch (me.getInfectionStatus()) {
            case INFECTED:
                // MODEL: infected people should update their status when they become healthy again
            case IMMUNE:
                // No matter what, I will remain so
                break;
            default:
                Map<Carrier, Integer> suspectedContacts = trackMyContacts(startTime, now);

                float cumulativeSocialTime = 0;

                for (int cTime : suspectedContacts.values()) {
                    cumulativeSocialTime += cTime;
                }

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

                if (updatedProbability > CERTAINTY_APPROXIMATION_THRESHOLD) {
                    // MODEL: If I'm almost certainly ill, then I will be marked as INFECTED
                    me.evolveInfection(InfectionStatus.INFECTED);
                } else {
                    me.setIllnessProbability(updatedProbability);
                }
        }
    }
}
