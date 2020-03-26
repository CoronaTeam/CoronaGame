package ch.epfl.sdp.contamination;

import android.location.Location;
import android.util.Pair;

import java.util.Date;
import java.util.HashMap;

import ch.epfl.sdp.AccountGetting;
import ch.epfl.sdp.User;

/**
 * This class, made to make testing other classes convenient, simulates the behavior of a regular datasender to firestore, but store info locally
 */
public final class FakeDataSender implements DataSender{
    HashMap<Pair<Date,Location>,String> firebaseStore;
    private String userID;
    public FakeDataSender(){
        this.firebaseStore = new HashMap<>();
        String userID = User.DEFAULT_USERID;
    }
    @Override
    public void sendALocationToFirebase(RoundLocation location, Date time) {
        firebaseStore.put(new Pair<>(time,location),userID);
    }
}
