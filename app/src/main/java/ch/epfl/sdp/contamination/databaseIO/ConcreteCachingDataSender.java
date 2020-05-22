package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.INFECTION_STATUS_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMESTAMP_TAG;
import static ch.epfl.sdp.identity.AuthenticationManager.getActivity;

/**
 * Implementation of a DataSender with a cache
 */
public class ConcreteCachingDataSender implements CachingDataSender {

    private GridFirestoreInteractor gridInteractor;
    private StorageManager<Date,Location> positionHistory;

    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
        this.positionHistory = initStorageManager();
    }
    private StorageManager<Date, Location> openStorageManager() {

        return new ConcreteManager<Date, Location>(
                CoronaGame.getContext(),
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
    private Location stringToLocation(String s){
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
    // Load previous probabilities history
    private StorageManager<Date, Location> initStorageManager() {

        StorageManager<Date, Location> cm = openStorageManager();

        if (!cm.isReadable()) {
            cm.delete();
            cm = openStorageManager();
        }

        return cm;
    }
    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
        location = CachingDataSender.roundLocation(location);
        CompletableFuture<Void> lastPositionsFuture, gridWriteFuture;
        refreshLastPositions(time, location);

        Map<String, Object> element = new HashMap<>();
        element.put(GEOPOINT_TAG, new GeoPoint(
                location.getLatitude(),
                location.getLongitude()
        ));
        element.put(TIMESTAMP_TAG, time.getTime());
        element.put(INFECTION_STATUS_TAG, carrier.getInfectionStatus());

        lastPositionsFuture = gridInteractor.writeDocumentWithID(
                documentReference(LAST_POSITIONS_COLL, AccountFragment.getAccount(getActivity()).getId()), element);

        gridWriteFuture = gridInteractor.gridWrite(location, String.valueOf(time.getTime()), carrier);

        return CompletableFuture.allOf(lastPositionsFuture, gridWriteFuture);
    }

    // Removes every locations older than PRE-SYMPTOMATIC_CONTAGION_TIME ms and adds a new position
    private void refreshLastPositions(Date time, Location geoPoint) {
        Date oldestDate = new Date(time.getTime() - MAX_CACHE_ENTRY_AGE);
        SortedMap<Date,Location> hist = new TreeMap();

        hist.put(time,geoPoint);
        positionHistory.write(hist);
        try {
            positionHistory.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {
        // Return a copy of the cache to avoid conflicts
        Date lastDate = new Date(System.currentTimeMillis()-MAX_CACHE_ENTRY_AGE);
        SortedMap<Date,Location> lastPos = positionHistory.filter((date, geoP) -> ((Date)(date)).after(lastDate));
        return lastPos;
    }
}
