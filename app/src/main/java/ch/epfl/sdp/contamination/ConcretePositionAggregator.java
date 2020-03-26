package ch.epfl.sdp.contamination;

import android.location.Location;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public final class ConcretePositionAggregator implements PositionAggregator {
    private Date lastDate = null;
    private DataSender dataSender;
    HashMap<Date, List<Location>> buffer;
    public ConcretePositionAggregator(DataSender dataSender){
        if(dataSender == null){
            throw new IllegalArgumentException("DataSender should not be null");
        }
        this.buffer = new HashMap<>();
        this.dataSender = dataSender;
    }

    @Override
    public void addPosition(Location location, Date date) {
        if(date == null || location == null){
            throw new IllegalArgumentException("Arguments should not be null");
        }
        Date roundedDate = getWindowForDate(date);
        List<Location> tmp = buffer.get(roundedDate);
        if(tmp != null){
            tmp.add(location);
            lastDate = roundedDate;
         //   buffer.replace(date,tmp);
        }else{
            update();
            lastDate = roundedDate;
            ArrayList<Location> newList = new ArrayList<Location>();
            newList.add(location);
            buffer.put(roundedDate,newList);

        }
    }

    private Date getWindowForDate(Date date) {
        long time = date.getTime();
        long roundedTime = time % WINDOW_FOR_LOCATION_AGGREGATION; // drop part not mutliple of WINDOW_FOR_LOCATION_AGGREGATION
        roundedTime = time - roundedTime;
        date.setTime(roundedTime);
        return date;
    }

    @Override
    public void update() {
        if(lastDate != null){
            List<Location> targetLocations = buffer.get(lastDate);
            Location meanLocation = getMean(targetLocations);
            dataSender.sendALocationToFirebase(meanLocation,lastDate);
        }
    }

    private Location getMean(List<Location> targetLocations) {
        if(targetLocations == null || targetLocations.size() == 0){
            throw new IllegalArgumentException("Target location shoul not be empty");
        }
        double latitudeSummation=0;
        double longitudeSummation=0;
        int size = targetLocations.size();
        for(int i = 0 ; i < size; i +=1){
            latitudeSummation += targetLocations.get(i).getLatitude();
            longitudeSummation += targetLocations.get(i).getLongitude();
        }
        latitudeSummation = latitudeSummation / ((double)(size));
        longitudeSummation = longitudeSummation /((double)( size));
        latitudeSummation = roundCoordinate(latitudeSummation);
        longitudeSummation = roundCoordinate(longitudeSummation);
        Location res = targetLocations.get(0);
        res.setLatitude(latitudeSummation);
        res.setLongitude(longitudeSummation);
        return res;

    }

    /**
     * Rounds a double to 5 digits after the comma
     * @param coor
     * @return
     */
    public static double roundCoordinate(double coor){
        return (double)Math.round(coor * 100000d) / 100000d;//fast rounding to 5 digits
    }

    /**
     * Rounds a location to 5 digits after the comma
     * @param l
     * @return
     */
    public static Location roundLocation(Location l){
        if(l == null){
            throw new IllegalArgumentException("Location can't be null");
        }
        double latitude = l.getLatitude();
        double longitude = l.getLongitude();
        latitude = roundCoordinate(latitude);
        longitude = roundCoordinate(longitude);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        return l;
    }
}
