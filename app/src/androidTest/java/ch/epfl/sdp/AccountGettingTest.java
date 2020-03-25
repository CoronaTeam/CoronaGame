package ch.epfl.sdp;

import android.widget.ImageView;

import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
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
    @Rule
    public final ActivityTestRule<AccountGetting> activityRule = new ActivityTestRule<AccountGetting>(AccountGetting.class);
    @Before
    public void setUp() throws Exception{
        initSafeTest(activityRule,true);
    }
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
        ImageView contentImage = getActivity().findViewById(R.id.profileImage);
        sleep();
        assertNotNull(contentImage.getDrawable());  //checking that the image is not null is sufficient
    }
    @Test
    public void signOutWorks(){
        ((AccountGetting)(getActivity())).signOut(null);
        sleep();
        assertSame(getActivity().getClass(),Authentication.class);
    }

    @Test
    public void signOutButtonWorks(){
        clickAndCheck(R.id.button_sign_out,R.id.sign_in_button);
    }
    @After
    public void tearDown() throws Exception{
        Intents.release();
    }

}
