package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.storage.ConcreteManager;

import static ch.epfl.sdp.contamination.databaseIO.DataSender.MAX_CACHE_ENTRY_AGE;

/**
 * This is a kind of a singleton class to avoid problems. It manages the storing exchanges of the last positions.
 * Will only store the lattitude and longitude in the location
 * This class should not be made static for testing purpose.
 */
public final class PositionHistoryManager  {
    private static ConcreteManager<Date, Location> instance = null;
    private static ReentrantLock lock = new ReentrantLock();
    private static void setHistoryManager(){
        lock.lock();
        try{
            if(instance==null){
                instance = getNewManager();
                if(!instance.isReadable()){
                    instance.delete();
                    instance=getNewManager();
                }
            }

//            if(instance!=null && !instance.isReadable()){
//                //TODO:LOG
//                Log.e("carrierTest in PositionManager", "before delete ln 38");
//
//                    instance.delete();
//                    instance = null;
//
//            }
//            else if(instance == null){
//                //TODO:LOG
//                Log.e("carrierTest in PositionManager", "before getNew");
//                instance =  getNewManager();
//                instance.read();
//            }else{
//                //TODO:LOG
//                Log.e("carrierTest in PositionManager", "useless setHistory");
//
//            }
        }finally {
            lock.unlock();
        }
    }

    private static ConcreteManager<Date, Location> getNewManager() {

        return new ConcreteManager<Date,Location>(CoronaGame.getContext(),
                String.valueOf(!AccountFragment.IN_TEST)+"_last_positions.csv",
                date_position -> {
                    try {
                        return CoronaGame.dateFormat.parse(date_position);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date_position'");
                    }
                }, location->{

                    return stringToLocation(location);
                }, ";"
        );
    }
    private static Location stringToLocation(String s){
        String[] splitted = s.split(",");
        if(splitted.length!=2){
            throw new IllegalArgumentException("The location string is wrong");
        }
        float n1 = getNumberFromString(splitted[0]);
        float n2 = getNumberFromString(splitted[1]);
        Location res = new Location("provider");
        res.reset();
        res.setLatitude(n1);
        res.setLongitude(n2);

        return res;
    }

    private static float getNumberFromString(String s) {
        s = s.replaceAll("[^\\d.]", "");
        return Float.parseFloat(s);
    }

    /**
     * Refresh last positions. If date is null, it is set to now.
     * @param time
     * @param location
     */
    protected static void refreshLastPositions(Date time, Location location) {
        if(location==null){
            return; //refresh with no location is useless
        }

        Location toStore = new Location("myFavoriteFakeProvider");
        toStore.reset();
        toStore.setLongitude(location.getLongitude());
        toStore.setLatitude(location.getLatitude());
        if(time==null){
            time = new Date();
        }

        setHistoryManager();
        SortedMap<Date,Location> hist = new TreeMap();

        hist.put(time,toStore);
        lock.lock();


        try {
            instance.write(hist);
            instance.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
    /**
     *
     * @return: positions send to firebase during the last UNINTENTIONAL_CONTAGION_TIME time.
     */
    protected static SortedMap<Date, Location> getLastPositions() {

        setHistoryManager();
        Date lastDate = new Date(System.currentTimeMillis()-MAX_CACHE_ENTRY_AGE);
        lock.lock();
        SortedMap<Date,Location> lastPos;
        try{
            lastPos = instance.filter((date, loc) -> ((Date)(date)).after(lastDate));
        }finally {
            lock.unlock();

        }
        return lastPos;
    }

    @VisibleForTesting
    public static void deleteLocalProbabilityHistory() {
        lock.lock();

        try {
            setHistoryManager();
            // Delete current manager
            if(instance!=null){
                instance.delete();
                instance = getNewManager();
                instance.read();
            }
        } finally {
            lock.unlock();
        }

//        //TODO:LOG
//        Log.e("carrierTest in PositionManager", "before setHistoryManager");
//        lock.lock();
//        setHistoryManager();
//        // Delete the file
//        //TODO:LOG
//        Log.e("carrierTest in PositionManager", "before delete");
//        if(instance!=null){
//            instance.delete();
//        }
//        lock.unlock();
//        //TODO:LOG
//        Log.e("carrierTest in PositionManager", "after delete");
    }

}
