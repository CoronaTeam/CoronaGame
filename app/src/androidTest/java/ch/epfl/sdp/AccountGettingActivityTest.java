package ch.epfl.sdp;


import android.app.Activity;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;

public class AccountGettingActivityTest {

    @Rule
    public final ActivityTestRule<AccountGettingActivity> mActivityRule = new ActivityTestRule<AccountGettingActivity>(AccountGettingActivity.class);

    @Before
    public void unlockScreen() {
        //CategorySelectionActivity
        final  Activity activity = mActivityRule.getActivity();
        Runnable wakeUpDevice = () -> activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.runOnUiThread(wakeUpDevice);
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



    @Test
    public void signOutButtonWorks(){
        onView(withId(R.id.button_sign_out)).perform(click());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertSame(getActivity().getClass(),AuthenticationActivity.class);
    }
    //found on the internet for getting activities
    public static Activity getActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    Activity activity = (Activity) activityField.get(activityRecord);
                    return activity;
                }
            }
        }catch(Exception e){
            return null;    //there should not be any exception, if so, try another way for getting the activity.
        }
        return null;
    }
}
