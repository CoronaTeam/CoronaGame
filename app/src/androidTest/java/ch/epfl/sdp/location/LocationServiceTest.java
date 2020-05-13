package ch.epfl.sdp.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.testActivities.DataExchangeActivity;

import static ch.epfl.sdp.TestTools.initSafeTest;
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
    public void setUp() {
        initSafeTest(mActivityRule, true);
        registered = new AtomicBoolean(false);

    }

    @After
    public void release(){
        Intents.release();
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
}
