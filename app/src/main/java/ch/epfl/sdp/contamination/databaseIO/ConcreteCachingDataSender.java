package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;
import android.util.Log;

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
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.HISTORY_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.HISTORY_POSITIONS_DOC;
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
    private ReentrantLock lock;
    public ConcreteCachingDataSender(GridFirestoreInteractor interactor) {
        this.lock = new ReentrantLock();
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
                }, ConcreteCachingDataSender::stringToLocation
        );
    }
    static Location stringToLocation(String s){
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
        CompletableFuture<Void> historyFuture, lastPositionsFuture, gridWriteFuture;

        refreshLastPositions(time, location);

        Map<String, Object> element = new HashMap<>();
        element.put(GEOPOINT_TAG, new GeoPoint(
                location.getLatitude(),
                location.getLongitude()
        ));
        element.put(TIMESTAMP_TAG, time.getTime());
        element.put(INFECTION_STATUS_TAG, carrier.getInfectionStatus());

        historyFuture = gridInteractor.writeDocumentWithID(documentReference(
                HISTORY_COLL + "/" + carrier.getUniqueId() + "/" + HISTORY_POSITIONS_DOC, "TS" + time.getTime()), element);

        lastPositionsFuture = gridInteractor.writeDocumentWithID(
                documentReference(LAST_POSITIONS_COLL, AccountFragment.getAccount(getActivity()).getId()), element);

        gridWriteFuture = gridInteractor.gridWrite(location, String.valueOf(time.getTime()), carrier);

        return CompletableFuture.allOf(historyFuture, lastPositionsFuture, gridWriteFuture);
    }

    // Removes every locations older than PRE-SYMPTOMATIC_CONTAGION_TIME ms and adds a new position
    private void refreshLastPositions(Date time, Location geoPoint) {
        SortedMap<Date,Location> hist = new TreeMap();
        hist.put(time,geoPoint);
        lock.lock();
        try {
        positionHistory.write(hist);

            positionHistory.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }

    }

    @Override
    public SortedMap<Date, Location> getLastPositions() {
        // Return a copy of the cache to avoid conflicts
        Date lastDate = new Date(System.currentTimeMillis()-MAX_CACHE_ENTRY_AGE);
        SortedMap<Date,Location> lastPos;
        lock.lock();
        try{
            lastPos = positionHistory.filter((date, geoP) -> ((Date)(date)).after(lastDate));
        }finally {
            lock.unlock();
        }
        return lastPos;
    }
}
