
package ch.epfl.sdp;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;

import androidx.fragment.app.Fragment;
import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Map;

import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.fragment.UserInfectionFragment;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.contamination.CachingDataSender.privateRecoveryCounter;
import static ch.epfl.sdp.contamination.CachingDataSender.privateUserFolder;
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
        return AuthenticationManager.getActivity();
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
    static void resetSickCounter(){
        DocumentReference ref = FirestoreInteractor.documentReference(privateUserFolder,User.DEFAULT_USERID);
        ref.update(privateRecoveryCounter, FieldValue.delete());
    }
    static void clickBack(){
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        sleep(1000);
        mDevice.pressBack();
        sleep(1000);
    }
}
