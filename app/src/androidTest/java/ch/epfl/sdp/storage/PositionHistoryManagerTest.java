package ch.epfl.sdp.storage;

import android.location.Location;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedMap;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataSender;
import ch.epfl.sdp.contamination.databaseIO.DataSender;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.identity.fragment.AccountFragment;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.contamination.InfectionAnalyst.PRESYMPTOMATIC_CONTAGION_TIME;
import static org.junit.Assert.assertTrue;

public class PositionHistoryManagerTest {
    private DataSender sender;
    @Before
    public void miniInit(){
        sender = new ConcreteDataSender(new GridFirestoreInteractor());
        AccountFragment.IN_TEST = true;
        PositionHistoryManager.delete();
    }
    @After
    public void release(){
        PositionHistoryManager.delete();
    }

    @Test
    public void getLastPositionsReturnsCorrectWindowOfLocations() {

        Layman me = new Layman(Carrier.InfectionStatus.HEALTHY);
        sender.registerLocation(me, newLoc(2, 2), new Date(System.currentTimeMillis() - 1 - PRESYMPTOMATIC_CONTAGION_TIME));
        sender.registerLocation(me, newLoc(1, 1), new Date(System.currentTimeMillis()));
        SortedMap<Date, Location> res = PositionHistoryManager.getLastPositions();
        Collection<Location> val = res.values();
        Iterator<Location> it = val.iterator();
        assertTrue(val.size() == 1);
        while (it.hasNext()) {
            assertTrue(it.next().getLatitude() == 1);
        }
    }
    @Test
    public void getLastPositionsWorksOnSecondUsage(){
        getLastPositionsReturnsCorrectWindowOfLocations();
    }
}
