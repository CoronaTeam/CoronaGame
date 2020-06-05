package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedMap;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.identity.User;
import ch.epfl.sdp.identity.fragment.AccountFragment;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.InfectionAnalyst.PRESYMPTOMATIC_CONTAGION_TIME;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicAlertAttribute;
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
        AccountFragment.IN_TEST = true;
    }

    @Test
    public void RoundAndExpandLocationIsCorrect() {
        Location location = new Location("provider");
        location.setLatitude(12.1234567);
        location.setLongitude(134.9876543);

        CachingDataSender.roundLocation(location);

        Location manuallyRoundedLocation = new Location("provider");
        manuallyRoundedLocation.setLongitude(134.98765);
        manuallyRoundedLocation.setLatitude(12.12346);
        assertEquals(manuallyRoundedLocation.getLongitude(), location.getLongitude(), 0.00000001f);
        assertEquals(manuallyRoundedLocation.getLatitude(), location.getLatitude(), 0.00000001f);
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
        sender.registerLocation(me, newLoc(2, 2), new Date(System.currentTimeMillis() - 1 - PRESYMPTOMATIC_CONTAGION_TIME));
        sender.registerLocation(me, newLoc(1, 1), new Date(System.currentTimeMillis()));
        SortedMap<Date, Location> res = sender.getLastPositions();
        Collection<Location> val = res.values();
        Iterator<Location> it = val.iterator();
        assertTrue(val.size() == 1);
        while (it.hasNext()) {
            assertTrue(it.next().getLatitude() == 1);
        }
    }

    @Test
    public void cacheWorksIfUsedTwice() {
        //During development, the above test failed if runed twice.
        getLastPositionsReturnsCorrectWindowOfLocations();
    }

    @Test
    public void stringToLocationWorks() {
        Location loc1 = ConcreteCachingDataSender.stringToLocation("1.000,2.456");
        assertEquals(loc1.getLatitude(), 1.0, 0.00001);
        assertEquals(loc1.getLongitude(), 2.456, 0.00001);
        loc1 = ConcreteCachingDataSender.stringToLocation("1.0001,2");
        assertEquals(loc1.getLatitude(), 1.0001, 0.00001);
        assertEquals(loc1.getLongitude(), 2.0, 0.00001);
        loc1 = ConcreteCachingDataSender.stringToLocation("0.0,0.0");
        assertEquals(loc1.getLatitude(), 0.0, 0.00001);
        assertEquals(loc1.getLongitude(), 0.0, 0.00001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringToLocAcceptsOnlyTuples() {
        ConcreteCachingDataSender.stringToLocation("1.235,2.235,6.5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void stringToLocRejectsNonNumberInput() {
        ConcreteCachingDataSender.stringToLocation("IAmANumber,BelieveMe");
    }
}
