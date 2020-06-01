package ch.epfl.sdp.map.fragment;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RFACLabelItem;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RapidFloatingActionContentLabelList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.R;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.map.MockLocationBroker;
import ch.epfl.sdp.map.PathsHandler;
import ch.epfl.sdp.testActivities.MapActivity;
import ch.epfl.sdp.toDelete.GpsActivityTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.location.LocationUtils.buildLocation;
import static ch.epfl.sdp.map.HeatMapHandler.HEATMAP_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_INFECTED_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_PATH_LAYER_ID;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class MapActivityTest {

    @Rule
    public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);
    private MapFragment mapFragment;
    private AtomicInteger sentinel;
    private MockLocationBroker mockLocationBroker;


    @BeforeClass
    public static void preSetup() {
        AccountFragment.IN_TEST = true;
        MapFragment.TESTING_MODE = true;
    }

    @AfterClass
    public static void postClean() {
        AccountFragment.IN_TEST = false;
        MapFragment.TESTING_MODE = false;
    }

    @Before
    public void setUp() throws Throwable {
        mockLocationBroker = new MockLocationBroker(activityRule);
        mockLocationBroker.setProviderStatus(true);
        mapFragment = (MapFragment) activityRule.getActivity().getFragment();
        mapFragment.setLocationBroker(mockLocationBroker);
        sentinel = new AtomicInteger(0);
    }

    @After
    public void clean() {
        sentinel = new AtomicInteger(0);
        //Intents.release();
    }

    @Test(timeout = 30000)
    public void testMapLoadCorrectly() throws Throwable{
        sTestMapLoadCorrectly(mapFragment, sentinel, activityRule);
    }


    static void sTestMapLoadCorrectly(MapFragment mapFragment, AtomicInteger sentinel, ActivityTestRule<MapActivity> activityRule) throws Throwable {
        while (mapFragment.getMap() == null) {
            sleep(500);
        }

        activityRule.runOnUiThread(() -> mapFragment.getMap().getStyle((s)
                -> sentinel.incrementAndGet()));
        // The sentinel value will only increase when the style has completely loaded

        sWaitForSentinelAndSetToZero(sentinel);
    }


    @Test(timeout = 50000)
    public void testHeatMapLoadCorrectly() throws Throwable {
        testMapLoadCorrectly();

        mockLocationBroker.setFakeLocation(buildLocation(46, 54));

        while (mapFragment.getHeatMapHandler() == null) {
            sleep(500);
        }

        activityRule.runOnUiThread(() -> mapFragment.getHeatMapHandler().onHeatMapDataLoaded(()
                -> sentinel.incrementAndGet()));
        // The sentinel value will only increase when the heatmap has completely loaded

        activityRule.runOnUiThread(() -> {
            mapFragment.getHeatMapHandler().onHeatMapDataLoaded(() -> sentinel.incrementAndGet());
        }); // The sentinel value will only increase when the heatmap has completely loaded

        waitForSentinelAndSetToZero();

    }

    private void testLayerVisibility(String visibility, String layerId) throws Throwable {
        activityRule.runOnUiThread(() -> {
            mapFragment.getMap().getStyle(style -> {
                Layer layer = style.getLayer(layerId);
                assertNotNull(layer);
                assertEquals(visibility, layer.getVisibility().getValue());
            });
            sentinel.incrementAndGet();
        });
    }

    @Test(timeout = 100000)
    public void testHeatMapToggleButton() throws Throwable {

        testMapVisible();

        testLayerVisibility(VISIBLE, HEATMAP_LAYER_ID);

        waitForSentinelAndSetToZero();
        onView(withId(R.id.heatMapToggle)).perform(click());

        testLayerVisibility(NONE, HEATMAP_LAYER_ID);

        waitForSentinelAndSetToZero();
    }

    @Test
    public void clickMyCurrentLocationTargetsMyLatLng() throws Throwable {
        testMapVisible();

        LatLng circleLatLng = mapFragment.getUserLocation().getLatLng();
        double exp_lat = circleLatLng.getLatitude();
        double exp_lon = circleLatLng.getLongitude();
        double precision = 1;

        onView(withId(R.id.myCurrentLocation)).perform(click());
        sleep(5000);

        LatLng cameraLatLng = mapFragment.getMap().getCameraPosition().target;
        double act_lat = cameraLatLng.getLatitude();
        double act_lon = cameraLatLng.getLongitude();

        assertEquals(exp_lat, act_lat, precision);
        assertEquals(exp_lon, act_lon, precision);
    }

    ////////////////////////////////// Tests for PathsHandler //////////////////////////////////////


    private void testMapVisible() throws Throwable {
        testHeatMapLoadCorrectly();
        mockLocationBroker.setFakeLocation(buildLocation(46, 55));
        mapFragment.onMapVisible(() -> sentinel.incrementAndGet());
        waitForSentinelAndSetToZero();
    }

    private void testLayerVisibilityIfNotEmpty(String visibility, boolean listIsEmpty, String layerId) throws Throwable {
        if (!listIsEmpty) {
            testLayerVisibility(visibility, layerId);
            waitForSentinelAndSetToZero();
        }
    }

    private void waitForSentinelAndSetToZero() {
        sWaitForSentinelAndSetToZero(sentinel);
    }

    static void sWaitForSentinelAndSetToZero(AtomicInteger sentinel) {
        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);
    }

    @Test
    public void asksForPermissions() throws Throwable {
        mockLocationBroker = new MockLocationBroker(activityRule) {
            private boolean fakePermissions = false;

            @Override
            public boolean hasPermissions(Provider provider) {
                return fakePermissions;
            }

            @Override
            public void requestPermissions(Activity activity, int requestCode) {
                fakePermissions = true;
                activity.onRequestPermissionsResult(requestCode, new String[]{"GPS"}, new int[]{PackageManager.PERMISSION_GRANTED});
            }
        };
        startActivityWithBroker(withoutPermissions);

        withoutPermissions.setProviderStatus(true);
        withoutPermissions.setFakeLocation(buildLocation(2, 3));

        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith(Double.toString(2)))));
        onView(withId(R.id.gpsLongitude)).check(matches(withText(startsWith(Double.toString(3)))));
    }

}
