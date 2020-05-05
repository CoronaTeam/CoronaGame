package ch.epfl.sdp.location;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import ch.epfl.sdp.TestUtils;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.ConcreteCachingDataSender;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.DataExchangeActivity;
import ch.epfl.sdp.contamination.FakeCachingDataSender;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.firestore.FirestoreInteractor;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class LocationServiceTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    public InfectionAnalyst uncallableAnalyst;

    private AtomicBoolean registered;

    private Carrier iAmBob = new Layman(HEALTHY);
    private Location beenThere = TestUtils.buildLocation(13, 78);
    private Date now = new Date();

    @Before
    public void setupMockito() {
        GridFirestoreInteractor gridFirestoreInteractor = new GridFirestoreInteractor();
        uncallableAnalyst = new ConcreteAnalysis(
                new Layman(HEALTHY),
                new ConcreteDataReceiver(gridFirestoreInteractor),
                new ConcreteCachingDataSender(gridFirestoreInteractor));
        //when(uncallableAnalyst.updateInfectionPredictions(anyObject(), anyObject(), anyObject()))
        //        .thenThrow(IllegalArgumentException.class);
    }

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

    @After
    public void checkWhichThreadIsStillRunning() {
        Log.e("THIS IS JUST TO TEST", "....");
    }

    Carrier me;
    Date lastUpdated;

    @Test
    public void canRetrieveCarrierFromFirestore() {
        //mActivityRule.getActivity().bindLocationService();

        CompletableFuture<Map<String, Object>> crr = new GridFirestoreInteractor().readDocument(FirestoreInteractor.documentReference("privateUser", "USER_ID_X42"));

        crr.thenAccept(map -> {
            float infectionProbability = (float) ((double) map.getOrDefault(LocationService.INFECTION_PROBABILITY_TAG, 0.d));
            String infectionStatus = (String) map.getOrDefault(LocationService.INFECTION_STATUS_TAG, Carrier.InfectionStatus.HEALTHY.toString());

            me = new Layman(Carrier.InfectionStatus.valueOf(infectionStatus), infectionProbability);

            lastUpdated = new Date((long) map.getOrDefault(LocationService.LAST_UPDATED_TAG, System.currentTimeMillis()));

        }).exceptionally(e -> {
            e.printStackTrace();
            throw new IllegalStateException(e);
            /*
            me = new Layman(Carrier.InfectionStatus.HEALTHY);
            lastUpdated = new Date();
            done.set(true);
            return null;
            */
        }).join();

        assertThat(me, not(equalTo(null)));

    }

    @Test
    public void serviceStartsWithoutAlarm() {
        uncallableAnalyst.updateInfectionPredictions(null, null, null);
    }

    @Test
    public void canRetrieveInfectionProbability() {
        LocationService service = mActivityRule.getActivity().getService();

        service.setSender(new FakeCachingDataSender());
        service.getSender().registerLocation(iAmBob, beenThere, now);

        AtomicInteger locationNum = new AtomicInteger(0);
        AtomicReference<Location> locationRef = new AtomicReference<>();

        InfectionAnalyst fakeAnalyst = new InfectionAnalyst() {
            @Override
            public CompletableFuture<Void> updateInfectionPredictions(Location location, Date startTime, Date endTime) {
                locationNum.incrementAndGet();
                locationRef.set(location);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public Carrier getCarrier() {
                return null;
            }

            @Override
            public boolean updateStatus(Carrier.InfectionStatus stat) {
                return false;
            }
        };

        service.setAnalyst(fakeAnalyst);

        Intent updateAlarm = new Intent(mActivityRule.getActivity(), LocationService.class);
        updateAlarm.putExtra(LocationService.ALARM_GOES_OFF, true);

        mActivityRule.getActivity().startService(updateAlarm);
    }

    @Test
    public void canAddStageToCompletedFuture() {
        CompletableFuture<Integer> futureAccumulator = new CompletableFuture<>();
        CompletableFuture<Integer> incremented = futureAccumulator.thenApply(acc -> acc + 1);
        futureAccumulator.complete(0);
        CompletableFuture<Integer> furtherIncremented = incremented.thenApply(acc -> acc + 2);
        assertThat(furtherIncremented.join(), equalTo(3));
        assertThat(furtherIncremented.thenApply(acc -> acc*(-1)).join(), equalTo(-3));
    }
}
