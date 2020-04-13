package ch.epfl.sdp.location;

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
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.ConcreteDataSender;
import ch.epfl.sdp.contamination.ConcretePositionAggregator;
import ch.epfl.sdp.contamination.DataReceiver;
import ch.epfl.sdp.contamination.DataSender;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;
import kotlin.NotImplementedError;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;

public class LocationService extends Service implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private LocationBroker broker;
    // TODO: Why am I forced to use concrete type
    private ConcretePositionAggregator aggregator;

    private DataReceiver receiver;
    private DataSender sender;

    private Carrier me;

    private InfectionAnalyst analyst;

    @Override
    public void onCreate() {
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();
        sender = new ConcreteDataSender(gridInteractor);
        receiver = new ConcreteDataReceiver(gridInteractor);

        broker = new ConcreteLocationBroker((LocationManager) this.getSystemService(Context.LOCATION_SERVICE), this);

        // TODO: Carrier here must be loaded from the database
        me = new Layman(Carrier.InfectionStatus.HEALTHY);

        analyst = new ConcreteAnalysis(me, receiver);
        aggregator = new ConcretePositionAggregator(sender, analyst);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public class LocationBinder extends android.os.Binder {
        public LocationService getService() {
            return LocationService.this;
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

    @Override
    public void onLocationChanged(Location location) {
        if (broker.hasPermissions(GPS)) {
            aggregator.addPosition(location);
        } else {
            displayToast("Missing permission");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated, do nothing
    }

    private void goOnline() {
        broker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
        displayToast(getString(R.string.gps_status_on));
        aggregator.updateToOnline();
    }

    private void goOffline() {
        displayToast(getString(R.string.gps_status_off));
        aggregator.updateToOffline();
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (broker.hasPermissions(GPS)) {
                goOnline();
            } else {
                // TODO: Do something effective to obtain permissions
                throw new NotImplementedError();
                //broker.requestPermissions(, LOCATION_PERMISSION_REQUEST);
                /* USED IN onRequestPermissionResult
                if (requestCode == LOCATION_PERMISSION_REQUEST
                        && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goOnline();
                }
                 */
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            goOffline();
        }
    }

    public LocationBroker getBroker() {
        return broker;
    }

    public InfectionAnalyst getAnalyst() {
        return analyst;
    }

    public DataSender getSender() {
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
    public void setSender(DataSender sender) {
        this.sender = sender;
    }

    public void setBroker(LocationBroker broker) {
        this.broker = broker;
    }

    @VisibleForTesting
    public void setAnalyst(InfectionAnalyst analyst) {
        this.analyst = analyst;
    }
}
