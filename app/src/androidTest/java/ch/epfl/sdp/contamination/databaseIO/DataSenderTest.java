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

public class DataSenderTest {
    DataReceiver receiver;
    DataSender sender;

    @Before
    public void init() {
        receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        sender = new ConcreteDataSender(new GridFirestoreInteractor());
        AccountFragment.IN_TEST = true;
    }

    @Test
    public void RoundAndExpandLocationIsCorrect() {
        Location location = new Location("provider");
        location.setLatitude(12.1234567);
        location.setLongitude(134.9876543);

        DataSender.roundLocation(location);

        Location manuallyRoundedLocation = new Location("provider");
        manuallyRoundedLocation.setLongitude(134.98765);
        manuallyRoundedLocation.setLatitude(12.12346);
        assertEquals(manuallyRoundedLocation.getLongitude(), location.getLongitude(), 0.00000001f);
        assertEquals(manuallyRoundedLocation.getLatitude(), location.getLatitude(), 0.00000001f);
    }

    @Test
    public void resetAlertsDeletesAlertAttribute() {
        DataSender.sendAlert(User.DEFAULT_USERID);
        DataSender.resetSickAlerts(User.DEFAULT_USERID);
//        DataReceiver receiver = new ConcreteDataReceiver(new GridFirestoreInteractor());<
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                assertTrue(res.isEmpty()));
        sleep();
    }

    @Test
    public void sendAlertIncrementsOrCreatesAlertAttribute() {
        DataSender.resetSickAlerts(User.DEFAULT_USERID);
        DataSender.sendAlert(User.DEFAULT_USERID);
        sleep();
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                assertFalse(res.isEmpty()));
        sleep();
        DataSender.sendAlert(User.DEFAULT_USERID);
        receiver.getNumberOfSickNeighbors(User.DEFAULT_USERID).thenAccept(res ->
                assertEquals(2, ((float) ((double) (res.get(publicAlertAttribute)))), 0.0001));
        sleep();
    }
}
