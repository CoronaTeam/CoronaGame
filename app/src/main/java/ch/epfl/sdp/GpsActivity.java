package ch.epfl.sdp;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.location.LocationService;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;
import static java.lang.String.format;
import static java.lang.String.valueOf;

public class GpsActivity extends AppCompatActivity implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private EditText latitudeBox;
    private EditText longitudeBox;
    private TextView uploadStatus;

    private ArrayAdapter<String> trackerAdapter;

    private Location prevLocation;

    private Account account;

    private HistoryFirestoreInteractor db;

    private LocationService service;

    private boolean connectedToService = false;

    @VisibleForTesting
    void setFirestoreInteractor(FirestoreInteractor interactor) {
        db.setFirestoreInteractor(interactor);
    }

    private void displayNewLocation(Location newLocation) {
        latitudeBox.setText(valueOf(newLocation.getLatitude()), TextView.BufferType.SPANNABLE);
        longitudeBox.setText(valueOf(newLocation.getLongitude()), TextView.BufferType.SPANNABLE);

        if (prevLocation == null) {
            prevLocation = newLocation;
        }

        double differenceThreshold = 0.0001;
        if (Math.abs(newLocation.getLatitude() - prevLocation.getLatitude()) > differenceThreshold ||
                Math.abs(newLocation.getLongitude() - prevLocation.getLongitude()) > differenceThreshold) {
            prevLocation = newLocation;
            Calendar now = Calendar.getInstance();
            trackerAdapter.insert(format("Last seen on %d:%d:%d @ %f, %f",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    now.get(Calendar.SECOND),
                    newLocation.getLatitude(),
                    newLocation.getLongitude()), 0);
        }
    }

    private void registerNewLocation(Location newLocation) {
        uploadStatus.setText(R.string.uploading_with_dots);
        Map<String, Object> element = new HashMap();
        element.put("Position", new PositionRecord(Timestamp.now(),
                new GeoPoint(newLocation.getLatitude(), newLocation.getLongitude())));
        db.write(element, o -> uploadStatus.setText(R.string.sync_ok), e -> uploadStatus.setText(R.string.sync_error));
    }

    @Override
    public void onLocationChanged(Location location) {
        if (service.getBroker().hasPermissions(GPS)) {
            registerNewLocation(location);
            displayNewLocation(location);
        } else {
            Toast.makeText(this, "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void goOnline() {
        service.getBroker().requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
//        Toast.makeText(this, R.string.gps_status_on, Toast.LENGTH_SHORT).show();
    }

    private void goOffline() {
        latitudeBox.setText(R.string.gps_signal_missing);
        longitudeBox.setText(R.string.gps_signal_missing);
//        Toast.makeText(this, R.string.gps_status_off, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (service.getBroker().hasPermissions(GPS)) {
                goOnline();
            } else {
                service.getBroker().requestPermissions(this, LOCATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            goOffline();
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goOnline();
        }
    }

    @VisibleForTesting
    void activatePosition() {
        if (service.getBroker().isProviderEnabled(GPS) && service.getBroker().hasPermissions(GPS)) {
            goOnline();
        } else if (service.getBroker().isProviderEnabled(GPS)) {
            // Must ask for permissions
            service.getBroker().requestPermissions(this, LOCATION_PERMISSION_REQUEST);
        } else {
            goOffline();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        latitudeBox = findViewById(R.id.gpsLatitude);
        longitudeBox = findViewById(R.id.gpsLongitude);
        uploadStatus = findViewById(R.id.history_upload_status);

        ListView locationTracker = findViewById(R.id.location_tracker);

        trackerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        locationTracker.setAdapter(trackerAdapter);


        // TODO: Take real account
        account = AuthenticationManager.getAccount(this);

        db = new HistoryFirestoreInteractor(account);
        Log.e("TEST", account.getId());

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                GpsActivity.this.service = ((LocationService.LocationBinder)service).getService();
                connectedToService = true;

                activatePosition();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                connectedToService = false;
                GpsActivity.this.service = null;
            }
        };

        Intent intent = new Intent(this, LocationService.class);

        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        // Now it's connected to LocationService
    }

    // TODO: think about using bluetooth technology to improve accuracy

}
