package ch.epfl.sdp;

import android.Manifest;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.contamination.DataReceiver;
import ch.epfl.sdp.contamination.InfectionActivity;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.contamination.InfectionFragment;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.fragment.UserInfectionFragment;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.resetSickCounter;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.contamination.CachingDataSender.privateSickCounter;
import static ch.epfl.sdp.contamination.CachingDataSender.privateUserFolder;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.HEALTHY;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UserInfectionTest {
    private InfectionAnalyst analyst;
    private DataReceiver receiver;
    @Rule
    public final ActivityTestRule<UserInfectionActivity> activityRule =
            new ActivityTestRule<>(UserInfectionActivity.class);
    @Rule
    public GrantPermissionRule fingerprintPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.USE_FINGERPRINT);
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() {
        initSafeTest(activityRule, true);
        UserInfectionFragment fragment = ((UserInfectionFragment)((UserInfectionActivity)(getActivity())).getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        analyst =  fragment.getLocationService().getAnalyst();
        receiver = fragment.getLocationService().getReceiver();

    }
    @After
    public void release(){
        Intents.release();
    }

    @Test
    @Ignore("Confirmation isn't visible in new UI")
    public void testDataUpload() {
        TestTools.clickAndCheck(R.id.infectionStatusButton,
                R.id.infectionStatusUploadConfirmation);
        TestTools.clickAndCheck(R.id.infectionStatusButton,
                R.id.infectionStatusUploadConfirmation);
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
    public void sendsNotificationToFirebaseAndAnalystOnRecovery(){
        analyst.updateStatus(HEALTHY);
        IS_NETWORK_DEBUG = false;
        IS_ONLINE = true;
        resetSickCounter();
        sleep(3000);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(5000);
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(5000);

        receiver.getSicknessCounter(User.DEFAULT_USERID,res -> {
            assertFalse(((Map)(res)).isEmpty());
            assertEquals(1l,((Map)(res)).get(privateSickCounter));

        });
        sleep(5000);
        assertSame(HEALTHY,analyst.getCarrier().getInfectionStatus());
    }

    @Test
    public void sendsNotificationToAnalystOnInfection(){
        analyst.updateStatus(HEALTHY);
        IS_NETWORK_DEBUG = false;
        IS_ONLINE = true;
        onView(withId(R.id.infectionStatusButton)).perform(click());
        sleep(5000);
        assertSame(Carrier.InfectionStatus.INFECTED,analyst.getCarrier().getInfectionStatus());
    }

}
