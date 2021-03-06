package ch.epfl.sdp.contamination;

import android.Manifest;
import android.content.Context;
import android.location.Location;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.TestTools;
import ch.epfl.sdp.contamination.databaseIO.DataReceiver;
import ch.epfl.sdp.contamination.fragment.UserInfectionFragment;
import ch.epfl.sdp.identity.User;
import ch.epfl.sdp.testActivities.UserInfectionActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.CoronaGame.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.CoronaGame.IS_ONLINE;
import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.resetSickCounter;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.firestore.FirestoreLabels.privateRecoveryCounter;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

public class UserInfectionTest {
    private static ObservableCarrier me;
    @Rule
    public final ActivityTestRule<UserInfectionActivity> activityRule =
            new ActivityTestRule<>(UserInfectionActivity.class);
    @Rule
    public GrantPermissionRule fingerprintPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.USE_FINGERPRINT);
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);
    private InfectionAnalyst analyst;
    private DataReceiver receiver;
    private UserInfectionFragment fragment;

    @Before
    public void setUp() {
        initSafeTest(activityRule, true);
        sleep(1001);
        fragment = ((UserInfectionFragment) ((UserInfectionActivity) TestTools.getActivity()).getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        sleep(1000);
        me = new Layman(HEALTHY);
        analyst = new InfectionAnalyst() {

            @Override
            public CompletableFuture<Integer> updateInfectionPredictions(Location location, Date startTime, Date endTime) {
                return null;
            }

            @Override
            public ObservableCarrier getCarrier() {
                return me;
            }
        };
        fragment.getLocationService().setAnalyst(analyst);
        receiver = fragment.getLocationService().getReceiver();
        resetLastChangeDate();

        sleep(1000);

    }

    @After
    public void resetAnalyst() {
        fragment.getLocationService().setAnalyst(
                new ConcreteAnalysis(me, fragment.getLocationService().getReceiver(), fragment.getLocationService().getSender()));
    }

    @After
    public void release() {
        Intents.release();
        analyst = null;
        receiver = null;
    }

    @Test
    public void testDetectNoInternet() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(5000);
        onView(withId(R.id.onlineStatusView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
        onView(withId(R.id.refreshButton)).perform(click());
        onView(withId(R.id.infectionStatusButton)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

    @Test
    public void testDetectInternet() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = true;
        onView(withId(R.id.onlineStatusView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));
        IS_NETWORK_DEBUG = false;
    }

    @Test
    public void sendsNotificationToFirebaseAndAnalystOnRecovery() {
        setIllnessToHealthy();
        analyst.getCarrier().evolveInfection(new Date(), HEALTHY, 0f);
//        IS_NETWORK_DEBUG = false;
//        IS_ONLINE = true;
        sleep(1000);
        resetSickCounter();
        sleep(3500);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        resetLastChangeDate();

        sleep(5000);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(5000);
        receiver.getRecoveryCounter(User.DEFAULT_USERID).thenAccept(res -> {
            assertFalse(res.isEmpty());
            assertEquals(1L, res.get(privateRecoveryCounter));
            assertSame(HEALTHY, analyst.getCarrier().getInfectionStatus());
        });
        sleep(2000);

    }

    private void resetLastChangeDate() {
        /*fragment.*/
        getActivity().getSharedPreferences("UserInfectionPrefFile", Context.MODE_PRIVATE)
                .edit().putLong("lastStatusChange", 0).apply();
    }

    private void setIllnessToHealthy() {
        sleep(5000);
        if (fragment.isImmediatelyNowIll()) {
            onView(withId(R.id.infectionStatusButton)).perform(click());

        }
        resetLastChangeDate();
    }

    @Test
    public void sendsNotificationToAnalystOnInfection() {
        setIllnessToHealthy();
        analyst.getCarrier().evolveInfection(new Date(), HEALTHY, 0f);
        sleep(2000);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(2000);
        assertSame(Carrier.InfectionStatus.INFECTED, analyst.getCarrier().getInfectionStatus());
    }

    @Test
    public void checkStatusChangeRateLimit() {
        setIllnessToHealthy();
        Carrier.InfectionStatus initial = analyst.getCarrier().getInfectionStatus();
        sleep(1000);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(1000);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(1000);
        assertNotSame(initial, analyst.getCarrier().getInfectionStatus());
    }
}
