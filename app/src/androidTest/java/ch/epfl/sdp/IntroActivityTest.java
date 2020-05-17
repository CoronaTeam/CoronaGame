package ch.epfl.sdp;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.initSafeTest;

@RunWith(AndroidJUnit4.class)
public class IntroActivityTest {
    private static final int N_SLIDES = 4; // number of slides in Intro screen
    @Rule
    public final ActivityTestRule<IntroActivity> activityRule = new ActivityTestRule<>(IntroActivity.class);
    @Before
    public void setUp() throws Exception {
        initSafeTest(activityRule, true);
    }

    @Test
    public void testCanNavigateToSignIn() {

        for (int i = 0; i < N_SLIDES; ++i)
            onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));

    }

    @After
    public void tearDown() throws Exception {
        Intents.release();
    }
}
