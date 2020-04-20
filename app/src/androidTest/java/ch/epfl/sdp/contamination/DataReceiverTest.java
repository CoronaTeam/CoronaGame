package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.User;

import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;
import static org.junit.Assert.assertEquals;

public class DataReceiverTest {
    DataReceiver receiver;
    ConcreteCachingDataSender sender;
    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);

    @Before
    public void init(){
        receiver = ((InfectionActivity)(getActivity())).getLocationService().getReceiver();
//                getReceiver();
        sender = (ConcreteCachingDataSender)((InfectionActivity)(getActivity())).getLocationService().getSender();
//                (ConcreteCachingDataSender) getSender();
    }
    class FakeGridInteractor extends GridFirestoreInteractor {
        private Map<Location,String> locationData;
        private Map<String,Integer> meetings;

        public FakeGridInteractor(){
            super();
            this.locationData = new HashMap<>();
            this.meetings = new HashMap<>();
        }

        @Override
        public CompletableFuture<Void> gridWrite(Location location, String time, Carrier carrier) {
            Location genericLoc = newLoc(location.getLongitude(),location.getLatitude());
            genericLoc.setTime(Integer.valueOf(time));
            locationData.put(location,carrier.getUniqueId());
            return CompletableFuture.completedFuture(null);
        }

        public void addMeeting(){
            int previous = meetings.getOrDefault(User.DEFAULT_USERID,0);
            if(previous == 0){
                meetings.put(User.DEFAULT_USERID,1);
            }else{
                meetings.replace(User.DEFAULT_USERID,previous+1);
            }
        }

        public Integer readDocument(String path, String documentID) {
            return meetings.get(documentID);
        }
    }
    @Test
    public void getSickNeighborDoesGetIt(){
        sender.resetSickAlerts(User.DEFAULT_USERID);
        sender.sendAlert(User.DEFAULT_USERID).thenRun(() ->
                receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                        assertEquals(1f, ((float)(double) (res.get(publicAlertAttribute))),
                                0.00001)));
        sleep();
    }

}
