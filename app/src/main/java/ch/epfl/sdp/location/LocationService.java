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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.R;
import ch.epfl.sdp.connectivity.ConcreteConnectivityBroker;
import ch.epfl.sdp.connectivity.ConnectivityBroker;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.ConcretePositionAggregator;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.contamination.ObservableCarrier;
import ch.epfl.sdp.contamination.PositionAggregator;
import ch.epfl.sdp.contamination.databaseIO.CachingDataSender;
import ch.epfl.sdp.contamination.databaseIO.ConcreteCachingDataSender;
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.databaseIO.DataReceiver;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.identity.AuthenticationManager;

import static ch.epfl.sdp.CoronaGame.getDemoSpeedup;
import static ch.epfl.sdp.connectivity.ConnectivityBroker.Provider.GPS;
import static ch.epfl.sdp.connectivity.ConnectivityBroker.Provider.INTERNET;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreInteractor.taskToFuture;

public class LocationService extends Service implements LocationListener, Observer {

    private final static int LOCATION_PERMISSION_REQUEST = 20201;

    public static final String ALARM_GOES_OFF = "beeep!";
    private static final String POISON_PILL = "dead!";

    public static final String INFECTION_PROBABILITY_PREF = "infectionProbability";
    public static final String INFECTION_STATUS_PREF = "infectionStatus";
    public static final String LAST_UPDATED_PREF = "lastUpdated";
    //TODO: should we accelerate also the interval between requests to location update?
    private static final int MIN_UP_INTERVAL_MILLIS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;
    // This correspond to 6h divided by the DEMO_SPEEDUP constant
    private static long alarmDelayMillis = 21_600_000 / getDemoSpeedup();

    private int serviceNotificationId = -1;

    private int boundActivities = 0;

    private ConnectivityBroker broker;
    private PositionAggregator aggregator;

    private SharedPreferences sharedPref;

    private DataReceiver receiver;
    private CachingDataSender sender;

    private boolean isAlarmSet = false;

    private PendingIntent alarmPending;
    private AlarmManager alarmManager;

    private Date lastUpdated;
    private InfectionAnalyst analyst;

