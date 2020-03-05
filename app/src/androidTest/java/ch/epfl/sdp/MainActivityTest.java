package ch.epfl.sdp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    @Rule
    public final ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);
    @Test
    public void dummyTest() {

        onView(withId(R.id.userIDText)).perform(typeText("from my unit test")).perform(closeSoftKeyboard());
        //onView(withId(R.id.launchButton)).perform(click());
        // onView(withId(R.id.greetingMessage)).check(matches(withText("Hello from my unit test!")));
    }
}