package ch.epfl.sdp.contamination;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.User;

import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CachingDataSenderTest {
    DataReceiver receiver;
    @Before
    public void init(){
        receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
    }

    @Test
    public void RoundAndExpandLocationIsCorrect(){
        Location location = new Location("provider");
        location.setLatitude(12.1234567);
        location.setLongitude(134.9876543);
        CachingDataSender.RoundAndExpandLocation(location);
        Location manuallyRoundedLocation = new Location("provider");
        manuallyRoundedLocation.setLongitude(13498765);
        manuallyRoundedLocation.setLatitude(1212346);
        assertTrue(TestTools.expandedLocEquals(manuallyRoundedLocation,location));
//        assertEquals(manuallyRoundedLocation.getLongitude(),location.getLongitude(),0);
//        assertEquals(manuallyRoundedLocation.getLatitude(),location.getLatitude(),0);
    }
    @Test
    public void resetAlertsDeletesAlertAttribute(){
        CachingDataSender.sendAlert(User.DEFAULT_USERID);
        CachingDataSender.resetSickAlerts(User.DEFAULT_USERID);
//        DataReceiver receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());<
        receiver.getSickNeighbors(User.DEFAULT_USERID, res -> assertTrue(((HashMap)(res)).isEmpty()));
        sleep();
    }
    @Test
    public void sendAlertIncrementsOrCreatesAlertAttribute(){
        CachingDataSender.resetSickAlerts(User.DEFAULT_USERID);
        CachingDataSender.sendAlert(User.DEFAULT_USERID);
        receiver.getSickNeighbors(User.DEFAULT_USERID,res -> assertFalse(((HashMap)(res)).isEmpty()));
        sleep();
        CachingDataSender.sendAlert(User.DEFAULT_USERID);
        receiver.getSickNeighbors(User.DEFAULT_USERID,res ->assertEquals(2,((int) ((long) (((HashMap) (res)).get(publicAlertAttribute))))));
        sleep();
    }
}
