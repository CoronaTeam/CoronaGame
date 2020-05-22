package ch.epfl.sdp.contamination;

import android.location.Location;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static ch.epfl.sdp.TestTools.equalLatLong;
import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConcretePositionAggregatorTest {
    private final int maxNumberOfLoc = 1000;
    private ConcretePositionAggregator aggregator;
    private FakeDataSender sender;
    private int timelap;

    @Before
    public void initTest() {
        this.sender = new FakeDataSender();
        this.aggregator = new ConcretePositionAggregator(sender, new Layman(HEALTHY), maxNumberOfLoc);
        aggregator.updateToOnline();
        timelap = PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION / maxNumberOfLoc;
    }

    @Test(expected = IllegalArgumentException.class)
    public void cantInstantiateOnZeroMaxLocationPerAggregation() {
        new ConcretePositionAggregator(sender, new Layman(HEALTHY), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cantInstantiateOnNegativeMaxLocationPerAggregation() {
        new ConcretePositionAggregator(sender, new Layman(HEALTHY), -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPositionFailsOnNullInput() {
        aggregator.addPosition(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void canNotInstantiateAggregatorWithNullSender() {
        new ConcretePositionAggregator(null, new Layman(HEALTHY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void canNotInstantiateAggregatorWithNullAnalyst() {
        new ConcretePositionAggregator(new FakeDataSender(), null);
    }

    private void addAndWait(Location l, Date d) {
        aggregator.addPosition(l, d);
        sleep(timelap);
    }

    @Test
    public void noPositionAreUploadedWhileOffline() {
        this.aggregator.updateToOffline();
        addAndWait(newLoc(0, 0), Calendar.getInstance().getTime());
        addAndWait(newLoc(1, 1), new Date(Calendar.getInstance().getTimeInMillis() + PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION));
        Map<Date, Location> res = sender.getMap();
        assertNull(res);
    }

    private void initTestMean(Date now, Date now1, Date now2) {
        Location l1 = newLoc(0, 0);
        Location l2 = newLoc(10, 10);

        Location a1 = newLoc(2, 1);
        Location a2 = newLoc(10, 1);
        Location a3 = newLoc(9, 9);
        Location a4 = newLoc(2, 7);

        Location b1 = newLoc(6, 4);
        Location b2 = newLoc(7, 4);
        Location b3 = newLoc(8, 4);
        sleep();
        addAndWait(l1, now);
        addAndWait(l2, now);
        addAndWait(a1, now1);
        addAndWait(a2, now1);
        addAndWait(a3, now1);
        addAndWait(a4, now1);
        addAndWait(b1, now2);
        addAndWait(b2, now2);
        addAndWait(b3, now2);
        addAndWait(l1, new Date(now2.getTime() + PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION));
    }

    @Test
    public void updatesTheCorrectMeanLocation() {
        Date now = new Date(0);
        Date now1 = new Date(PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION);
        Date now2 = new Date(2 * PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION);
        initTestMean(now, now1, now2);

        //TEST 1
        Map<Date, Location> firebaseLoc = sender.getMap();
        assertNotNull(firebaseLoc);
        Location res = newLoc(5, 5);
        assertTrue(firebaseLoc.containsKey(now));
        assertTrue(equalLatLong(firebaseLoc.get(now), res));

        // TEST 2
        Location res2 = newLoc(5.75, 4.5);
        assertTrue(firebaseLoc.containsKey(now1));
        assertTrue(equalLatLong(firebaseLoc.get(now1), res2));
        // TEST 3
        Location res3 = newLoc(7, 4);
        assertTrue(firebaseLoc.containsKey(now1));
        assertTrue(equalLatLong(firebaseLoc.get(now2), res3));
    }
}
