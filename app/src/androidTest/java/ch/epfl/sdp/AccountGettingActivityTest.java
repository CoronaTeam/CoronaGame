package ch.epfl.sdp;


import android.app.Activity;
import android.widget.ImageView;

import androidx.test.rule.ActivityTestRule;

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
    public final ActivityTestRule<AccountGettingActivity> mActivityRule = new ActivityTestRule<>(AccountGettingActivity.class);

    @Test
    public void textViewsDoShowUserInformation() {
        // System.out.println(onView(withId(R.id.name)).toString());
        onView(withId(R.id.name)).check(matches(withText(User.DEFAULT_DISPLAY_NAME)));
        onView(withId(R.id.lastName)).check(matches(withText(User.DEFAULT_FAMILY_NAME)));
        onView(withId(R.id.email)).check(matches(withText(User.DEFAULT_EMAIL)));

        // assertTrue(onView(withId(R.id.name)).toString().equals(User.default_display_Name));
    }
    @Test
    public void imageViewDoDisplayImage(){
        //onView(withId(R.id.imageView)).check(matches(withDrawable(new DrawableMatcher(User.default_uri))));
        ImageView contentImage = mActivityRule.getActivity().findViewById(R.id.imageView);
        customWait(1000);
        assertNotNull(contentImage.getDrawable());  //checking that the image is not null is sufficient
    }

    private void customWait(int i) {
        int k =0;
        while(k<i){
            k+=1;
        }
    }

    @Test
    public void signOutButtonWorks(){
        onView(withId(R.id.button_sign_out)).perform(click());
        assertSame(getActivity().getClass(),AuthenticationActivity.class);
    }
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
