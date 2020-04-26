package ch.epfl.sdp;

import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import ch.epfl.sdp.fragment.AuthenticationFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.sleep;

public class AuthenticationTest {
    @Rule
    public final ActivityTestRule<Authentication> activityRule = new ActivityTestRule<>(Authentication.class);
    @Before
    public void setUp() throws Exception {
        initSafeTest(activityRule, true);
        AuthenticationManager.signOut(activityRule.getActivity()); // fixes Auth skip to TabActivity
    }

    @Test(expected = Test.None.class) //expect no error
    public void signInButtonIsDisplayedAndClickable() {
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_button)).perform(click());
        sleep(10000);
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressBack(); // closes the google popup for signing in
    }

    @Test(expected = IllegalStateException.class)
    public void onActivityResultThrowsExceptionOnWrongRequestCode() {
        activityRule.getActivity().getFragment().onActivityResult(AuthenticationFragment.RC_SIGN_IN - 1, 0, null);
    }

    @Test(expected = Test.None.class)
    public void onActivityResultThrowsNoExceptionOnRightRequestCode() {
        activityRule.getActivity().getFragment().onActivityResult(AuthenticationFragment.RC_SIGN_IN, 0, null);
    }

    @Test @Ignore
    public void signInButtonIsVisibleWhenAccountIsNull() {
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
    }

    @After
    public void tearDown() throws Exception {
        Intents.release();
    }
}
