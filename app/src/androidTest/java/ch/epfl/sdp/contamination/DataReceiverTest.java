package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.User;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.contamination.InfectionActivity.getReceiver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataReceiverTest {

    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);


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
    public void getAndResetDoesAGet(){
        FakeGridInteractor interactor = new FakeGridInteractor();
        ((ConcreteDataReceiver)getReceiver()).setInteractor(interactor);
        interactor.addMeeting();
        int res = getReceiver().getAndResetSickNeighbors(User.DEFAULT_USERID);

        assertEquals(1,res);
    }
    @Test
    public void getAndResetDoesReset(){
        long res = getReceiver().getAndResetSickNeighbors(User.DEFAULT_USERID);
        assertEquals(1024,res);
//        long res2 = getReceiver().getAndResetSickNeighbors(User.DEFAULT_USERID);
//        assertEquals(0,res2);
    }

}
