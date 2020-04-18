package ch.epfl.sdp.location;

import android.app.Activity;
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

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;

public class LocationService extends Service implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private LocationBroker broker;
    // TODO: Why am I forced to use concrete type
    private ConcretePositionAggregator aggregator;

    private DataReceiver receiver;
    private CachingDataSender sender;

    private Carrier me;

    private InfectionAnalyst analyst;

    private boolean hasGpsPermissions = false;

    @Override
    public void onCreate() {
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();
        sender = new ConcreteCachingDataSender(gridInteractor);
        receiver = new ConcreteDataReceiver(gridInteractor);

        broker = new ConcreteLocationBroker((LocationManager) this.getSystemService(Context.LOCATION_SERVICE), this);

        // TODO: Carrier here must be loaded from the database
        me = new Layman(Carrier.InfectionStatus.HEALTHY);

        analyst = new ConcreteAnalysis(me, receiver,sender);
        aggregator = new ConcretePositionAggregator(sender, analyst);

        refreshPermissions();
        if (hasGpsPermissions) {
            startAggregator();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
