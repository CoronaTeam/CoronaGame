package ch.epfl.sdp;

import android.Manifest;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static junit.framework.TestCase.assertTrue;

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
    public void setUp() throws Exception {
        initSafeTest(activityRule, true);
    }

    @Test
    public void changeViewContentWhenClick() {
        // click for the first time changes view from default to infected status
        waitingForTravis(5000);
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am cured", "Your user status is set to infected.", 5000);
        // click again changes view from infected status to cured status
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am infected", "Your user status is set to not infected.", 5000);
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
        waitingForTravis(5000);
        onView(withId(R.id.onlineStatusView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
        onView(withId(R.id.refreshButton)).perform(click());
    }

    @Test
    public void testDetectInternet() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = true;
        onView(withId(R.id.onlineStatusView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)));
        IS_NETWORK_DEBUG = false;
    }

    private void clickWaitAndCheckTexts(int buttonID, int textID, String expectedButtonText, String expectedText, int waitingTime) {
        onView(withId(buttonID)).perform(click());
        //onView(withId())
        waitingForTravis(waitingTime);

        onView(withId(textID)).check(matches(withText(expectedText)));
        onView(withId(buttonID)).check(matches(withText(expectedButtonText)));
    }

    @Test
    @Ignore("Not the right way")
    public void keepLastInfectionStatusWhenRestartingApp() {
        ActivityScenario<UserInfectionActivity> launchedActivity =
                ActivityScenario.launch(UserInfectionActivity.class);
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am cured", "Your user status is set to infected.", 5000);
        launchedActivity.recreate();
        onView(withId(R.id.infectionStatusView)).check(matches(withText("Your user status is set to infected.")));
        onView(withId(R.id.infectionStatusButton)).check(matches(withText("I am cured")));
    }

    @Test
    @Ignore("Good luck setting up a fingerprint on Cirrus")
    public void canAuthenticateWithBiometric() {
        assertTrue(BiometricUtils.canAuthenticate(activityRule.getActivity().getBaseContext()));
    }

    private void waitingForTravis(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
