package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Collections;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class ConcreteCachingDataSender implements CachingDataSender {
    SortedMap<Date, Location> lastPositions;
    private GridFirestoreInteractor gridInteractor;

    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
        this.lastPositions = new TreeMap<>();
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
        refreshLastPositions(time, location);
        return gridInteractor.gridWrite(location, String.valueOf(time.getTime()), carrier);
    }

    /**
     * removes every locations older than UNINTENTIONAL_CONTAGION_TIME ms and adds a new position
     */
    private void refreshLastPositions(Date time, Location location) {
        Date oldestDate = new Date(time.getTime() - InfectionAnalyst.UNINTENTIONAL_CONTAGION_TIME);
        lastPositions.headMap(oldestDate).clear();
        if (location != null) {
            lastPositions.put(time, location);
        }
    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {
        refreshLastPositions(new Date(System.currentTimeMillis()), null);
        return Collections.unmodifiableSortedMap(lastPositions);
    }
}
