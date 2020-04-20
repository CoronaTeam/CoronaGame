package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * This class makes testing easier
 */
public class FakeAnalyst implements InfectionAnalyst {
    Carrier carrier;
    public FakeAnalyst(){
        this.carrier = new Layman(Carrier.InfectionStatus.HEALTHY);
    }

    @Override
    public CompletableFuture<Void> updateInfectionPredictions(Location location, Date startTime) {
        return null;
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
