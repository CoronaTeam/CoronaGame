package ch.epfl.sdp;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static androidx.test.espresso.Espresso.onIdle;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;


public class FirebaseActivityTest {

    private ConnectivityManager cm;
    public FirebaseFirestore mockFF = Mockito.mock(FirebaseFirestore.class);


    @Rule
    public GrantPermissionRule internetPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.INTERNET);

    @Rule
    public final ActivityTestRule<FirebaseActivity> mActivityRule =
            new ActivityTestRule<>(FirebaseActivity.class);


    @Before
    public void setup() {
        String internetPermission = "android.permission.ACCESS_INTERNET";
        if (ContextCompat.checkSelfPermission(mActivityRule.getActivity().getBaseContext(),
                internetPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ABORT", "Aborting test -- not having internet permission!!!!");
        }


    }

    @Test
    public void testDataDownloadIsDisplayed() {
        clickWaitAndCheckText(R.id.FirebaseDownloadButton,
                R.id.FirebaseDownloadResult,
                "User#000 => {Position=GeoPoint { latitude=0.0, longitude=0.0 }, Time=Timestamp(seconds=1583276400, nanoseconds=0)}",
                10000);
    }

    @Test
    public void testDataUploadIsDisplayed() {
        clickWaitAndCheckText(R.id.FirebaseUploadButton,
                R.id.FirebaseUploadConfirmation,
                "Document snapshot successfully added to firestore.",
                5000);
    }

    @Test
    public void testDetectNoInternetConnectionWhenUpload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        clickWaitAndCheckText(R.id.FirebaseUploadButton,
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
        clickWaitAndCheckText(R.id.FirebaseDownloadButton,
                R.id.FirebaseDownloadResult,
                "Can't download while offline",
                0);
        IS_NETWORK_DEBUG = false;
    }

    @Test
    public void testHandleUploadDatabaseError() {
        Mockito.when(mockFF.collection("players").add(null)).thenAnswer(new Answer<String>(){
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception();
            }
        });
    }

    @Test
    public void testHandleDownloadDatabaseError() {

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
