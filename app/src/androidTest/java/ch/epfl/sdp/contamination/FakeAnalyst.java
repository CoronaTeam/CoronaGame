package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;

/**
 * This class makes testing easier
 */
public class FakeAnalyst implements InfectionAnalyst {
    Carrier carrier;
    public FakeAnalyst(){
        this.carrier = new Layman(Carrier.InfectionStatus.HEALTHY);
    }
    @Override
    public void updateInfectionPredictions(Location location, Date startTime) {
        //This method is void for aggregationTests
    }

    @Override
    public Carrier getCurrentCarrier() {
        return new Layman(carrier.getInfectionStatus(),carrier.getIllnessProbability());
    }
}
