package ch.epfl.sdp;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class IntroActivityTest {
    @Rule
    public final ActivityTestRule<IntroActivity> activityRule = new ActivityTestRule<IntroActivity>(IntroActivity.class);
    @Before
    public void setUp() throws Exception{
        initSafeTest(activityRule,true);
    }

    private static final int N_SLIDES = 3; // number of slides in Intro screen

    @Test
    public void testCanNavigateToMainScreen() {

        for (int i = 0; i < N_SLIDES - 1; ++i)
            onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.done)).perform(click());

        intended(hasComponent(MainActivity.class.getName()));

    }
    @After
    public void tearDown() throws Exception{
        Intents.release();
    }
}