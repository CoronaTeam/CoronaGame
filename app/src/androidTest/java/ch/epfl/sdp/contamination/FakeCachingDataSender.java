package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.databaseIO.CachingDataSender;
import ch.epfl.sdp.identity.User;

/**
 * This class, made to make testing other classes convenient, simulates the behavior of a regular
 * dataSender to firestore, but store info locally
 */
public class FakeCachingDataSender implements CachingDataSender {

    final HashMap<Date, Location> fakeFirebaseStore;

    public FakeCachingDataSender() {
        this.fakeFirebaseStore = new HashMap<>();
    }

    /**
     * Again, this function is to be used only for testing
     *
     * @return
     */
    public Map<Date, Location> getMap() {
        if (fakeFirebaseStore.size() != 0) {
            return Collections.unmodifiableMap(fakeFirebaseStore);
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
        fakeFirebaseStore.put(time, location);
        return null;
    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {
        return new TreeMap<>(fakeFirebaseStore);
    }
}
