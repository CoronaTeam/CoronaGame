package ch.epfl.sdp.contamination;

import java.util.Date;
import java.util.Set;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus;

public class ConcreteAnalysis implements InfectionAnalyst {

    private Carrier me = new Layman(InfectionStatus.HEALTHY);

    private DataReceiver receiver = new ConcreteReceiver();

    @Override
    public void updateInfectionPredictions(Date startTime) {

        Date now = new Date(System.currentTimeMillis());

        if (me.getInfectionStatus() == InfectionStatus.INFECTED) {
            // Do nothing: infected people should update their status when they become healthy again
        } else {
            Set<? extends Carrier> aroundMe = receiver.getUserNearby(receiver.getMyLocationAtTime(startTime), startTime);

            for (Carrier person : aroundMe) {
                if (person.getInfectionStatus() == InfectionStatus.INFECTED) {

                }
            }
        }
    }
}
