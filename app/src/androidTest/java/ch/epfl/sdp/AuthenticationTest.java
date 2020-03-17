package ch.epfl.sdp;

import android.app.Activity;
import android.content.Intent;

import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
//@RunWith(JUnitPlatform.class)
public class AuthenticationTest {
    private Authentication activAuth;
    @Rule
    public ActivityTestRule<Authentication> activityRule = new ActivityTestRule<Authentication>(Authentication.class, true, false);
    @Before
    public void setUp() throws Exception{
        Intents.init();
        activAuth = activityRule.launchActivity(new Intent());
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

    @Test(expected = IllegalStateException.class)
    public void onActivityResultThrowsExceptionOnWrongRequestCode(){
        activAuth.onActivityResult(Authentication.RC_SIGN_IN -1 ,0,null);
      //  Assert.assertThrows(IllegalStateException.class,()->{activAuth.onActivityResult(-1,-1,null)});
    }
    @After
    public void tearDown() throws Exception{
        Intents.release();
    }
}
