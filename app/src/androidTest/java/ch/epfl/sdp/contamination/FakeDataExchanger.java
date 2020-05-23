package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.databaseIO.DataReceiver;
import ch.epfl.sdp.contamination.databaseIO.DataSender;
import ch.epfl.sdp.identity.Account;
import ch.epfl.sdp.identity.User;

/**
 * This class, made to make testing other classes convenient, simulates the behavior of a regular datasender to firestore, but store info locally
 */
public class FakeDataExchanger implements DataSender, DataReceiver {

    HashMap<Date, Location> fakeFirebaseStore;
    private String userID;

    public FakeDataExchanger() {
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

    @Override
    public CompletableFuture<Set<Carrier>> getUserNearby(Location location, Date date) {
        return null;
    }

    @Override
    public CompletableFuture<Map<Carrier, Integer>> getUserNearbyDuring(Location location, Date startDate, Date endDate) {
        return null;
    }

    @Override
    public CompletableFuture<Location> getMyLastLocation(Account account) {
        return null;
    }
    @Override
    public SortedMap<Date, Location> getLastPositions(){
        return new TreeMap<>(fakeFirebaseStore);
    }

    @Override
    public CompletableFuture<Map<String, Object>> getNumberOfSickNeighbors(String userId) {
        return null;
    }

    @Override
    public CompletableFuture<Map<String, Object>> getRecoveryCounter(String userId) {
        return null;
    }
}
