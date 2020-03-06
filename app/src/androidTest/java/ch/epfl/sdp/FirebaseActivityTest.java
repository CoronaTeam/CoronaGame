package ch.epfl.sdp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.espresso.Espresso;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Array;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;


public class FirebaseActivityTest {

    private NetworkStatsManager ni;

    @Rule
    public GrantPermissionRule internetPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.INTERNET);

    @Rule
    public final ActivityTestRule<FirebaseActivity> mActivityRule =
            new ActivityTestRule<>(FirebaseActivity.class);

    @Before
    public void setupMockFirebase(){
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("appops set ch.epfl.sdp.test android:mock_internet allow");
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("appops set ch.epfl.sdp android:mock_internet allow");

        String internetPermission = "android.permission.ACCESS_INTERNET";
        if (mActivityRule.getActivity().getBaseContext().checkSelfPermission(internetPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ABORT", "Aborting test -- not having internet permission!!!!");
        }





    }

    @Test
    public void testDetectNoInternetConnection() {
    }

    @Test
    public void testDataDownloadIsDisplayed() {

    }

    @Test
    public void testHandleDataDownloadWithNoInternetConnection() {

    }

    @Test
    public void testDataIsCorrectlyUploaded() {

    }

    @Test
    public void testDataUploadIsDisplayed() {
        onView(withId(R.id.FirebaseUploadButton)).perform(click());
        onView(withId(R.id.FirebaseUploadConfirmation)).check(matches(withText("DocumentSnapshot successfully added")));
    }
}
