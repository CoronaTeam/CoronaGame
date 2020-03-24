package ch.epfl.sdp;

import android.widget.ImageView;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertSame;
import static ch.epfl.sdp.TestTools.*;
public class AccountGettingTest {
   // @Rule
  //  public final ActivityTestRule<AccountGettingActivity> mActivityRule = new ActivityTestRule<AccountGettingActivity>(AccountGettingActivity.class);
//    @Rule
//    public final IntentsTestRule<AccountGettingActivity> intentsTestRule =
//            new IntentsTestRule<>(AccountGettingActivity.class);
    @Rule
    public IntentsTestRule<AccountGetting> activityRule = new IntentsTestRule<>(AccountGetting.class);


//    @Before
//    public void setUp() throws Exception{
//        Intents.init();
//        activityRule.launchActivity(new Intent());
//    }
    @Test
    public void nameIsDisplayed(){
        onView(withId(R.id.name)).check(matches(withText(User.DEFAULT_DISPLAY_NAME)));
    }
    @Test
    public void lastNameIsDisplayed(){
        onView(withId(R.id.lastName)).check(matches(withText(User.DEFAULT_FAMILY_NAME)));
    }
    @Test
    public void emailIsDisplayed(){
        onView(withId(R.id.email)).check(matches(withText(User.DEFAULT_EMAIL)));
    }
    @Test
    public void userIdViewIsDisplayed() {
        onView(withId(R.id.userIdView)).check(matches(withText(User.DEFAULT_USERID)));
    }

    @Test
    public void imageViewDoDisplayImage(){
        //onView(withId(R.id.imageView)).check(matches(withDrawable(new DrawableMatcher(User.default_uri))));
        ImageView contentImage = getActivity().findViewById(R.id.profileImage);
        sleep();
        assertNotNull(contentImage.getDrawable());  //checking that the image is not null is sufficient
    }
    @Test
    public void signOutWorks(){
        ((AccountGetting)(getActivity())).signOut(null);
        sleep();
        assertSame(getActivity().getClass(),Authentication.class);
//        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
    }



    @Test
    public void signOutButtonWorks(){
        clickAndCheck(R.id.button_sign_out,R.id.sign_in_button);
    }
//    @After
//    public void tearDown() throws Exception{
//        Intents.release();
//    }

}
