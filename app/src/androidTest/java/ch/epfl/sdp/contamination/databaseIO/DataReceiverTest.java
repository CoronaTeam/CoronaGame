package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.identity.User;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicAlertAttribute;
import static org.junit.Assert.assertEquals;

public class DataReceiverTest {
    DataReceiver receiver;
    ConcreteCachingDataSender sender;

    @Before
    public void init() {
        sender = new ConcreteCachingDataSender(new GridFirestoreInteractor());
        receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());

        /*receiver = fragment.getLocationService().getReceiver();
        sender = (ConcreteCachingDataSender)fragment.getLocationService().getSender();*/
    }

    @Test
    public void getSickNeighborDoesGetIt() {
        sender.sendAlert(User.DEFAULT_USERID).thenRun(() ->
                receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                        assertEquals(1f, ((float) (double) (res.get(publicAlertAttribute))),
                                0.00001)));
        sleep();
    }

    class FakeGridInteractor extends GridFirestoreInteractor {
        private Map<Location, String> locationData;
        private Map<String, Integer> meetings;

        public FakeGridInteractor() {
            super();
            this.locationData = new HashMap<>();
            this.meetings = new HashMap<>();
        }

        @Override
        public CompletableFuture<Void> gridWrite(Location location, String time, Carrier carrier) {
            Location genericLoc = newLoc(location.getLongitude(), location.getLatitude());
            genericLoc.setTime(Integer.valueOf(time));
            locationData.put(location, carrier.getUniqueId());
            return CompletableFuture.completedFuture(null);
        }

        public void addMeeting() {
            int previous = meetings.getOrDefault(User.DEFAULT_USERID, 0);
            if (previous == 0) {
                meetings.put(User.DEFAULT_USERID, 1);
            } else {
                meetings.replace(User.DEFAULT_USERID, previous + 1);
            }
        }

        public Integer readDocument(String path, String documentID) {
            return meetings.get(documentID);
        }
    }

    //TODO: test with a non-empty fake grid

}
