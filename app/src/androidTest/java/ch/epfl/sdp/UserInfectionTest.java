package ch.epfl.sdp;

import android.Manifest;

import androidx.biometric.BiometricFragment;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
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
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertTrue;

public class UserInfectionTest {

    @Rule
    public ActivityScenarioRule<UserInfectionActivity> rule =
            new ActivityScenarioRule<>(UserInfectionActivity.class);

    @Rule
    public final ActivityTestRule<UserInfectionActivity> mActivityRule =
            new ActivityTestRule<>(UserInfectionActivity.class);

    @Rule
    public GrantPermissionRule fingerprintPermissionRule =
            GrantPermissionRule.grant(Manifest.permission.USE_FINGERPRINT);

    private ActivityScenario<UserInfectionActivity> scenario;

    @Before
    public void setUp() {
        scenario = rule.getScenario();
    }

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void canAuthenticateWithPermissions() {
        assertTrue(BiometricUtils.canAuthenticate(mActivityRule.getActivity().getBaseContext()));
    }

    @Test
    @Ignore("Modify test to include biometric")
    public void changeViewContentWhenClick() {
        // click for the first time changes view from default to infected status
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am cured", "Your user status is set to infected.", 5000);
        // click again changes view from infected status to cured status
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am infected", "Your user status is set to not infected.", 5000);
    }

    @Test
    public void clickOnButtonShowsDialog(){
        onView(withId(R.id.infectionStatusButton)).perform(click());
        waitingForTravis(2000);
        onView(withText("Biometric"))
                .inRoot(isFocusable()) // <---
                .check(matches(isDisplayed()));
    }

    @Test
    @Ignore("Modify test to include biometric")
    public void keepLastInfectionStatusWhenRestartingApp() {
        ActivityScenario<UserInfectionActivity> launchedActivity = scenario.launch(UserInfectionActivity.class);
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am cured", "Your user status is set to infected.", 5000);
        launchedActivity.recreate();
        onView(withId(R.id.infectionStatusView)).check(matches(withText("Your user status is set to infected.")));
        onView(withId(R.id.infectionStatusButton)).check(matches(withText("I am cured")));
    }

    private void clickWaitAndCheckTexts(int buttonID, int textID, String expectedButtonText, String expectedText, int waitingTime) {
        onView(withId(buttonID)).perform(click());
        //onView(withId())
        waitingForTravis(waitingTime);

        onView(withId(textID)).check(matches(withText(expectedText)));
        onView(withId(buttonID)).check(matches(withText(expectedButtonText)));
    }

    private void waitingForTravis(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}
