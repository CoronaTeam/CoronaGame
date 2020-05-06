package ch.epfl.sdp;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.firestore.FirebaseActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;


public class FirebaseActivityTest {

    @Rule
    public final ActivityTestRule<FirebaseActivity> mActivityRule =
            new ActivityTestRule<>(FirebaseActivity.class);
    @Rule
    public GrantPermissionRule internetPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.INTERNET);
    private ConnectivityManager cm;

    @Before
    public void setUp() {
        String internetPermission = "android.permission.ACCESS_INTERNET";
        if (ContextCompat.checkSelfPermission(mActivityRule.getActivity().getBaseContext(),
                internetPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ABORT", "Aborting test -- not having internet permission!!!!");
        }
        //EspressoIdling res: https://developer.android.com/reference/androidx/test/espresso/idling/CountingIdlingResource
    }

    @Test
    public void testDataDownloadIsDisplayed1() {
        clickWaitAndCheckText(R.id.FirebaseDownloadButton1,
                R.id.FirebaseDownloadResult,
                "{DownloadTest={value=success}}",
                30000);
    }

    @Test
    public void testDataDownloadIsDisplayed2() {
        clickWaitAndCheckText(R.id.FirebaseDownloadButton2,
                R.id.FirebaseDownloadResult,
                "{value=success}",
                30000);
    }

    @Test
    public void testDataUploadIsDisplayed1() {
        clickWaitAndCheckText(R.id.FirebaseUploadButton1,
                R.id.FirebaseUploadConfirmation,
                "Document snapshot successfully added to firestore.",
                5000);
    }

    @Test
    public void testDataUploadIsDisplayed2() {
        clickWaitAndCheckText(R.id.FirebaseUploadButton2,
                R.id.FirebaseUploadConfirmation,
                "Document snapshot successfully added to firestore.",
                5000);
    }

    @Test
    public void testDetectNoInternetConnectionWhenUpload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        clickWaitAndCheckText(R.id.FirebaseUploadButton2,
                R.id.FirebaseUploadConfirmation,
                "Can't upload while offline",
                0);
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }

    @Test
    public void testDetectNoInternetConnectionWhenDownload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        clickWaitAndCheckText(R.id.FirebaseDownloadButton1,
                R.id.FirebaseDownloadResult,
                "Can't download while offline",
                0);
        IS_NETWORK_DEBUG = false;
    }

    @After
    public void restoreOnline() {
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }

    private void clickWaitAndCheckText(int buttonID, int textID, String expectedText, int waitingTime) {
        onView(withId(buttonID)).perform(click());
        waitingForTravis(waitingTime);
        onView(withId(textID)).check(matches(withText(expectedText)));
    }

    private void waitingForTravis(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
