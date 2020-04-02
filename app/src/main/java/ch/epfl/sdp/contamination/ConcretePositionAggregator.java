package ch.epfl.sdp.contamination;

import android.location.Location;
import android.util.Pair;

import com.google.android.gms.common.data.DataBufferObserver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author lucas
 */
public final class ConcretePositionAggregator extends Observable implements PositionAggregator {
    private Date lastDate ;
    private Pair<Location,Date> newestPosition ;
    private boolean isOnline;
    private DataSender dataSender;
    private InfectionAnalyst analyst;
    private Timer updatePosTimer;
    HashMap<Long, List<Location>> buffer;
    public ConcretePositionAggregator(DataSender dataSender, InfectionAnalyst analyst){
        if(dataSender == null || analyst == null){
            throw new IllegalArgumentException("DataSender or analyst should not be null");
        }
        this.buffer = new HashMap<>();
        this.dataSender = dataSender;
        this.analyst = analyst;
        this.lastDate = null;
        this.newestPosition = null;
        this.isOnline = false;
        startTimer();
    }

    /**
     * The timer will automatically re-enter the position if the user is not moving and online so that the average is more accurate
     */
    private void startTimer(){
        class UpdatePosTask extends TimerTask {
            public void run() {
                if(isOnline){
                    registerPosition(newestPosition.first,newestPosition.second);
                }
            }
        }
        if(updatePosTimer != null){
            stopTimer();
        }
        updatePosTimer = new Timer();
        updatePosTimer.scheduleAtFixedRate(new UpdatePosTask(), 0, TIMELAP_BETWEEN_NEW_LOCATION_REGISTRATION);
    }

    private void stopTimer(){
        updatePosTimer.cancel();
    }

    private void registerPosition(Location location, Date date){
        if(location == null){
            throw new IllegalArgumentException("Location should not be null");
        }
        Date roundedDate = PositionAggregator.getWindowForDate(date);
        List<Location> tmp = buffer.get(roundedDate.getTime());
        if(tmp != null){
            tmp.add(location);
            lastDate = roundedDate;
        }else{
            update();
            lastDate = roundedDate;
            ArrayList<Location> newList = new ArrayList<Location>();
            newList.add(location);
            buffer.put(roundedDate.getTime(),newList);
        }
    }
    @Override
    public void addPosition(Location location, Date date) {
        this.newestPosition = new Pair<>(location,date);
    }

    /**
     * Every WINDOW_FOR_LOCATION_AGGREGATION time, the PositionAggregator should send the mean value of the
     * saved positions to the DataSender. This method estimates whether the PositionAggregator should send that mean,
     * or if it just returns without doing anything.
     *
     */
    private void update() {
        if(lastDate != null){
            List<Location> targetLocations = buffer.remove(lastDate.getTime()); //remove useless data. MAY BE CHANGED TO ADD A CACHE
            Location meanLocation = getMean(targetLocations);
            Location expandedLocation = DataSender.RoundAndExpandLocation(meanLocation);
            //System.out.println("--------------------------------------------"+expandedLocation.toString() + " with date : "+lastDate.toString());
            dataSender.registerLocation(analyst.getCurrentCarrier(),expandedLocation,lastDate);
        }
    }
    private Location getMean(List<Location> targetLocations) {
        if(targetLocations == null || targetLocations.size() == 0){
            throw new IllegalArgumentException("Target location should not be empty");
        }
        double latitudeSum=0;
        double longitudeSum=0;
        int size = targetLocations.size();
        for(int i = 0 ; i < size; i +=1){
            latitudeSum += targetLocations.get(i).getLatitude();
            longitudeSum += targetLocations.get(i).getLongitude();
        }
        latitudeSum = latitudeSum / ((double)(size));
        longitudeSum = longitudeSum /((double)( size));
        Location res = new Location(targetLocations.get(0)); // creates a new location with same properties as other locations
        res.setLatitude(latitudeSum);
        res.setLongitude(longitudeSum);
        return res;
    }

    public void updateToOffline(){
        this.isOnline = false;
    }
    public void updateToOnline(){
        this.isOnline = true;
    }
}
