package ch.epfl.sdp.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.DefaultAuthenticationManager;
import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.contamination.CachingDataSender;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.DataExchangeActivity;
import ch.epfl.sdp.contamination.FakeAnalyst;
import ch.epfl.sdp.contamination.FakeCachingDataSender;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CarrierUpdatePersistenceTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    private static String fakeUserID = "THIS_IS_A_FAKE_ID";

    private Carrier iAmBob;
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

    @Before
    public void initBob() {
        iAmBob = new Layman(HEALTHY, fakeUserID);
    }

    @Before
    public void resetSentinel() {
        sentinel = new AtomicInteger(0);
    }

    // TODO: Should convert it to @After
    @Before
    public void resetFakeUserHistory() {
        // Delete existing file
        initStorageManager().delete();

        LocationService service = mActivityRule.getActivity().getService();

        Carrier newCarrier = new Layman(
                service.getAnalyst().getCarrier().getInfectionStatus(),
                service.getAnalyst().getCarrier().getIllnessProbability(),
                AuthenticationManager.getUserId()
        );

        service.setAnalyst(new ConcreteAnalysis(newCarrier, service.getReceiver(), service.getSender()));
    }

    @After
    public void stopLocationService() {
        mActivityRule.getActivity().stopService(new Intent(mActivityRule.getActivity(), LocationService.class));
    }

    @AfterClass
    public static void resetFakeCarrierStatus() {
        SharedPreferences sharedPreferences = CoronaGame.getContext().getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.remove(LocationService.INFECTION_STATUS_PREF)
                .remove(LocationService.INFECTION_PROBABILITY_PREF)
                .remove(LocationService.LAST_UPDATED_PREF)
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

    private void startLocationServiceWithAlarm() {
        Intent intentWithAlarm = new Intent(mActivityRule.getActivity(), LocationService.class);
        intentWithAlarm.putExtra(LocationService.ALARM_GOES_OFF,true);
        mActivityRule.getActivity().startService(intentWithAlarm);
    }

    private InfectionAnalyst realAnalyst;

    private void useAnalystWithSentinel() {
        realAnalyst = mActivityRule.getActivity().getService().getAnalyst();
        mActivityRule.getActivity().getService().setAnalyst(analystWithSentinel);
    }

    private void restoreRealAnalyst() {
        mActivityRule.getActivity().getService().setAnalyst(realAnalyst);
    }

    @Test
    public void fakeUserIdIsSet() {
        assertThat(AuthenticationManager.getUserId(), equalTo(fakeUserID));
    }

    @Test
    public void updateNotDoneWithoutNewLocations() {
        useAnalystWithSentinel();
        startLocationServiceWithAlarm();

        TestTools.sleep(1000);

        assertThat(sentinel.get(), equalTo(0));

        restoreRealAnalyst();
    }

    @Test
    public void modelUpdatedWhenAlarmAndNewLocations() {

        useAnalystWithSentinel();

        assertThat(sentinel.get(), equalTo(0));

        Date now = new Date();
        CachingDataSender fakeSender = new FakeCachingDataSender();
        fakeSender.registerLocation(iAmBob, TestTools.newLoc(0, 0), now);
        mActivityRule.getActivity().getService().setSender(fakeSender);

        startLocationServiceWithAlarm();

        TestTools.sleep(1000);

        assertThat(sentinel.get(), equalTo(1));

        restoreRealAnalyst();
    }

    @Test(timeout = 10000)
    public void alarmSetByServiceIsSuccessful() {

        useAnalystWithSentinel();

        sentinel.set(0);

        assertThat(sentinel.get(), equalTo(0));

        LocationService.setAlarmDelay(2000);
        startLocationServiceWithAlarm();


        Date now = new Date();
        CachingDataSender fakeSender = new FakeCachingDataSender();
        fakeSender.registerLocation(iAmBob, TestTools.newLoc(1, 1), now);
        mActivityRule.getActivity().getService().setSender(fakeSender);

        while (sentinel.get() == 0) {}

        assertThat(sentinel.get(), equalTo(1));

        restoreRealAnalyst();

        LocationService.setAlarmDelay(2000);
    }

    @Test
    public void carrierStatusIsStored() {

        LocationService service = mActivityRule.getActivity().getService();

        // Pass LocationService's carrier to FakeAnalyst
        InfectionAnalyst fakeAnalyst = new FakeAnalyst(service.getAnalyst().getCarrier());

        CachingDataSender fakeSender = new FakeCachingDataSender();

        service.setAnalyst(fakeAnalyst);
        service.setSender(fakeSender);

        ((Layman)service.getAnalyst().getCarrier()).addObserver(service);

        startLocationServiceWithAlarm();

        assertThat(fakeAnalyst.getCarrier().getIllnessProbability(), equalTo(0f));
        assertThat(fakeAnalyst.getCarrier().getInfectionStatus(), equalTo(HEALTHY));

        fakeAnalyst.updateStatus(UNKNOWN);
        TestTools.sleep();

        assertThat(fakeAnalyst.getCarrier().setIllnessProbability(.3f), equalTo(true));
        TestTools.sleep();

        Date aDate = new Date();

        fakeSender.registerLocation(fakeAnalyst.getCarrier(), TestTools.newLoc(1, 1), aDate);

        startLocationServiceWithAlarm();

        TestTools.sleep();

        SharedPreferences sharedPreferences = CoronaGame.getContext().getSharedPreferences(CoronaGame.SHARED_PREF_FILENAME, Context.MODE_PRIVATE);
        assertThat(sharedPreferences.getInt(LocationService.INFECTION_STATUS_PREF, -1), equalTo(UNKNOWN.ordinal()));
        assertThat(sharedPreferences.getFloat(LocationService.INFECTION_PROBABILITY_PREF, 0f), equalTo(.3f));
    }

    @Test
    public void carrierStatusIsReloadedOnRestart() {

        LocationService serviceBefore = mActivityRule.getActivity().getService();

        assertThat(serviceBefore.getAnalyst().getCarrier().getInfectionStatus(), equalTo(HEALTHY));

        InfectionAnalyst fakeAnalyst = new FakeAnalyst(serviceBefore.getAnalyst().getCarrier());

        serviceBefore.setAnalyst(fakeAnalyst);
        serviceBefore.setSender(new FakeCachingDataSender());

        // Modify carrier status
        fakeAnalyst.updateStatus(UNKNOWN);
        assertThat(fakeAnalyst.getCarrier().setIllnessProbability(.45f), equalTo(true));

        // Stop LocationService
        mActivityRule.getActivity().unbindService(mActivityRule.getActivity().serviceConnection);
        serviceBefore.stopSelf();

        // Restart the service and bind it
        Intent serviceIntent = new Intent(mActivityRule.getActivity(), LocationService.class);
        mActivityRule.getActivity().startService(serviceIntent);

        mActivityRule.getActivity().bindLocationService();

        Carrier carrierAfter = mActivityRule.getActivity().getService().getAnalyst().getCarrier();

        assertThat(carrierAfter.getInfectionStatus(), equalTo(UNKNOWN));
        assertThat(carrierAfter.getIllnessProbability(), equalTo(.45f));

        carrierAfter.deleteLocalProbabilityHistory();
    }

    private StorageManager<Date, Float> initStorageManager() {
        return new ConcreteManager<>(
                mActivityRule.getActivity(),
                AuthenticationManager.getUserId() + ".csv",
                date -> {
                    try {
                        return CoronaGame.dateFormat.parse(date);
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("The file specified has wrong format: field 'date'");
                    }
                },
                Float::valueOf);
    }

    @Test
    public void carrierHistoryTest() {
        LocationService service = mActivityRule.getActivity().getService();

        service.getAnalyst().getCarrier().setIllnessProbability(.1f);
        TestTools.sleep();
        service.getAnalyst().getCarrier().setIllnessProbability(.2f);
        TestTools.sleep();
        service.getAnalyst().getCarrier().setIllnessProbability(.6f);
        TestTools.sleep();
        service.getAnalyst().updateStatus(INFECTED);

        StorageManager<Date, Float> manager = initStorageManager();
        assertThat(manager.read().size(), equalTo(3));

        service.getAnalyst().getCarrier().deleteLocalProbabilityHistory();
    }
}
