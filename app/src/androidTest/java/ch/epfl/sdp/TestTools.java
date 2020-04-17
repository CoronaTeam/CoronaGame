package ch.epfl.sdp;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;

import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.contamination.CachingDataSender.publicAlertAttribute;

public interface TestTools {
    /**
     * This method prevents a double init call
     *
     * @param activityTestRule : activity to launch
     * @param <E>
     */
    static <E extends Activity> void initSafeTest(ActivityTestRule<E> activityTestRule, Boolean launchActivity) throws IllegalStateException {
        try {
            Intents.init();
        } catch (IllegalStateException alreadyBeenInit) {
            Intents.release();
            Intents.init();
        } finally {
            if (launchActivity) {
                activityTestRule.launchActivity(new Intent());
            }
        }
    }

    /*
        This method was found on the internet for getting the current activity
     */
    static Activity getActivity() {
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
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (Exception e) {
            return null;    //there should not be any exception, if so, try another way for getting the activity.
        }
        return null;
    }

    static void clickAndCheck(int buttonID, int UIelementID) {
        onView(withId(buttonID)).perform(click());
        sleep();
        onView(withId(UIelementID)).check(matches(isDisplayed()));
    }

    static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     By default, the waiting time is set to 2 seconds.
     */
    static void sleep() {
        sleep(2000);
    }
    /**
     * Rounds a double to 5 digits after the comma
     * @param coor
     * @return
     */
    static double roundCoordinate(double coor){
        return (double)Math.round(coor * 100000d) / 100000d;//fast rounding to 5 digits
    }

    /**
     * Rounds a location to 5 digits after the comma
     * @param l
     * @return
     */
    static Location roundLocation(Location l){
        if(l == null){
            throw new IllegalArgumentException("Location can't be null");
        }
        double latitude = l.getLatitude();
        double longitude = l.getLongitude();
        latitude = roundCoordinate(latitude);
        longitude = roundCoordinate(longitude);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        return l;
    }
    static Location newLoc(double lati,double longi){
        Location res =  new Location("provider");
        res.reset();
        res.setLatitude(lati);
        res.setLongitude(longi);
        return res;
    }
    static boolean expandedLocEquals(Location loc1, Location loc2){
        return loc1.getLatitude() == loc2.getLatitude() && loc1.getLongitude() == loc2.getLongitude();
    }

    /**
     * Use with parcymony !
     * @param res
     * @return
     */
    static float getMapValue(Object res){
        return  ((float) (((Map) (res)).get(publicAlertAttribute)));
    }
}
