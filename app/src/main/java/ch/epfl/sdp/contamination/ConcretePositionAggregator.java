package ch.epfl.sdp.contamination;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author lucas
 */
public final class ConcretePositionAggregator implements PositionAggregator {
    private int timelapBetweenNewLocationRegistration;
    private Date lastDate ;
    private Location newestLocation;
    private Date newestDate;
    private boolean isOnline;
    private CachingDataSender cachingDataSender;
    private Carrier carrier;
    private Timer updatePosTimer;
    HashMap<Long, List<Location>> buffer;

    public ConcretePositionAggregator(CachingDataSender cachingDataSender, Carrier carrier, int maxLocationsPerAggregation) {
        if (cachingDataSender == null || carrier == null) {
            throw new IllegalArgumentException("DataSender and Carrier should not be null");
        } else if (maxLocationsPerAggregation <= 0) {
            throw new IllegalArgumentException("There should be more than zero locations per aggregation!");
        }
        this.timelapBetweenNewLocationRegistration =  WINDOW_FOR_LOCATION_AGGREGATION / maxLocationsPerAggregation;
        this.buffer = new HashMap<>();
        this.cachingDataSender = cachingDataSender;
        this.carrier = carrier;
        this.lastDate = null;
        this.isOnline = false;
        startTimer();
    }

    public ConcretePositionAggregator(CachingDataSender cachingDataSender, Carrier carrier) {
       this(cachingDataSender, carrier, PositionAggregator.MAXIMAL_NUMBER_OF_LOCATIONS_PER_AGGREGATION);
    }

    /**
     * The timer will automatically re-enter the position if the user is not moving and online so that the average is more accurate
     */
    private void startTimer() {
        class UpdatePosTask extends TimerTask {
            public void run() {
                if (isOnline && newestLocation != null) {
                    registerPosition(newestLocation,newestDate);
                }
            }
        }
        if (updatePosTimer != null) {
            stopTimer();
        }
        updatePosTimer = new Timer();
        updatePosTimer.scheduleAtFixedRate(new UpdatePosTask(), 0, timelapBetweenNewLocationRegistration);
    }

    private void stopTimer() {
        updatePosTimer.cancel();
    }

    private void registerPosition(Location location, Date date) {
        if (location == null) {
            throw new IllegalArgumentException("Location should not be null");
        }
        Date roundedDate = PositionAggregator.getWindowForDate(date);
        List<Location> tmp = buffer.get(roundedDate.getTime());
        if (tmp != null) {
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
        if (location == null || date == null) {
            throw new IllegalArgumentException("Location or date should not be null !");
        }
        this.newestLocation = location;
        this.newestDate = date;
    }


    /**
     * Every WINDOW_FOR_LOCATION_AGGREGATION time, the PositionAggregator should send the mean value of the
     * saved positions to the DataSender. This method estimates whether the PositionAggregator should send that mean,
     * or if it just returns without doing anything.
     *
     */
    private void update() {
        if (lastDate != null) {
            List<Location> targetLocations = buffer.remove(lastDate.getTime());
            Location meanLocation = getMean(targetLocations);

            AsyncTask.execute(() -> {
                cachingDataSender.registerLocation(carrier, meanLocation, lastDate).join();

                // TODO: [LOG]
                Log.e("POSITION_AGGREGATOR", meanLocation.toString() + " with date : " + lastDate.getTime());
                Log.e("POSITION_AGGREGATOR", "Upload performed");
            });
        }
    }



    private Location getMean(List<Location> targetLocations) {
        if (targetLocations == null || targetLocations.size() == 0) {
            throw new IllegalArgumentException("Target location should not be empty");
        }
        double latitudeSum=0;
        double longitudeSum=0;
        int size = targetLocations.size();
        for (int i = 0 ; i < size; i +=1) {
            latitudeSum += targetLocations.get(i).getLatitude();
            longitudeSum += targetLocations.get(i).getLongitude();
        }
        latitudeSum = latitudeSum / ((double)(size));
        longitudeSum = longitudeSum /((double)(size));
        Location res = new Location(targetLocations.get(0)); // creates a new location with same properties as other locations
        res.setLatitude(latitudeSum);
        res.setLongitude(longitudeSum);
        return res;
    }

    public void updateToOffline() {
        this.isOnline = false;
    }
    public void updateToOnline() {
        this.isOnline = true;
    }
}
