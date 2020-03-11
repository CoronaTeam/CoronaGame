package ch.epfl.sdp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import static ch.epfl.sdp.LocationBroker.Provider.GPS;

public class GpsActivity extends AppCompatActivity implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private LocationBroker locationBroker;

    private EditText latitudeBox;
    private EditText longitudeBox;

    private ArrayAdapter<String> trackerAdapter;

    private Location prevLocation;

    @VisibleForTesting
    void setLocationBroker(LocationBroker testBroker) {
        locationBroker = testBroker;
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationBroker.hasPermissions(GPS)) {
            latitudeBox.setText(String.valueOf(location.getLatitude()), TextView.BufferType.SPANNABLE);
            longitudeBox.setText(String.valueOf(location.getLongitude()), TextView.BufferType.SPANNABLE);

            if (prevLocation == null) {
                prevLocation = location;
            }

            double differenceThreshold = 0.0001;
            if (Math.abs(location.getLatitude() - prevLocation.getLatitude()) > differenceThreshold ||
                    Math.abs(location.getLongitude() - prevLocation.getLongitude()) > differenceThreshold) {
                prevLocation = location;
                Calendar now = Calendar.getInstance();
                trackerAdapter.insert(String.format("Last seen on %d:%d:%d @ %f, %f",
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        now.get(Calendar.SECOND),
                        location.getLatitude(),
                        location.getLongitude()), 0);
            }
        } else {
            Toast.makeText(this, "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void goOnline() {
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
        Toast.makeText(this, R.string.gps_status_on, Toast.LENGTH_SHORT).show();
    }

    private void goOffline() {
        latitudeBox.setText(R.string.gps_signal_missing);
        longitudeBox.setText(R.string.gps_signal_missing);
        Toast.makeText(this, R.string.gps_status_off, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (locationBroker.hasPermissions(GPS)) {
                goOnline();
            } else {
                locationBroker.requestPermissions(LOCATION_PERMISSION_REQUEST);
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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        latitudeBox = findViewById(R.id.gpsLatitude);
        longitudeBox = findViewById(R.id.gpsLongitude);

        ListView locationTracker = findViewById(R.id.location_tracker);

        trackerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        locationTracker.setAdapter(trackerAdapter);

        // TODO: do not execute in production code
        if (locationBroker == null) {
            locationBroker = new ConcreteLocationBroker((LocationManager) getSystemService(Context.LOCATION_SERVICE), this);
        }
    }

    // TODO: think about using bluetooth technology to improve accuracy

    @Override
    protected void onResume() {
        super.onResume();
        if (locationBroker.isProviderEnabled(GPS) && locationBroker.hasPermissions(GPS)) {
            goOnline();
        } else if (locationBroker.isProviderEnabled(GPS)) {
            // Must ask for permissions
            locationBroker.requestPermissions(LOCATION_PERMISSION_REQUEST);
        } else {
            goOffline();
        }
    }
}
