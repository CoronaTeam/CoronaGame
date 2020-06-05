package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * This class makes testing easier
 */
public class FakeAnalyst implements InfectionAnalyst {
    static int infectMeets = 0;
    private ObservableCarrier carrier;

    public FakeAnalyst(ObservableCarrier originalCarrier) {
        carrier = originalCarrier;
    }

    public FakeAnalyst() {
        this.carrier = new Layman(Carrier.InfectionStatus.HEALTHY);
    }

    @Override
    public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {
        return CompletableFuture.completedFuture(infectMeets);
    }

    @Override
    public ObservableCarrier getCarrier() {
        return carrier;
    }
}
