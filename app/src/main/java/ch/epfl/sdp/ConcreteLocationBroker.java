package ch.epfl.sdp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

import static ch.epfl.sdp.LocationBroker.Provider.GPS;
import static ch.epfl.sdp.LocationBroker.Provider.NETWORK;

public class ConcreteLocationBroker implements LocationBroker {

    // TODO: use Map.of()
    private static final Map<Provider, String> providerToString = new HashMap<Provider, String>() {{
        put(GPS, LocationManager.GPS_PROVIDER);
        put(NETWORK, LocationManager.NETWORK_PROVIDER);
    }};

    private final LocationManager locationManager;
    private final Activity activity;

    ConcreteLocationBroker(LocationManager locationManager, Activity activity) {
        this.locationManager = locationManager;
        this.activity = activity;
    }

    @Override
    public boolean isProviderEnabled(@NonNull Provider provider) {
        assert provider != null;
        return locationManager.isProviderEnabled(providerToString.get(provider));
    }


    @SuppressLint("MissingPermission")
    @Override
    public boolean requestLocationUpdates(@NonNull Provider provider, long minTimeDelay, float minSpaceDist, LocationListener listener) {
        assert provider != null;
        if (!providerToString.containsKey(provider)) {
            throw new IllegalArgumentException(String.format("Provider %s is not yet supported", provider));
        }

        if (hasPermissions(provider)) {
            locationManager.requestLocationUpdates(providerToString.get(provider), minTimeDelay, minSpaceDist, listener);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removeUpdates(LocationListener listener) {
        locationManager.removeUpdates(listener);
    }

    @SuppressLint("MissingPermission")
    @Override
    public Location getLastKnownLocation(@NonNull Provider provider) {
        assert provider != null;
        if (hasPermissions(provider)) {
            return locationManager.getLastKnownLocation(providerToString.get(provider));
        }
        return null;
    }

    @Override
    public boolean hasPermissions(Provider provider) {
        if (provider != GPS) {
            throw new IllegalArgumentException();
        }
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestPermissions(int requestCode) {
        // TODO: uncomment this
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
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
    }
}
