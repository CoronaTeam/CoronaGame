package ch.epfl.sdp.contaminationTest;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;


import ch.epfl.sdp.contamination.ConcretePositionAggregator;
import ch.epfl.sdp.contamination.FakeDataSender;

import static ch.epfl.sdp.contamination.ConcretePositionAggregator.roundLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ConcretePositionAggregatorTest {
    private ConcretePositionAggregator aggregator;

    @Before
    public void initTest(){
        this.aggregator = new ConcretePositionAggregator(new FakeDataSender());
    }
    @Test(expected = IllegalArgumentException.class)
    public void addPositionFailsOnNullInput(){
        aggregator.addPosition(null,null);
    }
    @Test(expected = IllegalArgumentException.class)
    public void canNotInstantiateAggregatorWithNullSender(){
        new ConcretePositionAggregator(null);
    }
    @Test
    public void roundCoordinatesRounds5Digits(){
        assertSame(5.94728,ConcretePositionAggregator.roundCoordinate(5.947280398257));
    }
    @Test
    public void roundLocationRoundsCorrectly(){
        Location location = new Location("provider");
        location.setLatitude(12.1234567);
        location.setLongitude(134.9876543);
        location = roundLocation(location);
        Location manuallyRoundedLocation = new Location("provider");
        manuallyRoundedLocation.setLongitude(134.98765);
        manuallyRoundedLocation.setLatitude(12.12346);
        assertEquals(manuallyRoundedLocation,location);

    }
    @Test(expected = IllegalArgumentException.class)
    public void roundLocationRejectsNullInput(){
        roundLocation(null);
    }
}
