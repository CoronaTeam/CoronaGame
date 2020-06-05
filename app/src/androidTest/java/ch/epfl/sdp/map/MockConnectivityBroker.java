package ch.epfl.sdp.map;

import android.Manifest;
import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;
import androidx.test.rule.ActivityTestRule;

import ch.epfl.sdp.connectivity.ConnectivityBroker;
import ch.epfl.sdp.testActivities.MapActivity;

import static ch.epfl.sdp.connectivity.ConnectivityBroker.Provider.GPS;


public class MockConnectivityBroker implements ConnectivityBroker {
    private LocationListener listener = null;

    private Location fakeLocation;
    private boolean fakeStatus;
    private boolean GPSpermission;

    private final ActivityTestRule<MapActivity> mActivityRule;

    public MockConnectivityBroker(ActivityTestRule<MapActivity> activity) {
        mActivityRule = activity;
        fakeStatus = true;
        GPSpermission = true;
    }


    public void setFakeLocation(Location location) throws Throwable {
        fakeLocation = location;
        mActivityRule.runOnUiThread(() -> listener.onLocationChanged(location));
    }

    public void setProviderStatus(boolean status) throws Throwable {
        fakeStatus = status;
        if (listener != null) {
            if (fakeStatus) {
                mActivityRule.runOnUiThread(() -> listener.onProviderEnabled(LocationManager.GPS_PROVIDER));
            } else {
                mActivityRule.runOnUiThread(() -> listener.onProviderDisabled(LocationManager.GPS_PROVIDER));
            }
        }
    }

    @Override
    public boolean isProviderEnabled(Provider provider) {
        return fakeStatus;
    }

    @Override
    public boolean requestLocationUpdates(Provider provider, long minTimeDelay, float minSpaceDist, LocationListener listener) {
        if (provider == GPS) {
            this.listener = listener;
        }
        return true;
    }

    @Override
    public void removeUpdates(LocationListener listener) {
        this.listener = null;
    }

    @Override
    public Location getLastKnownLocation(Provider provider) {
        return fakeLocation;
    }

    public void setGPSpermission(boolean permission){
        GPSpermission = permission;
    }

    @Override
    public boolean hasPermissions(Provider provider) {
        return GPSpermission;
    }

    @Override
    public void requestPermissions(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
    }
}