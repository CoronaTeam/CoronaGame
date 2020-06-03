package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedMap;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataSender;
import ch.epfl.sdp.contamination.databaseIO.DataReceiver;
import ch.epfl.sdp.contamination.databaseIO.DataSender;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.databaseIO.PositionHistoryManager;
import ch.epfl.sdp.identity.fragment.AccountFragment;

import static ch.epfl.sdp.TestTools.newLoc;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.InfectionAnalyst.PRESYMPTOMATIC_CONTAGION_TIME;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PositionHistoryManagerTest {
    private DataSender sender;
    private DataReceiver receiver;
    @Before
    public void miniInit(){
        GridFirestoreInteractor grid = new GridFirestoreInteractor();
        sender = new ConcreteDataSender(grid);
        receiver = new ConcreteDataReceiver(grid);
        AccountFragment.IN_TEST = true;
        PositionHistoryManager.deleteLocalProbabilityHistory();
    }
    private Iterator<Location> addReg(Layman me,int checkNumber){
        sender.registerLocation(me, newLoc(2, 2), new Date(System.currentTimeMillis() - 1 - PRESYMPTOMATIC_CONTAGION_TIME));
        sender.registerLocation(me, newLoc(1, 1), new Date(System.currentTimeMillis()));
        SortedMap<Date, Location> res = receiver.getLastPositions();
        Collection<Location> val = res.values();
        Iterator<Location> it = val.iterator();
        assertSame(checkNumber,val.size());
        return it;
    }
    private void checkItValue(Iterator<Location> it){
        while (it.hasNext()) {
            assertEquals(1f,(float)it.next().getLatitude(),0.0000001f);
        }
    }
    @Test
    public void getLastPositionsReturnsCorrectWindowOfLocations() {

        Layman me = new Layman(Carrier.InfectionStatus.HEALTHY);
        Iterator<Location> it = addReg(me,1);
        checkItValue(it);

        it = addReg(me,2);
        checkItValue(it);
    }
    @Test
    public void positionHistoryDoesDeleteHistoryOnDemand(){
        getLastPositionsReturnsCorrectWindowOfLocations();
        PositionHistoryManager.deleteLocalProbabilityHistory();
        getLastPositionsReturnsCorrectWindowOfLocations();
    }
}
