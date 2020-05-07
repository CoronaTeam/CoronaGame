package ch.epfl.sdp.contamination;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedMap;

import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.User;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;
import static ch.epfl.sdp.contamination.InfectionAnalyst.UNINTENTIONAL_CONTAGION_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CachingDataSenderTest {
    DataReceiver receiver;
    CachingDataSender sender;

    @Before
    public void init() {
        receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        sender = new ConcreteCachingDataSender(new GridFirestoreInteractor());
    }

    @Test
    public void RoundAndExpandLocationIsCorrect() {
        Location location = new Location("provider");
        location.setLatitude(12.1234567);
        location.setLongitude(134.9876543);
        CachingDataSender.RoundAndExpandLocation(location);
        Location manuallyRoundedLocation = new Location("provider");
        manuallyRoundedLocation.setLongitude(13498765);
        manuallyRoundedLocation.setLatitude(1212346);
        assertTrue(TestTools.expandedLocEquals(manuallyRoundedLocation, location));
//        assertEquals(manuallyRoundedLocation.getLongitude(),location.getLongitude(),0);
//        assertEquals(manuallyRoundedLocation.getLatitude(),location.getLatitude(),0);
    }

    @Test
    public void resetAlertsDeletesAlertAttribute() {
        sender.sendAlert(User.DEFAULT_USERID);
        sender.resetSickAlerts(User.DEFAULT_USERID);
//        DataReceiver receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());<
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                assertTrue(res.isEmpty()));
        sleep();
    }

    @Test
    public void sendAlertIncrementsOrCreatesAlertAttribute() {
        sender.resetSickAlerts(User.DEFAULT_USERID);
        sender.sendAlert(User.DEFAULT_USERID);
        sleep();
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                assertFalse(res.isEmpty()));
        sleep();
        sender.sendAlert(User.DEFAULT_USERID);
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                assertEquals(2, ((float) ((double) (res.get(publicAlertAttribute)))), 0.0001));
        sleep();
    }

    @Test
    public void getLastPositionsReturnsCorrectWindowOfLocations() {
        Layman me = new Layman(Carrier.InfectionStatus.HEALTHY);
        sender.registerLocation(me, newLoc(2, 2), new Date(System.currentTimeMillis() - 1 - UNINTENTIONAL_CONTAGION_TIME));
        sender.registerLocation(me, newLoc(1, 1), new Date(System.currentTimeMillis()));
        SortedMap<Date, Location> res = sender.getLastPositions();
        Collection<Location> val = res.values();
        Iterator<Location> it = val.iterator();
        assertTrue(val.size() == 1);
        while (it.hasNext()) {
            assertTrue(it.next().getLatitude() == 1);
        }
    }
}
