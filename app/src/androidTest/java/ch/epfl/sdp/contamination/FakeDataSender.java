package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.databaseIO.DataSender;
import ch.epfl.sdp.identity.User;

/**
 * This class, made to make testing other classes convenient, simulates the behavior of a regular datasender to firestore, but store info locally
 */
public class FakeDataSender implements DataSender {

    HashMap<Date, Location> fakeFirebaseStore;
    private String userID;

    public FakeDataSender() {
        this.fakeFirebaseStore = new HashMap<>();
        String userID = User.DEFAULT_USERID;
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
    public CompletableFuture<Void> sendAlert(String userId, float previousIllnessProbability) {
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> sendAlert(String userId) {
        return sendAlert(userId, 0);
    }

    public CompletableFuture<Void> resetSickAlerts(String userId) {
        return CompletableFuture.completedFuture(null);
    }
    public SortedMap<Date, Location> getLastPositions() {
        return new TreeMap<>(fakeFirebaseStore);
    }
}
