package ch.epfl.sdp;

import android.content.Intent;

import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class AuthenticationActivityTest {
    @Rule
    public ActivityTestRule<Authentication> activityRule = new ActivityTestRule<Authentication>(Authentication.class, true, false);
    @Before
    public void setUp() throws Exception{
        Intents.init();
        activityRule.launchActivity(new Intent());
    }
    @Test(expected = Test.None.class) //expect no error
    public void signInButtonIsDisplayedAndClickable(){

        onView(withId(R.id.sign_in_button)).perform(click());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @After
    public void tearDown() throws Exception{
        Intents.release();
    }
}
