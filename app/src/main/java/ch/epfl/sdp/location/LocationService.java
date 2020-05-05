package ch.epfl.sdp.location;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.CachingDataSender;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.ConcreteCachingDataSender;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.ConcretePositionAggregator;
import ch.epfl.sdp.contamination.DataReceiver;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.contamination.PositionAggregator;
import ch.epfl.sdp.firestore.FirestoreInteractor;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;

public class LocationService extends Service implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    public static final String ALARM_GOES_OFF = "beeep!";

    // TODO: Debug, make them private
    public static final String INFECTION_PROBABILITY_TAG = "infectionProbability";
    public static final String INFECTION_STATUS_TAG = "infectionStatus";
    public static final String LAST_UPDATED_TAG = "lastUpdated";

    private LocationBroker broker;
    private PositionAggregator aggregator;

    private GridFirestoreInteractor gridInteractor;

    private DataReceiver receiver;
    private CachingDataSender sender;

    private volatile boolean isStarted = false;
    private CompletableFuture<Void> startOperationQueue;

    private Carrier me;

    // TODO: This value should be set to several hours. It's now 2 minutes to allow for demo
    private long alarmDelayMillis = 120_000;

    private Date lastUpdated;
    private InfectionAnalyst analyst;

    private boolean hasGpsPermissions = false;

    private CompletableFuture<Boolean> loadCarrierStatus() {
        // TODO: Fix this: should not require activity to get Account ID
        CompletableFuture<Map<String, Object>> crr = gridInteractor.readDocument(FirestoreInteractor.documentReference("privateUser", "USER_ID_X42"));

        return crr.thenApply(map -> {
                float infectionProbability = (float) ((double) map.getOrDefault(
                        LocationService.INFECTION_PROBABILITY_TAG,
                        0.d));
                String infectionStatus = (String) map.getOrDefault(
                        LocationService.INFECTION_STATUS_TAG,
                        Carrier.InfectionStatus.HEALTHY.toString());

                me = new Layman(Carrier.InfectionStatus.valueOf(infectionStatus), infectionProbability);
                lastUpdated = new Date((long) map.getOrDefault(
                        LocationService.LAST_UPDATED_TAG,
                        System.currentTimeMillis()));

                return true;
            }).exceptionally(e -> {

                e.printStackTrace();
                me = new Layman(Carrier.InfectionStatus.HEALTHY);
                lastUpdated = new Date();

                return false;
            });
    }

    private void setModelUpdateAlarm() {
        Intent alarm = new Intent(this, LocationService.class);
        alarm.putExtra(ALARM_GOES_OFF, true);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, alarm, 0);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + alarmDelayMillis, pendingIntent);
    }

    @Override
    public void onCreate() {
        gridInteractor = new GridFirestoreInteractor();
        sender = new ConcreteCachingDataSender(gridInteractor);
        receiver = new ConcreteDataReceiver(gridInteractor);

        broker = new ConcreteLocationBroker((LocationManager) this.getSystemService(Context.LOCATION_SERVICE), this);

        refreshPermissions();

        startOperationQueue = loadCarrierStatus()
                .thenApply(v -> {
                    analyst = new ConcreteAnalysis(me, receiver, sender);
                    aggregator = new ConcretePositionAggregator(sender, me);

                    if (hasGpsPermissions) {
                        startAggregator();
                    }
                    return true; })
                .thenAccept(v -> {
                    if (v) {
                        isStarted = true;
                    } else {
                        // TODO: When adding support for offline mode, turn that into CompletableFuture<Boolean>
                        throw new IllegalStateException("Could not successfully start LocationService");
                    }
                });
    }

    /** Updates the infection probability by running the model
     * WARNING: Cannot be called after a period longer than MAX_CACHE_ENTRY_AGE
     * Since DataSender cache would have been partially emptied already
     */
    private void runInfectionModel() {
        SortedMap<Date, Location> locations = sender.getLastPositions().tailMap(lastUpdated);

        for (Map.Entry<Date, Location> l : locations.entrySet()) {
            analyst.updateInfectionPredictions(l.getValue(), lastUpdated, l.getKey());
            lastUpdated = l.getKey();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(ALARM_GOES_OFF)) {
            // It's time to run the model, starting from time 'lastUpdated';
            if (isStarted) {
                // Optimization: do not indefinitely grow the number of stages of startOperationQueue
                runInfectionModel();
            } else {
                startOperationQueue.thenAccept(startResult -> runInfectionModel());
            }
        }

        // Create next alarm
        setModelUpdateAlarm();
        return START_STICKY;
    }

    // TODO: When implementing offline mode, make all the functions return CompletableFuture
    private void waitUntilServiceStarted() {
        if (!startOperationQueue.isDone()) {
            startOperationQueue.join();
        }
    }

    public class LocationBinder extends android.os.Binder {
        public LocationService getService() {
            return LocationService.this;
        }
        public boolean hasGpsPermissions() {
            return hasGpsPermissions;
        }
        public void requestGpsPermissions(Activity activity) {
            broker.requestPermissions(activity, LOCATION_PERMISSION_REQUEST);
        }
        public boolean startAggregator() {
            return LocationService.this.startAggregator();
        }
        private void stopAggregator() {
            LocationService.this.stopAggregator();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocationBinder();
    }

    private void displayToast(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    private void refreshPermissions() {
        hasGpsPermissions = broker.hasPermissions(GPS);
    }

    @Override
    public void onLocationChanged(Location location) {

        waitUntilServiceStarted();

        refreshPermissions();
        if (hasGpsPermissions) {
            aggregator.addPosition(location);
        } else {
            displayToast("Missing permission");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated, do nothing
    }

    private boolean startAggregator() {
        if (broker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this)) {
            displayToast(getString(R.string.aggregator_status_on));
            aggregator.updateToOnline();
            return true;
        } else {
            return false;
        }
    }

    private void stopAggregator() {

        displayToast(getString(R.string.aggregator_status_off));
        aggregator.updateToOffline();
    }

    @Override
    public void onProviderEnabled(String provider) {

        waitUntilServiceStarted();

        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            refreshPermissions();
            if (hasGpsPermissions) {
                startAggregator();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

        waitUntilServiceStarted();

        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            stopAggregator();
        }
    }

    public LocationBroker getBroker() {

        waitUntilServiceStarted();

        return broker;
    }

    public InfectionAnalyst getAnalyst() {

        waitUntilServiceStarted();

        return analyst;
    }

    public CachingDataSender getSender() {

        waitUntilServiceStarted();

        return sender;
    }

    public DataReceiver getReceiver() {

        waitUntilServiceStarted();

        return receiver;
    }

    public void setAlarmDelay(int millisDelay) {

        waitUntilServiceStarted();

        alarmDelayMillis = millisDelay;
    }

    @VisibleForTesting
    public void setReceiver(DataReceiver receiver) {

        waitUntilServiceStarted();

        this.receiver = receiver;
    }

    @VisibleForTesting
    public void setSender(CachingDataSender sender) {

        waitUntilServiceStarted();

        this.sender = sender;
    }

    @VisibleForTesting
    public void setBroker(LocationBroker broker) {

        waitUntilServiceStarted();

        this.broker = broker;
    }

    @VisibleForTesting
    public void setAnalyst(InfectionAnalyst analyst) {

        waitUntilServiceStarted();

        this.analyst = analyst;
    }
}
