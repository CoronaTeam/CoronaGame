package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.SortedMap;

import ch.epfl.sdp.User;

/**
 * This class, made to make testing other classes convenient, simulates the behavior of a regular datasender to firestore, but store info locally
 */
public class FakeCachingDataSender implements CachingDataSender {
    HashMap<Date, Location> fakeFirebaseStore;
    private String userID;
    public FakeCachingDataSender(){
        this.fakeFirebaseStore = new HashMap<>();
        String userID = User.DEFAULT_USERID;
    }
    /**
     * Again, this function is to be used only for testing
     * @return
     */
    public Map<Date, Location> getMap(){
        if(fakeFirebaseStore.size() !=0){
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
        return null;
    }
}
