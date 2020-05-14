package ch.epfl.sdp.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.DataExchangeActivity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class LocationServiceTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private AtomicBoolean registered;

    private Location beenThere = TestTools.newLoc(13, 78);
    private Date now = new Date();


    @Before
    public void setupTestIndicator() {
        registered = new AtomicBoolean(false);
    }

    private LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            registered.set(true);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            registered.set(true);
        }

        @Override
        public void onProviderEnabled(String provider) {
            registered.set(true);
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Test
    public void registerForUpdatesFailsWithWrongProvider() {
        LocationService service = mActivityRule.getActivity().getService();

        exception.expect(IllegalArgumentException.class);

        service.getBroker().requestLocationUpdates(LocationBroker.Provider.NETWORK, 1, 1, listener);
    }

    @Test
    public void registrationDependsOnPermissions() throws Throwable {

        AtomicBoolean result = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);

        mActivityRule.runOnUiThread(() ->{
            LocationBroker broker = mActivityRule.getActivity().getService().getBroker();
            boolean hasPermissions = broker.hasPermissions(LocationBroker.Provider.GPS);
            boolean registrationSucceeded = broker.requestLocationUpdates(LocationBroker.Provider.GPS, 1, 1, listener);
            result.set(hasPermissions == registrationSucceeded);
            done.set(true);
        });

        while (!done.get()) {}

        assertThat(result.get(), equalTo(true));

        mActivityRule.runOnUiThread(() -> {
            LocationBroker broker = mActivityRule.getActivity().getService().getBroker();
            broker.removeUpdates(listener);
        });

    }

    @Test
    public void obtainNullWithoutPermissions() throws Throwable {

        AtomicBoolean result = new AtomicBoolean(false);
        AtomicBoolean done = new AtomicBoolean(false);

        mActivityRule.runOnUiThread(() -> {
            LocationBroker broker = mActivityRule.getActivity().getService().getBroker();
            boolean hasPermission = broker.hasPermissions(LocationBroker.Provider.GPS);
            Location loc = broker.getLastKnownLocation(LocationBroker.Provider.GPS);

            if (hasPermission) {
                result.set(true);
            } else {
                result.set(loc == null);
            }

            done.set(true);
        });

        while (!done.get()) { }

        assertThat(result.get(), equalTo(true));
    }

    @Test
    public void canStopAggregator() throws Throwable {
        mActivityRule.getActivity().getService().onProviderDisabled(LocationManager.GPS_PROVIDER);
        // TODO: This test should check the effects of the operation
        mActivityRule.finishActivity();
    }

    // TODO: Move to DemoTools
    @Test
    public void storedUserStatus() {
        SharedPreferences sharedPrefs = mActivityRule.getActivity().getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        Log.e("USER_STATUS", Carrier.InfectionStatus.values()[sharedPrefs.getInt(LocationService.INFECTION_STATUS_PREF, 0)].toString());
        Log.e("USER_PROB", Float.toString(sharedPrefs.getFloat(LocationService.INFECTION_PROBABILITY_PREF, -1.f)));
    }

    // TODO: Move to DemoTools
    @Test
    public void changesToCarrierAreSavedLocally() {
        LocationService service = mActivityRule.getActivity().getService();

        service.getAnalyst().updateStatus(Carrier.InfectionStatus.INFECTED);

        TestTools.sleep();

        SharedPreferences sharedPrefs = mActivityRule.getActivity().getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);

        assertThat(sharedPrefs.getFloat(LocationService.INFECTION_PROBABILITY_PREF, 0f), equalTo(1f));
        assertThat(sharedPrefs.getInt(LocationService.INFECTION_STATUS_PREF, 0), equalTo(Carrier.InfectionStatus.INFECTED.ordinal()));

        service.getAnalyst().updateStatus(Carrier.InfectionStatus.HEALTHY);

        TestTools.sleep();

        assertThat(sharedPrefs.getFloat(LocationService.INFECTION_PROBABILITY_PREF, 0f), equalTo(0f));
        assertThat(sharedPrefs.getInt(LocationService.INFECTION_STATUS_PREF, 0), equalTo(Carrier.InfectionStatus.HEALTHY.ordinal()));
    }
}
