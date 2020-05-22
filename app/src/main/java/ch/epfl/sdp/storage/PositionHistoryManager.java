package ch.epfl.sdp.storage;

import android.location.Location;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.epfl.sdp.CoronaGame;

import static ch.epfl.sdp.contamination.databaseIO.DataSender.MAX_CACHE_ENTRY_AGE;

/**
 * This is a singleton class to avoid problems. It manages the storing exchanges of the last positions.
 */
public class PositionHistoryManager extends ConcreteManager<Date, Location> {
    private static PositionHistoryManager instance = null;
    private static void setHistoryManager(){
        if(instance!=null && !instance.isReadable()){
            instance.delete();
            instance = null;
        }
        if(instance == null){
            instance = new PositionHistoryManager();
        }
        return;
    }

    private PositionHistoryManager() {
        super(CoronaGame.getContext(),
                "last_positions.csv",
                date_position -> {
                    try {
                        return CoronaGame.dateFormat.parse(date_position);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date_position'");
                    }
                }, location->{

                    return stringToLocation(location);
                }
        );
    }
    private static Location stringToLocation(String s){
        Pattern p = Pattern.compile("-?\\d+(,\\d+)*?\\.?\\d+?");
        List<Double> numbers = new ArrayList<Double>();
        Matcher m = p.matcher(s);

        while (m.find()) {
            numbers.add(Double.valueOf(m.group()));
        }
        Location res = new Location("provider");
        res.reset();
        try{
            res.setLatitude(numbers.get(0));
            res.setLongitude(numbers.get(1));
        }catch (NullPointerException e){
            throw new IllegalArgumentException("The cache is not storing Location" + e.getStackTrace());
        }
        return res;
    }
    public static void refreshLastPositions(Date time, Location geoPoint) {
        setHistoryManager();
        Date oldestDate = new Date(time.getTime() - MAX_CACHE_ENTRY_AGE);
        SortedMap<Date,Location> hist = new TreeMap();

        hist.put(time,geoPoint);
        instance.write(hist);
        try {
            instance.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     *
     * @return: positions send to firebase during the last UNINTENTIONAL_CONTAGION_TIME time.
     */
    public static SortedMap<Date, Location> getLastPositions() {
        setHistoryManager();
        Date lastDate = new Date(System.currentTimeMillis()-MAX_CACHE_ENTRY_AGE);
        SortedMap<Date,Location> lastPos = instance.filter((date, geoP) -> ((Date)(date)).after(lastDate));
        return lastPos;
    }
}
