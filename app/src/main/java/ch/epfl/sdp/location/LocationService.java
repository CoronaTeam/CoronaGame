package ch.epfl.sdp.location;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import ch.epfl.sdp.identity.AuthenticationManager;
import ch.epfl.sdp.CoronaGame;
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

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus;
import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;

public class LocationService extends Service implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    public static final String ALARM_GOES_OFF = "beeep!";

    public static final String INFECTION_PROBABILITY_TAG = "infectionProbability";
    public static final String INFECTION_STATUS_TAG = "infectionStatus";
    public static final String LAST_UPDATED_TAG = "lastUpdated";

    // TODO: This value should be set to several hours. It's now 2 minutes to allow for demo
    private static long alarmDelayMillis = 120_000;

    private LocationBroker broker;
    private PositionAggregator aggregator;

    private GridFirestoreInteractor gridInteractor;

    private SharedPreferences sharedPref;

    private DataReceiver receiver;
    private CachingDataSender sender;

    private Carrier me;

    private boolean isAlarmSet = false;

    private Date lastUpdated;
    private InfectionAnalyst analyst;

    private boolean hasGpsPermissions = false;

    public static void setAlarmDelay(int millisDelay) {
        alarmDelayMillis = millisDelay;
    }

    private void setModelUpdateAlarm() {
        Intent alarm = new Intent(this, LocationService.class);
        alarm.putExtra(ALARM_GOES_OFF, true);

        PendingIntent pendingIntent = PendingIntent.getService(this, 0, alarm, 0);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.set(
                AlarmManager.RTC,
                System.currentTimeMillis() + alarmDelayMillis,
                pendingIntent);

        isAlarmSet = true;
    }

    private void loadCarrierStatus() {
        float infectionProbability = sharedPref.getFloat(INFECTION_PROBABILITY_TAG, 0);
        InfectionStatus infectionStatus = InfectionStatus.values()[sharedPref.getInt(INFECTION_STATUS_TAG, InfectionStatus.HEALTHY.ordinal())];

        me = new Layman(infectionStatus, infectionProbability, AuthenticationManager.getUserId());

        lastUpdated = new Date(sharedPref.getLong(LAST_UPDATED_TAG, System.currentTimeMillis()));
    }

    private void storeCarrierStatus() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(INFECTION_PROBABILITY_TAG, me.getIllnessProbability())
                .putInt(INFECTION_STATUS_TAG, me.getInfectionStatus().ordinal())
                .putLong(LAST_UPDATED_TAG, lastUpdated.getTime())
                .commit();
    }

    @Override
    public void onCreate() {
        gridInteractor = new GridFirestoreInteractor();
        sender = new ConcreteCachingDataSender(gridInteractor);
        receiver = new ConcreteDataReceiver(gridInteractor);

        broker = new ConcreteLocationBroker((LocationManager) this.getSystemService(Context.LOCATION_SERVICE), this);

        refreshPermissions();

        sharedPref = this.getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        loadCarrierStatus();

        analyst = new ConcreteAnalysis(me, receiver, sender);
        aggregator = new ConcretePositionAggregator(sender, me);

        if (hasGpsPermissions) {
            startAggregator();
        }
    }

    /** Updates the infection probability by running the model
     * WARNING: Cannot be called after a period longer than MAX_CACHE_ENTRY_AGE
     * Since DataSender cache would have been partially emptied already
     */
    private void updateInfectionModel() {
        SortedMap<Date, Location> locations = sender.getLastPositions().tailMap(lastUpdated);

        for (Map.Entry<Date, Location> l : locations.entrySet()) {
            analyst.updateInfectionPredictions(l.getValue(), lastUpdated, l.getKey());
            lastUpdated = l.getKey();
        }

        storeCarrierStatus();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(ALARM_GOES_OFF)) {
            // It's time to run the model, starting from time 'lastUpdated';
            updateInfectionModel();
        }

        if (!isAlarmSet) {
            // Create next alarm
            setModelUpdateAlarm();
        }
        return START_STICKY;
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
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            refreshPermissions();
            if (hasGpsPermissions) {
                startAggregator();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            stopAggregator();
        }
    }

    public LocationBroker getBroker() {
        return broker;
    }

    public InfectionAnalyst getAnalyst() {
        return analyst;
    }



    public CachingDataSender getSender() {
        return sender;
    }

    public DataReceiver getReceiver() {
        return receiver;
    }

    @VisibleForTesting
    public void resetAnalyst(){
        analyst = new ConcreteAnalysis(me, receiver, sender);
    }

    @VisibleForTesting
    public void setCarrier(Carrier carrier){
        me = carrier;
    }

    @VisibleForTesting
    public void resetSender(){
        sender = new ConcreteCachingDataSender(gridInteractor);
    }

    @VisibleForTesting
    public void setReceiver(DataReceiver receiver) {
        this.receiver = receiver;
    }

    @VisibleForTesting
    public void setSender(CachingDataSender sender) {
        this.sender = sender;
    }

    @VisibleForTesting
    public void setBroker(LocationBroker broker) {
        this.broker = broker;
    }

    @VisibleForTesting
    public void setAnalyst(InfectionAnalyst analyst) {
        this.analyst = analyst;
    }
}
