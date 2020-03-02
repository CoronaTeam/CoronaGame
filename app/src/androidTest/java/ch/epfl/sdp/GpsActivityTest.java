package ch.epfl.sdp;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;

import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class GpsActivityTest {

    private LocationManager lm;

    @Rule
    public final ActivityTestRule<GpsActivity> mActivityRule =
            new ActivityTestRule<>(GpsActivity.class);

    @Before
    public void setupMockLocation() {
        lm = (LocationManager) mActivityRule.getActivity().getSystemService(Context.LOCATION_SERVICE);

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
    public void missingSignal() throws InterruptedException {
        // TODO: implement this
    }
}
