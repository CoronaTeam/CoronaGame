package ch.epfl.sdp.contamination;

import android.location.Location;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author lucas
 */
public final class ConcretePositionAggregator implements PositionAggregator {
    private Date lastDate = null;
    private DataSender dataSender;
    private InfectionAnalyst analyst;
    HashMap<Long, List<Location>> buffer;
    public ConcretePositionAggregator(DataSender dataSender, InfectionAnalyst analyst){
        if(dataSender == null || analyst == null){
            throw new IllegalArgumentException("DataSender or analyst should not be null");
        }
        this.buffer = new HashMap<>();
        this.dataSender = dataSender;
        this.analyst = analyst;
    }

    @Override
    public void addPosition(Location location, Date date) {
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
        double latitudeSummation=0;
        double longitudeSummation=0;
        int size = targetLocations.size();
        for(int i = 0 ; i < size; i +=1){
            latitudeSummation += targetLocations.get(i).getLatitude();
            longitudeSummation += targetLocations.get(i).getLongitude();
        }
        latitudeSummation = latitudeSummation==0?0:latitudeSummation / ((double)(size));
        longitudeSummation = longitudeSummation==0?0:longitudeSummation /((double)( size));
        Location res = targetLocations.get(0);
        res.setLatitude(latitudeSummation);
        res.setLongitude(longitudeSummation);
        return res;
    }
}