    private void showOfflineNotification() {
        removeNotifications();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CoronaGame.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_offline)
                .setContentTitle("Virus Tracker")
                .setContentText(getString(R.string.internet_offline_msg))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.internet_offline_msg)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        serviceNotificationId = (int) (Math.random() * 100);

        NotificationManagerCompat.from(this).notify(serviceNotificationId, builder.build());
    }

    private final Observer internetObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            Log.e("LOCATION_SERVICE", "Showing service.......? " + ((boolean) arg));
            if ((boolean) arg) {
                removeNotifications();
                showServiceNotification();
            } else {
                showOfflineNotification();
            }
        }
    };

    public static void setAlarmDelay(int millisDelay) {
        alarmDelayMillis = millisDelay;
    }

    private void setModelUpdateAlarm() {
        Intent alarm = new Intent(this, LocationService.class);
        alarm.putExtra(ALARM_GOES_OFF, true);

        alarmPending = PendingIntent.getService(this, 0, alarm, 0);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(
                AlarmManager.RTC,
                System.currentTimeMillis() + alarmDelayMillis,
                alarmPending);

        isAlarmSet = true;
    }

    private ObservableCarrier locallyLoadCarrier() {
        lastUpdated = new Date(sharedPref.getLong(LAST_UPDATED_PREF, System.currentTimeMillis()));

        float infectionProbability = sharedPref.getFloat(INFECTION_PROBABILITY_PREF, 0);
        InfectionStatus infectionStatus = InfectionStatus.values()[sharedPref.getInt(INFECTION_STATUS_PREF, InfectionStatus.HEALTHY.ordinal())];

        Layman carrier = new Layman(infectionStatus, infectionProbability, AuthenticationManager.getUserId());

        // Register as observer of Layman
        carrier.addObserver(this);

        return carrier;
    }

    private void locallyStoreCarrier() {
        Carrier me = analyst.getCarrier();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(INFECTION_PROBABILITY_PREF, me.getIllnessProbability())
                .putInt(INFECTION_STATUS_PREF, me.getInfectionStatus().ordinal())
                .putLong(LAST_UPDATED_PREF, lastUpdated.getTime())
                .commit();
    }

    private boolean showServiceNotification() {

        if (boundActivities > 0 || !broker.isProviderEnabled(INTERNET)) {
            // Only show the notification when the UI is NOT running
            return false;
        }

        removeNotifications();

        Intent killIntent = new Intent(this, LocationService.class);
        killIntent.putExtra(POISON_PILL, true);

        PendingIntent pendingKill = PendingIntent.getService(this, 1, killIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CoronaGame.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_protected)
                .setContentTitle("Virus Tracker")
                .setContentText(getString(R.string.background_protection_msg))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.background_protection_msg)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_pause, "PAUSE", pendingKill);


        serviceNotificationId = (int) (Math.random() * 100);

        NotificationManagerCompat.from(this).notify(serviceNotificationId, builder.build());

        return true;
    }

    private void removeNotifications() {
        NotificationManagerCompat.from(this).cancelAll();
    }

    @Override
    public void onCreate() {
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();
        sender = new ConcreteCachingDataSender(gridInteractor);
        receiver = new ConcreteDataReceiver(gridInteractor);

        broker = new ConcreteConnectivityBroker((LocationManager) this.getSystemService(Context.LOCATION_SERVICE), this);

        // Observe Internet connection
        ((ConcreteConnectivityBroker) broker).addObserver(internetObserver);

        sharedPref = this.getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        ObservableCarrier me = locallyLoadCarrier();

        analyst = new ConcreteAnalysis(me, receiver, sender);
        aggregator = new ConcretePositionAggregator(sender, me);

        if (broker.hasPermissions(GPS)) {
            startAggregator();
        }
    }

    /**
     * Updates the infection probability by running the model
     * WARNING: Cannot be called after a period longer than MAX_CACHE_ENTRY_AGE
     * since DataSender cache would have been partially emptied already
     */
    private void updateInfectionModel() {
        SortedMap<Date, Location> locations = sender.getLastPositions().tailMap(lastUpdated);

        // TODO: [LOG]
        Log.e("POSITION_ITERATOR", Integer.toString(locations.size()));
        List<CompletableFuture<Integer>> operationFutures = new ArrayList<>();

        for (Map.Entry<Date, Location> l : locations.entrySet()) {
            operationFutures.add(analyst.updateInfectionPredictions(l.getValue(), lastUpdated, l.getKey()));
            lastUpdated = l.getKey();
        }

        for (CompletableFuture<Integer> future : operationFutures) {
            try {
                future.get(1500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                Log.e("MODEL_UPDATE", "Infection model update timed out");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopService() {
        isAlarmSet = true;
        stopAggregator();
        if (alarmManager != null) {
            alarmManager.cancel(alarmPending);
        }
        // TODO: [LOG]
        Log.e("LOCATION_SERVICE", "Kill message received");
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(POISON_PILL)) {
                stopService();
            } else if (intent.hasExtra(ALARM_GOES_OFF)) {
                isAlarmSet = false;
                // It's time to run the model, starting from time 'lastUpdated';
                new Thread(this::updateInfectionModel).start();
            }
        }

        if (!isAlarmSet) {
            // TODO: [LOG]
            Log.e("LOCATION_SERVICE", "Setting alarm");
            // Create next alarm
            setModelUpdateAlarm();
        }
        return START_STICKY;
    }

    private CompletableFuture<Void> remotelyStoreCarrierStatus(InfectionStatus status) {
        boolean isInfected = status == INFECTED;

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("Infected", isInfected);

        String userId = AuthenticationManager.getAccount(CoronaGame.getContext()).getId();

        // TODO: [LOG]
        Log.e("ACCOUNT_NAME", userId);

        DocumentReference userRef = documentReference("Users", userId);
        CompletableFuture<Void> future1 = taskToFuture(userRef.set(userPayload, SetOptions.merge()));
        CompletableFuture<Void> future2 = taskToFuture(userRef.update("Infected", isInfected));

        return CompletableFuture.allOf(future1, future2);
    }

    @Override
    public void update(Observable o, Object arg) {
        // Store updates to Carrier
        AsyncTask.execute(() -> {
            locallyStoreCarrier();
            remotelyStoreCarrierStatus(analyst.getCarrier().getInfectionStatus());
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Increment number of bound activities
        boundActivities++;
        // TODO: [LOG]
        Log.e("LOCATION_SERVICE", "Unregister binding: " + boundActivities + " remaining");
        if (boundActivities > 0) {
            removeNotifications();
        }
        return new LocationBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boundActivities--;
        // TODO: [LOG]
        Log.e("LOCATION_SERVICE", "Unregister binding: " + boundActivities + " remaining");
        showServiceNotification();
        return super.onUnbind(intent);
    }

    private void displayToast(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onLocationChanged(Location location) {
        if (broker.hasPermissions(GPS)) {
            AsyncTask.execute(() -> aggregator.addPosition(location));
        } else {
            displayToast("Missing Location permission");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated, do nothing
    }

    private boolean startAggregator() {
        if (broker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLIS, MIN_UP_INTERVAL_METERS, this)) {
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
            if (broker.hasPermissions(GPS)) {
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

    public ConnectivityBroker getBroker() {
        return broker;
    }

    @VisibleForTesting
    public void setBroker(ConnectivityBroker broker) {
        this.broker = broker;
    }

    public InfectionAnalyst getAnalyst() {
        return analyst;
    }

    @VisibleForTesting
    public void setAnalyst(InfectionAnalyst analyst) {
        this.analyst = analyst;
    }

    public CachingDataSender getSender() {
        return sender;
    }

    @VisibleForTesting
    public void setSender(CachingDataSender sender) {
        this.sender = sender;
    }

    public DataReceiver getReceiver() {
        return receiver;
    }

    @VisibleForTesting
    public void setReceiver(DataReceiver receiver) {
        this.receiver = receiver;
    }

    public class LocationBinder extends android.os.Binder {
        public LocationService getService() {
            return LocationService.this;
        }

        public boolean hasGpsPermissions() {
            return broker.hasPermissions(GPS);
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


    @Override
    public void onDestroy() {
        Log.e("LOCATION_SERVICE", "Destroying service ...");
        removeNotifications();
        super.onDestroy();
    }
}
