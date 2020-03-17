package ch.epfl.sdp;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertTrue;

//@RunWith(JUnitPlatform.class)
public class AuthenticationTest {
    private Authentication activAuth;
    @Rule
    public IntentsTestRule<Authentication> activityRule = new IntentsTestRule<Authentication>(Authentication.class);
//    @Before
//    public void setUp() throws Exception{
//        Intents.init();
//        activityRule.launchActivity(new Intent());
//    }
    @Test(expected = Test.None.class) //expect no error
    public void signInButtonIsDisplayedAndClickable(){
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
        onView(withId(R.id.sign_in_button)).perform(click());

    }

    @Test(expected = IllegalStateException.class)
    public void onActivityResultThrowsExceptionOnWrongRequestCode(){
        ((Authentication )AccountGettingTest.getActivity()).onActivityResult(Authentication.RC_SIGN_IN -1 ,0,null);
      //  Assert.assertThrows(IllegalStateException.class,()->{activAuth.onActivityResult(-1,-1,null)});
    }
    @Test(expected = Test.None.class)
    public void onActivityResultThrowsNoExceptionOnRightRequestCode(){
        ((Authentication )AccountGettingTest.getActivity()).onActivityResult(Authentication.RC_SIGN_IN  ,0,null);
        //  Assert.assertThrows(IllegalStateException.class,()->{activAuth.onActivityResult(-1,-1,null)});
    }
    @Test
    public void signInButtonIsVisibleWhenAccountIsNull(){
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
//        assertTrue(activAuth.findViewById(R.id.sign_in_button).getVisibility() == View.VISIBLE);
    }
//    @After
//    public void tearDown() throws Exception{
//        Intents.release();
//    }
}
