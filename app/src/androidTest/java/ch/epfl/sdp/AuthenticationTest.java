package ch.epfl.sdp;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Rule;
import org.junit.Test;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.getActivity;

public class AuthenticationTest {
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
        ((Authentication )getActivity()).onActivityResult(Authentication.RC_SIGN_IN -1 ,0,null);
      //  Assert.assertThrows(IllegalStateException.class,()->{activAuth.onActivityResult(-1,-1,null)});
    }
    @Test(expected = Test.None.class)
    public void onActivityResultThrowsNoExceptionOnRightRequestCode(){
        ((Authentication )getActivity()).onActivityResult(Authentication.RC_SIGN_IN  ,0,null);
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
