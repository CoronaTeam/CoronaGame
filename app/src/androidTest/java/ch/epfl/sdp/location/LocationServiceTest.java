package ch.epfl.sdp.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.test.rule.ActivityTestRule;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.DefaultAuthenticationManager;
import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.TestUtils;
import ch.epfl.sdp.contamination.CachingDataSender;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.DataExchangeActivity;
import ch.epfl.sdp.contamination.FakeAnalyst;
import ch.epfl.sdp.contamination.FakeCachingDataSender;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class LocationServiceTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static String fakeUserID = "THIS_IS_A_FAKE_ID";

    private AtomicBoolean registered;

    private Carrier iAmBob = new Layman(HEALTHY);
    private Location beenThere = TestUtils.buildLocation(13, 78);
    private Date now = new Date();

    private AtomicInteger sentinel;

    @BeforeClass
    public static void mockUserId() {
        // To not pollute application status, make AuthenticationManager return a mock UserID
        AuthenticationManager.defaultManager = new DefaultAuthenticationManager() {
            @Override
            public String getUserId() {
                return fakeUserID;
            }
        };
    }

    @AfterClass
    public static void restoreUserId() {
        // Restore real UserID
        AuthenticationManager.defaultManager = new DefaultAuthenticationManager() {};
    }

    @Before public void resetSentinel() {
        sentinel = new AtomicInteger(0);
    }

    @AfterClass
    public static void resetFakeCarrierStatus() {
        SharedPreferences sharedPreferences = CoronaGame.getContext().getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.remove(LocationService.INFECTION_STATUS_TAG)
                .remove(LocationService.INFECTION_PROBABILITY_TAG)
                .remove(LocationService.LAST_UPDATED_TAG)
                .commit();
    }

    private InfectionAnalyst analystWithSentinel = new InfectionAnalyst() {
        @Override
        public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {
            sentinel.incrementAndGet();
            return CompletableFuture.completedFuture(0);
        }

        @Override
        public Carrier getCarrier() {
            return iAmBob;
        }

        @Override
        public boolean updateStatus(Carrier.InfectionStatus stat) {
            return false;
        }
    };

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
    public void fakeUserIdIsSet() {
        assertThat(AuthenticationManager.getUserId(), equalTo(fakeUserID));
    }

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


    private void startLocationServiceWithAlarm() {
        Intent intentWithAlarm = new Intent(mActivityRule.getActivity(), LocationService.class);
        intentWithAlarm.putExtra(LocationService.ALARM_GOES_OFF,true);
        mActivityRule.getActivity().startService(intentWithAlarm);
    }

    @Test
    public void updateNotDoneWithoutNewLocations() {

        mActivityRule.getActivity().getService().setAnalyst(analystWithSentinel);

        startLocationServiceWithAlarm();

        TestTools.sleep(1000);

        assertThat(sentinel.get(), equalTo(1));
    }

    @Test
    public void modelUpdatedWhenAlarmAndNewLocations() {

        mActivityRule.getActivity().getService().setAnalyst(analystWithSentinel);

        assertThat(sentinel.get(), equalTo(0));

        Date now = new Date();
        CachingDataSender fakeSender = new FakeCachingDataSender();
        fakeSender.registerLocation(iAmBob, TestUtils.buildLocation(0, 0), now);
        mActivityRule.getActivity().getService().setSender(fakeSender);

        startLocationServiceWithAlarm();

        TestTools.sleep(1000);

        assertThat(sentinel.get(), equalTo(1));
    }

    @Test(timeout = 10000)
    public void alarmSetByServiceIsSuccessful() {

        mActivityRule.getActivity().getService().setAnalyst(analystWithSentinel);

        LocationService.setAlarmDelay(1000);

        startLocationServiceWithAlarm();

        assertThat(sentinel.get(), equalTo(0));

        Date now = new Date();
        CachingDataSender fakeSender = new FakeCachingDataSender();
        fakeSender.registerLocation(iAmBob, TestUtils.buildLocation(1, 1), now);
        mActivityRule.getActivity().getService().setSender(fakeSender);

        while (sentinel.get() == 0) {}

        assertThat(sentinel.get(), equalTo(1));
    }

    @Test
    public void carrierStatusIsStored() {

        LocationService service = mActivityRule.getActivity().getService();

        // Pass LocationService's carrier to FakeAnalyst
        InfectionAnalyst fakeAnalyst = new FakeAnalyst(service.getAnalyst().getCarrier());

        CachingDataSender fakeSender = new FakeCachingDataSender();

        service.setAnalyst(fakeAnalyst);
        service.setSender(fakeSender);

        startLocationServiceWithAlarm();

        assertThat(fakeAnalyst.getCarrier().getIllnessProbability(), equalTo(0f));
        assertThat(fakeAnalyst.getCarrier().getInfectionStatus(), equalTo(HEALTHY));

        fakeAnalyst.updateStatus(UNKNOWN);
        assertThat(fakeAnalyst.getCarrier().setIllnessProbability(.3f), equalTo(true));

        Date aDate = new Date();

        fakeSender.registerLocation(fakeAnalyst.getCarrier(), TestUtils.buildLocation(1, 1), aDate);

        startLocationServiceWithAlarm();

        TestTools.sleep();

        SharedPreferences sharedPreferences = CoronaGame.getContext().getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        assertThat(sharedPreferences.getInt(LocationService.INFECTION_STATUS_TAG, HEALTHY.ordinal()), equalTo(UNKNOWN.ordinal()));
        assertThat(sharedPreferences.getFloat(LocationService.INFECTION_PROBABILITY_TAG, 0f), equalTo(.3f));
    }
}
