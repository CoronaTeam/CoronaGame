package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;

import ch.epfl.sdp.Callback;

/**
 * This class makes testing easier
 */
public class FakeAnalyst implements InfectionAnalyst {
    Carrier carrier;
    public FakeAnalyst(){
        this.carrier = new Layman(Carrier.InfectionStatus.HEALTHY);
    }

    @Override
    public void updateInfectionPredictions(Location location, Date startTime, Callback<Void> callback) {

    }

    @Override
    public Carrier getCarrier() {
        return new Layman(carrier.getInfectionStatus(),carrier.getIllnessProbability());
    }

    @Override
    public boolean updateStatus(Carrier.InfectionStatus stat) {
        return false;
    }
}
