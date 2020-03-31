package ch.epfl.sdp.contamination;

import android.location.Location;
import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import java.util.Map;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.expandedLocEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConcretePositionAggregatorTest {
    private ConcretePositionAggregator aggregator;
    private FakeDataSender sender;

    @Before
    public void initTest(){
        this.sender  = new FakeDataSender();
        this.aggregator = new ConcretePositionAggregator(sender,new FakeAnalyst());
    }
    @Test(expected = IllegalArgumentException.class)
    public void addPositionFailsOnNullInput(){
        aggregator.addPosition(null,null);
    }
    @Test(expected = IllegalArgumentException.class)
    public void canNotInstantiateAggregatorWithNullSender(){
        new ConcretePositionAggregator(null,new FakeAnalyst());
    }
    @Test(expected = IllegalArgumentException.class)
    public void canNotInstantiateAggregatorWithNullAnalyst(){
        new ConcretePositionAggregator(new FakeDataSender(),null);
    }
    @Test
    public void updatesTheCorrectMeanLocation(){
        Date now = new Date(0);
        Date now1 = new Date(PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION);
        Date now2 = new Date(2* PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION);
        // TEST 1
        Location l1 = newLoc(0,0);
        Location l2 = newLoc(10,10);

        Location a1 = newLoc(2,1);
        Location a2 = newLoc(10,1);
        Location a3 = newLoc(9,9);
        Location a4 = newLoc(2,7);

        Location b1 = newLoc(6,4);
        Location b2 = newLoc(7,4);
        Location b3 = newLoc(8,4);

        aggregator.addPosition(l1,now);
        aggregator.addPosition(l2,now);

        assertNull(sender.getMap());

        aggregator.addPosition(a1, now1);
        Map<Date,Location> firebaseLoc = sender.getMap();
        assertNotNull(firebaseLoc);
        Location res = newLoc(5*DataSender.EXPAND_FACTOR,5*DataSender.EXPAND_FACTOR);
        assertTrue(firebaseLoc.containsKey(now));
        assertTrue(expandedLocEquals(firebaseLoc.get(now),res));

        // TEST 2

        aggregator.addPosition(a2, now1);
        aggregator.addPosition(a3, now1);
        aggregator.addPosition(a4, now1);
        aggregator.addPosition(b1, now2);

        Map<Date,Location> firebaseLoc2 = sender.getMap();
        assertNotNull(firebaseLoc2);
        Location res2 = newLoc(5.75*DataSender.EXPAND_FACTOR,4.5*DataSender.EXPAND_FACTOR);
        assertTrue(firebaseLoc2.containsKey(now1));
        assertTrue(expandedLocEquals(firebaseLoc2.get(now1),res2));

        // TEST 3
        aggregator.addPosition(b2,now2);
        aggregator.addPosition(b3,now2);
        aggregator.addPosition(l1,new Date(now2.getTime() + PositionAggregator.WINDOW_FOR_LOCATION_AGGREGATION));

        Map<Date,Location> firebaseLoc3 = sender.getMap();
        assertNotNull(firebaseLoc3);
        Location res3 = newLoc(7*DataSender.EXPAND_FACTOR,4*DataSender.EXPAND_FACTOR);
        assertTrue(firebaseLoc3.containsKey(now1));
        assertTrue(expandedLocEquals(firebaseLoc3.get(now2),res3));
    }
}
