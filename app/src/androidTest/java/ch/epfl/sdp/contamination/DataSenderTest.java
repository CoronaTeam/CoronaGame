package ch.epfl.sdp.contamination;

import android.location.Location;

import org.junit.Test;

import ch.epfl.sdp.TestTools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DataSenderTest {
    @Test
    public void RoundAndExpandLocationIsCorrect(){
        Location location = new Location("provider");
        location.setLatitude(12.1234567);
        location.setLongitude(134.9876543);
        DataSender.RoundAndExpandLocation(location);
        Location manuallyRoundedLocation = new Location("provider");
        manuallyRoundedLocation.setLongitude(13498765);
        manuallyRoundedLocation.setLatitude(1212346);
        assertTrue(TestTools.expandedLocEquals(manuallyRoundedLocation,location));
//        assertEquals(manuallyRoundedLocation.getLongitude(),location.getLongitude(),0);
//        assertEquals(manuallyRoundedLocation.getLatitude(),location.getLatitude(),0);
    }
}
