package ch.epfl.sdp.map;

import android.app.Activity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.test.rule.ActivityTestRule;

import ch.epfl.sdp.location.LocationBroker;
import ch.epfl.sdp.testActivities.MapActivity;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;


public class MockLocationBroker implements LocationBroker {
    private LocationListener listener = null;

    private Location fakeLocation;
    private boolean fakeStatus = true;

    private ActivityTestRule<MapActivity> mActivityRule;

    public MockLocationBroker(ActivityTestRule<MapActivity> activity) {
        mActivityRule = activity;
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

    @Override
    public boolean hasPermissions(Provider provider) {
        return true;
    }

    @Override
    public void requestPermissions(Activity activity, int requestCode) {
        // Trivial since always has permissions
    }
}