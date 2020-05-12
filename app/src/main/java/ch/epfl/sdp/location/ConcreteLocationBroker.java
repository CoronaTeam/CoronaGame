package ch.epfl.sdp.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;
import static ch.epfl.sdp.location.LocationBroker.Provider.NETWORK;

public class ConcreteLocationBroker implements LocationBroker {

    private LocationManager locationManager;
    private Context context;

    public ConcreteLocationBroker(LocationManager locationManager, Context context) {
        this.locationManager = locationManager;
        this.context = context;
    }

    private static final Map<Provider, String> providerToString = new HashMap<Provider, String>() {{
        put(GPS, LocationManager.GPS_PROVIDER);
        put(NETWORK, LocationManager.NETWORK_PROVIDER);
    }};

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

        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestPermissions(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
    }
}
