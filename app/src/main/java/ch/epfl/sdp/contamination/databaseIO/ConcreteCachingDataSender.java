package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;

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

    SortedMap<Date, Location> lastPositions;
    private GridFirestoreInteractor gridInteractor;
    private StorageManager<Date,GeoPoint> position_history;

    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
        this.lastPositions = new TreeMap<>();
        this.position_history = initStorageManager();
    }
    private StorageManager<Date, GeoPoint> openStorageManager() {

        return new ConcreteManager<Date, GeoPoint>(
                CoronaGame.getContext(),
                "last_positions.csv",
                date_position -> {
                    try {
                        return CoronaGame.dateFormat.parse(date_position);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date_position'");
                    }
                }, geoPoint->{

                return stringToGeoPoint(geoPoint);
                }
        );
    }
    private GeoPoint stringToGeoPoint(String s){
        Pattern p = Pattern.compile("-?\\d+(,\\d+)*?\\.?\\d+?");
        List<Double> numbers = new ArrayList<Double>();
        Matcher m = p.matcher(s);

        while (m.find()) {
            numbers.add(Double.valueOf(m.group()));
        }
        GeoPoint res;
        try{
            res = new GeoPoint(numbers.get(0),numbers.get(1));
        }catch (NullPointerException e){
            throw new IllegalArgumentException("The cache is not storing geoPoint" + e.getStackTrace());
        }
        return res;
    }
    // Load previous probabilities history
    private StorageManager<Date, GeoPoint> initStorageManager() {

        StorageManager<Date, GeoPoint> cm = openStorageManager();

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
    private void refreshLastPositions(Date time, Location location) {

        Date oldestDate = new Date(time.getTime() - MAX_CACHE_ENTRY_AGE);
        lastPositions.headMap(oldestDate).clear();
        if (location != null) {
            lastPositions.put(time, location);
        }
    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {

        // Return a copy of the cache to avoid conflicts
        SortedMap<Date, Location> copyOfLastPositions = new TreeMap<>(lastPositions.tailMap(new Date(System.currentTimeMillis() - MAX_CACHE_ENTRY_AGE)));

        return copyOfLastPositions;
    }
}
