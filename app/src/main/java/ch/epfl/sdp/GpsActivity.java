package ch.epfl.sdp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Calendar;

public class GpsActivity extends AppCompatActivity implements LocationListener {

    // TODO: Assign useful value here
    private static final int LOCATION_REQUEST_CODE = 2020;

    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private LocationManager locationManager;

    private EditText latitudeBox;
    private EditText longitudeBox;

    private ArrayAdapter<String> trackerAdapter;

    private double currLatitude, currLongitude;

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean updateLocationPermissions() {
        if (!hasLocationPermissions()) {
            /* This is the correct implementation. Could not test it!
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Need to explicitly ask for location permission
                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_title)
                        .setMessage(R.string.location_permission_description)
                        .setPositiveButton(R.string.location_permission_accept_btn, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(
                                        GpsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_REQUEST_CODE);
                            }
                        })
                        .create()
                        .show();
            } else {
                // Implicit consent
                ActivityCompat.requestPermissions(
                        GpsActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_REQUEST_CODE);
            }
             */
            ActivityCompat.requestPermissions(
                    GpsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);

            return false;
        } else {
            return true;
        }
    }

    // TODO: clean SuppressLint: permission check is done in hasLocationPermission()
    @SuppressLint("MissingPermission")
    private void registerLocationUpdates() {
        if (hasLocationPermissions()) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_UP_INTERVAL_MILLISECS,
                    MIN_UP_INTERVAL_METERS,
                    this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        latitudeBox.setText(Double.toString(location.getLatitude()), TextView.BufferType.SPANNABLE);
        longitudeBox.setText(Double.toString(location.getLongitude()), TextView.BufferType.SPANNABLE);

        double differenceThreshold = 0.0001;
        if (Math.abs(location.getLatitude() - currLatitude) > differenceThreshold ||
                Math.abs(location.getLongitude() - currLongitude) > differenceThreshold) {
            currLatitude = location.getLatitude();
            currLongitude = location.getLongitude();
            Calendar now = Calendar.getInstance();
            trackerAdapter.insert(String.format("Last seen on %d:%d:%d @ %f, %f",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    now.get(Calendar.SECOND),
                    currLatitude,
                    currLongitude), 0);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            registerLocationUpdates();
            Toast.makeText(this, R.string.gps_status_on, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            latitudeBox.setText(R.string.gps_signal_missing);
            longitudeBox.setText(R.string.gps_signal_missing);
            Toast.makeText(this, R.string.gps_status_off, Toast.LENGTH_LONG).show();
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (hasLocationPermissions()) {
                        // TODO: possible optimization here: Use PASSIVE_PROVIDER to save battery!
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_UP_INTERVAL_MILLISECS,
                                MIN_UP_INTERVAL_METERS,
                                this
                        );
                    }
                }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        updateLocationPermissions();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;

        registerLocationUpdates();

        latitudeBox = findViewById(R.id.gpsLatitude);
        longitudeBox = findViewById(R.id.gpsLongitude);

        ListView locationTracker = findViewById(R.id.location_tracker);

        trackerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        locationTracker.setAdapter(trackerAdapter);

        currLatitude = 0;
        currLongitude = 0;
    }

    // TODO: think about using bluetooth technology to improve accuracy

    @Override
    protected void onResume() {
        super.onResume();
        registerLocationUpdates();
    }
}
