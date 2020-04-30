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

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.R;
import ch.epfl.sdp.User;

import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.initSafeTest;
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
        InfectionFragment fragment = ((InfectionFragment)((InfectionActivity)(getActivity())).getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        initSafeTest(mActivityRule,true);
        receiver = fragment.getLocationService().getReceiver();
        sender = (ConcreteCachingDataSender)fragment.getLocationService().getSender();
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
        sender.resetSickAlerts(User.DEFAULT_USERID);
        sleep(3000);
        sender.sendAlert(User.DEFAULT_USERID);
        sleep(3000);
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID, res ->assertEquals(1f, ((float)(double) (((Map) (res)).get(publicAlertAttribute))),0.00001));
        sleep(6000);
    }

}
