package ch.epfl.sdp;

import android.Manifest;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.TestTools.initSafeTest;

public class UserInfectionTest {

    @Rule
    public final ActivityTestRule<UserInfectionActivity> activityRule =
            new ActivityTestRule<>(UserInfectionActivity.class);
    @Rule
    public GrantPermissionRule fingerprintPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.USE_FINGERPRINT);
    @Rule
    public ActivityScenarioRule<UserInfectionActivity> rule =
            new ActivityScenarioRule<>(UserInfectionActivity.class);
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() {
        initSafeTest(activityRule, true);
    }

    @Test
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
        TestTools.sleep(5000);
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
}
