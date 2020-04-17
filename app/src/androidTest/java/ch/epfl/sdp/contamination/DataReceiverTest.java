package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.User;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;
import static ch.epfl.sdp.contamination.InfectionActivity.getReceiver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataReceiverTest {
    ConcreteDataReceiver receiver;
    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);

    @Before
    public void init(){
        receiver = ((ConcreteDataReceiver)getReceiver());
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
        public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
            Location genericLoc = newLoc(location.getLongitude(),location.getLatitude());
            genericLoc.setTime(Integer.valueOf(time));
            locationData.put(location,carrier.getUniqueId());
        }
        public void addMeeting(){
            int previous = meetings.getOrDefault(User.DEFAULT_USERID,0);
            if(previous == 0){
                meetings.put(User.DEFAULT_USERID,1);
            }else{
                meetings.replace(User.DEFAULT_USERID,previous+1);
            }
        }
        @Override
        public void readDocument(String path, String documentID, Callback callback) {
            callback.onCallback(meetings.get(documentID));
        }
    }
    @Test
    public void getSickNeighborDoesGetIt(){
        CachingDataSender.resetSickAlerts(User.DEFAULT_USERID);
        CachingDataSender.sendAlert(User.DEFAULT_USERID);
        receiver.getSickNeighbors(User.DEFAULT_USERID,res ->assertEquals(1,((int) ((long) (((HashMap) (res)).get(publicAlertAttribute))))));
        sleep();
    }

}
