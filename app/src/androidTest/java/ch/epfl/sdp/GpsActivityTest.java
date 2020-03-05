package ch.epfl.sdp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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

import java.lang.reflect.Array;
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

public class GpsActivityTest {

    private LocationManager lm;

    @Rule
    public final ActivityTestRule<GpsActivity> mActivityRule =
            new ActivityTestRule<>(GpsActivity.class);

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setupMockLocation()  {

        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("appops set ch.epfl.sdp.test android:mock_location allow");
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("appops set ch.epfl.sdp android:mock_location allow");


        String locationPermission = "android.permission.ACCESS_FINE_LOCATION";
        if (mActivityRule.getActivity().getBaseContext().checkSelfPermission(locationPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ABORT", "Aborting test -- not having permission!!!!");
            onView(withText(startsWith("Yes"))).perform(click());
            onView(withText("ALLOW")).perform(click());
        }

        lm = (LocationManager) mActivityRule.getActivity().getSystemService(Context.LOCATION_SERVICE);
        assert lm != null;

        lm.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                0,
                1);

        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
    }

    @After
    public void disableMockLocation() {
        lm.removeTestProvider(LocationManager.GPS_PROVIDER);
    }

    @TargetApi(17)
    private Location buildLocation(double latitude, double longitude) {
        Location l = new Location(LocationManager.GPS_PROVIDER);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        l.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            // Also need to set the et field
            l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        l.setAltitude(400);
        l.setAccuracy(1);
        return l;
    }

    @Test
    public void locationIsUpdated() throws InterruptedException {

        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

        double currLatitude, currLongitude;
        currLatitude = 46.5188;
        currLongitude = 6.5625;

        for (int i = 0; i < 10; i++) {
            double variation = Math.random() * .1;
            if (Math.random() < .5) {
                currLatitude += variation;
            } else {
                currLongitude += variation;
            }
            lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, buildLocation(currLatitude, currLongitude));
            Thread.sleep(1000);
            onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith(Double.toString(Math.floor(currLatitude * 100)/100)))));
            onView(withId(R.id.gpsLongitude)).check(matches(withText(startsWith(Double.toString(Math.floor(currLongitude * 100)/100)))));
        }
    }

    @Test
    public void changesInSignalProviderAreDetected() throws InterruptedException {
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
        Log.e("[TEST]", "Disabled location");
        Thread.sleep(1000);
        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith("Missing GPS signal"))));

        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, buildLocation(12, 19));
        Thread.sleep(1000);
        onView(withId(R.id.gpsLatitude)).check(matches(withText(not(startsWith("Missing GPS signal")))));
    }

    @Test
    public void reactionWhenEnabled() {
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        mActivityRule.getActivity().onProviderEnabled("something else");
    }

    @Test
    public void canRefreshPermissions() throws InterruptedException {
        lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
        mActivityRule.getActivity().onRequestPermissionsResult(2020, new String[]{"gps"}, new int[]{PackageManager.PERMISSION_GRANTED});
        Thread.sleep(1000);
        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith("Missing GPS signal"))));
    }
}
