package ch.epfl.sdp.contamination;

import android.location.Location;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import ch.epfl.sdp.User;

/**
 * This class, made to make testing other classes convenient, simulates the behavior of a regular datasender to firestore, but store info locally
 */
public final class FakeCachingDataSender implements CachingDataSender {
    HashMap<Date, Location> firebaseStore;
    private String userID;
    public FakeCachingDataSender(){
        this.firebaseStore = new HashMap<>();
        String userID = User.DEFAULT_USERID;
    }
//    public void sendALocationToFirebase(Location location, Date time) {
//
//        firebaseStore.put(new Pair<Date,Location>(time, DataSender.RoundAndExpandLocation(location)),userID);
//    }

    /**
     * Again, this function is to be used only for testing
     * @return
     */
    public Map<Date, Location> getMap(){
        if(firebaseStore.size() !=0){
            return Collections.unmodifiableMap(firebaseStore);
        }
        return null;
    }

    @Override
    public void registerLocation(Carrier carrier, Location location, Date time) {
        firebaseStore.put(time, location);
    }

    @Override
    public void sendAlert(String userId) {

    }

    @Override
    public void resetSickAlerts(String userId) {

    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {
        return null;
    }
}
