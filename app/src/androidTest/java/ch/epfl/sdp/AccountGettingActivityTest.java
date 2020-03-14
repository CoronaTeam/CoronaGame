package ch.epfl.sdp;


import android.app.Activity;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;

public class AccountGettingActivityTest {

   // @Rule
  //  public final ActivityTestRule<AccountGettingActivity> mActivityRule = new ActivityTestRule<AccountGettingActivity>(AccountGettingActivity.class);
    @Rule
    public final IntentsTestRule<AccountGettingActivity> intentsTestRule =
            new IntentsTestRule<>(AccountGettingActivity.class);

//    @Test
//    public void nameIsDisplayed(){
//        onView(withId(R.id.name)).check(matches(withText(User.DEFAULT_DISPLAY_NAME)));
//    }
    @Test
    public void lastNameIsDisplayed(){
        onView(withId(R.id.lastName)).check(matches(withText(User.DEFAULT_FAMILY_NAME)));
    }
    @Test
    public void emailIsDisplayed(){
        onView(withId(R.id.email)).check(matches(withText(User.DEFAULT_EMAIL)));
    }
    @Test
    public void userIdViewIsDisplayed(){
        onView(withId(R.id.userIdView)).check(matches(withText(User.DEFAULT_USERID)));
    }

  /*  @Test
    public void imageViewDoDisplayImage(){
        //onView(withId(R.id.imageView)).check(matches(withDrawable(new DrawableMatcher(User.default_uri))));
        ImageView contentImage = getActivity().findViewById(R.id.profileImage);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNotNull(contentImage.getDrawable());  //checking that the image is not null is sufficient
    }*/



//    @Test
//    public void signOutButtonWorks(){
//        onView(withId(R.id.button_sign_out)).perform(click());
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        intended(hasComponent(AuthenticationActivity.class.getName()));//.class.getName()
//      //  assertSame(getActivity().getClass(),AuthenticationActivity.class);
//    }

}
