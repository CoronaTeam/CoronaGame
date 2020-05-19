
package ch.epfl.sdp;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;

import androidx.test.espresso.intent.Intents;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Map;

import ch.epfl.sdp.contamination.databaseIO.ConcreteCachingDataSender;
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.identity.AuthenticationManager;
import ch.epfl.sdp.identity.User;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.privateRecoveryCounter;
import static ch.epfl.sdp.firestore.FirestoreLabels.privateUserFolder;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicAlertAttribute;

public interface TestTools {
    /**
     * This method prevents a double init call
     *
     * @param activityTestRule : activity to launch
     * @param <E>
     */
    static <E extends Activity> void initSafeTest(ActivityTestRule<E> activityTestRule, boolean launchActivity) throws IllegalStateException {
        AccountFragment.IN_TEST = true;
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
     * @param
     * @return
     */


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

    /**
     * Reset the correct status of LocationService
     * @param service
     */
    static void resetLocationServiceStatus(LocationService service) {
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();
        service.setReceiver(new ConcreteDataReceiver(gridInteractor));
        service.setSender(new ConcreteCachingDataSender(gridInteractor));
    }

    static void resetSickCounter(){
        DocumentReference ref = documentReference(privateUserFolder, User.DEFAULT_USERID);
        ref.update(privateRecoveryCounter, FieldValue.delete());
    }
    static void clickBack(){
        clickBack(1000);
    }

    /**
     * Will click on the back button of the phone and wait before and after
     * @param waitTime
     */
    static void clickBack(int waitTime){
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        sleep(waitTime);
        mDevice.pressBack();
        sleep(waitTime);
    }
}
