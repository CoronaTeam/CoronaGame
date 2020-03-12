package ch.epfl.sdp;

import android.location.Location;
import android.location.LocationListener;

/**
 * Wrapper for LocationManager
 * Used to test functionality without android permissions to mock locations
 */
public interface LocationBroker {

    enum Provider {
        GPS,
        NETWORK
    }

    boolean isProviderEnabled(Provider provider);

    /**
     * As in LocationManager except for returned value semantic.
     * Before performing the operation, permissions are checked.
     * If the action is not allowed, the function requests it and then
     * returns 'false' (without performing additional actions)
     * The function can then be called again inside onRequestPermissionResults
     * @param provider the location provider
     * @param minTimeDelay minimum delay between updates (in seconds)
     * @param minSpaceDist minimum distance between updates (in meters)
     * @param listener the location listener
     * @return false if the activity doesn't have enough permissions
     */
    boolean requestLocationUpdates(Provider provider, long minTimeDelay, float minSpaceDist, LocationListener listener);

    void removeUpdates(LocationListener listener);

    Location getLastKnownLocation(Provider provider);

    boolean hasPermissions(Provider provider);

    void requestPermissions(int requestCode);
}
