package ch.epfl.sdp.location;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import androidx.test.espresso.intent.Intents;
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

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.FakeAnalyst;
import ch.epfl.sdp.contamination.FakeDataExchanger;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.Layman;
import ch.epfl.sdp.contamination.ObservableCarrier;
import ch.epfl.sdp.contamination.databaseIO.DataSender;
import ch.epfl.sdp.contamination.databaseIO.PositionHistoryManager;
import ch.epfl.sdp.identity.AuthenticationManager;
import ch.epfl.sdp.identity.DefaultAuthenticationManager;
import ch.epfl.sdp.storage.ConcreteManager;
import ch.epfl.sdp.storage.StorageManager;
import ch.epfl.sdp.testActivities.DataExchangeActivity;

import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.UNKNOWN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CarrierUpdatePersistenceTest {

    private static String fakeUserID = "THIS_IS_A_FAKE_ID";
    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);
    Intent locaIntentWithAlarm;
    private ObservableCarrier iAmBob = new Layman(HEALTHY);
    private AtomicInteger sentinel;
    private InfectionAnalyst analystWithSentinel = new InfectionAnalyst() {
        @Override
        public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {
            sentinel.incrementAndGet();
            return CompletableFuture.completedFuture(0);
        }

        @Override
        public ObservableCarrier getCarrier() {
            return iAmBob;
        }
    };
    private InfectionAnalyst realAnalyst;
    private InfectionAnalyst originalAnalyst;

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
        AuthenticationManager.defaultManager = new DefaultAuthenticationManager() {
        };
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

    @Before
    public void before() {
        initSafeTest(mActivityRule, true);

        iAmBob = new Layman(HEALTHY, fakeUserID);

        LocationService service = mActivityRule.getActivity().getService();
        originalAnalyst = service.getAnalyst();
        InfectionAnalyst fakeAnalyst = new ConcreteAnalysis(iAmBob, service.getReceiver());
        service.setAnalyst(fakeAnalyst);

        sentinel = new AtomicInteger(0);
    }

    @After
    public void resetAnalyst() {
        // TODO: @Adrien this mechanism can be refactored into a new method resetAnalysts() to
        // be placed in some test file
        mActivityRule.getActivity().getService().setAnalyst(originalAnalyst);
    }

    @After
    public void release() {
        Intents.release();
    }

    private void cleanFakeUserHistory() {
        // Delete existing file
        initStorageManager().delete();
        PositionHistoryManager.deleteLocalProbabilityHistory();
        LocationService service = mActivityRule.getActivity().getService();

        ObservableCarrier newCarrier = new Layman(
                service.getAnalyst().getCarrier().getInfectionStatus(),
                service.getAnalyst().getCarrier().getIllnessProbability(),
                AuthenticationManager.getUserId()
        );

        service.setAnalyst(new ConcreteAnalysis(newCarrier, service.getReceiver()));
    }

    @Before
    public void resetHistoryBeforeTest() {
        // Clean in case there is something left from previous tests
        cleanFakeUserHistory();
    }

    @After
    public void resetHistoryAfterTest() {
        cleanFakeUserHistory();
    }

    @After
    public void stopLocationService() {
        mActivityRule.getActivity().stopService(new Intent(mActivityRule.getActivity(), LocationService.class));
    }

    private void startLocationServiceWithAlarm() {
        locaIntentWithAlarm = new Intent(mActivityRule.getActivity(), LocationService.class);
        locaIntentWithAlarm.putExtra(LocationService.ALARM_GOES_OFF, true);
        mActivityRule.getActivity().startService(locaIntentWithAlarm);
    }

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
        FakeDataExchanger fakeSender = new FakeDataExchanger();
        fakeSender.registerLocation(iAmBob, TestTools.newLoc(0, 0), now);
        mActivityRule.getActivity().getService().setSender(fakeSender);
        mActivityRule.getActivity().getService().setReceiver(fakeSender);
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
        FakeDataExchanger fakeSender = new FakeDataExchanger();
        fakeSender.registerLocation(iAmBob, TestTools.newLoc(1, 1), now);
        mActivityRule.getActivity().getService().setSender(fakeSender);
        mActivityRule.getActivity().getService().setReceiver(fakeSender);
        while (sentinel.get() == 0) {
        }

        assertThat(sentinel.get(), equalTo(1));

        restoreRealAnalyst();

        LocationService.setAlarmDelay(2000);
    }

    @Test
    public void carrierStatusIsStored() {

        LocationService service = mActivityRule.getActivity().getService();

        // Pass LocationService's carrier to FakeAnalyst
        InfectionAnalyst fakeAnalyst = new FakeAnalyst(service.getAnalyst().getCarrier());

        FakeDataExchanger fakeSender = new FakeDataExchanger();

        service.setAnalyst(fakeAnalyst);
        service.setSender(fakeSender);
        service.setReceiver(fakeSender);
        service.getAnalyst().getCarrier().addObserver(service);

        startLocationServiceWithAlarm();

        assertThat(fakeAnalyst.getCarrier().getIllnessProbability(), equalTo(0f));
        assertThat(fakeAnalyst.getCarrier().getInfectionStatus(), equalTo(HEALTHY));

        assertThat(fakeAnalyst.getCarrier().evolveInfection(new Date(), UNKNOWN, .3f), equalTo(true));
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

        FakeDataExchanger fakeSender = new FakeDataExchanger();

        serviceBefore.setSender(fakeSender);
        serviceBefore.setReceiver(fakeSender);

        // Modify carrier status
        assertThat(fakeAnalyst.getCarrier().evolveInfection(new Date(), UNKNOWN, .45f), equalTo(true));

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

        service.getAnalyst().getCarrier().deleteLocalProbabilityHistory();

        service.getAnalyst().getCarrier().setIllnessProbability(new Date(), .1f);
        TestTools.sleep();
        service.getAnalyst().getCarrier().setIllnessProbability(new Date(), .2f);
        TestTools.sleep();
        service.getAnalyst().getCarrier().evolveInfection(new Date(), INFECTED, .6f);
        TestTools.sleep();

        StorageManager<Date, Float> manager = initStorageManager();

        for (Float f : manager.read().values()) {
            Log.e("MNG", f.toString());
        }

        assertThat(manager.read().size(), equalTo(3));

        service.getAnalyst().getCarrier().deleteLocalProbabilityHistory();
    }
}
