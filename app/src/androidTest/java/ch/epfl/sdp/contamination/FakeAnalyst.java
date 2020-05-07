package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * This class makes testing easier
 */
public class FakeAnalyst implements InfectionAnalyst {
    Carrier carrier;
    static int infectMeets = 0 ;

    public FakeAnalyst(Carrier originalCarrier) {
        carrier = originalCarrier;
    }

    public FakeAnalyst(){
        this.carrier = new Layman(Carrier.InfectionStatus.HEALTHY);
    }

    @Override
    public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {
        return CompletableFuture.completedFuture(infectMeets);
    }

    @Override
    public Carrier getCarrier() {
        return carrier;
    }

    @Override
    public boolean updateStatus(Carrier.InfectionStatus stat) {
        return carrier.evolveInfection(stat);
    }
}
