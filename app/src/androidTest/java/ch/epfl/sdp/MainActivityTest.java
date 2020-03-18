package ch.epfl.sdp;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {


    @Rule
    public final ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testCanGoToFirebaseActivity() {
        clickAndCheck(R.id.button_firebase, R.id.FirebaseTextView);
    }

    @Test
    public void testCanGoToGPSActivity() {
        clickAndCheck(R.id.button_gps, R.id.gpsLatitude);
    }


    @Test
    @Ignore
    public void dummyTest() {
        System.out.println("hello");
        //onView(withId(R.id.userIDText)).perform(typeText("from my unit test")).perform(closeSoftKeyboard());
        onView(withId(R.id.button_map)).perform(click());
        Espresso.pressBack();
        // onView(withId(R.id.greetingMessage)).check(matches(withText("Hello from my unit test!")));
    }

    private void clickAndCheck(int buttonID, int UIelementID){
        onView(withId(buttonID)).perform(click());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(UIelementID)).check(matches(isDisplayed()));
    }

}