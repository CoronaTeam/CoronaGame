package ch.epfl.sdp.map.fragment;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.Layer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.R;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.map.MockConnectivityBroker;
import ch.epfl.sdp.testActivities.MapActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.location.LocationUtils.buildLocation;
import static ch.epfl.sdp.map.HeatMapHandler.HEATMAP_LAYER_ID;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class MapActivityTest {

    @Rule
    public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);
    private MapFragment mapFragment;
    private AtomicInteger sentinel;
    private MockConnectivityBroker mockLocationBroker;

    @BeforeClass
    public static void preClassSetup() {
        AccountFragment.IN_TEST = true;
        MapFragment.TESTING_MODE = true;
    }

    @AfterClass
    public static void postClassClean() {
        AccountFragment.IN_TEST = false;
        MapFragment.TESTING_MODE = false;
    }

    @Before
    public void setUp() throws Throwable {
        mockLocationBroker = new MockConnectivityBroker(activityRule);
        mockLocationBroker.setProviderStatus(true);
        mapFragment = (MapFragment) activityRule.getActivity().getFragment();
        mapFragment.setConnectivityBroker(mockLocationBroker);
        sentinel = new AtomicInteger(0);
    }

    @After
    public void clean() {
        sentinel = new AtomicInteger(0);
    }


    ////////////////////////////////// Tests //////////////////////////////////////


    @Test(timeout = 30000)
    public void testMapLoadCorrectly() throws Throwable{
        sTestMapLoadCorrectly(mapFragment, sentinel, activityRule);
    }

    @Test(timeout = 50000)
    public void testHeatMapLoadCorrectly() throws Throwable {
        testMapLoadCorrectly();

        mockLocationBroker.setFakeLocation(buildLocation(46, 54));

        while (mapFragment.getHeatMapHandler() == null) {
            sleep(500);
        }

        activityRule.runOnUiThread(() -> mapFragment.getHeatMapHandler().onHeatMapDataLoaded(
                sentinel::incrementAndGet));
        // The sentinel value will only increase when the heatmap has completely loaded

        activityRule.runOnUiThread(() -> {
            mapFragment.getHeatMapHandler().onHeatMapDataLoaded(sentinel::incrementAndGet);
        }); // The sentinel value will only increase when the heatmap has completely loaded

        waitForSentinelAndSetToZero();

    }

    @Test(timeout = 100000)
    public void testHeatMapToggleButton() throws Throwable {

        testHeatMapLoadCorrectly();

        testLayerVisibility(VISIBLE, HEATMAP_LAYER_ID);

        waitForSentinelAndSetToZero();
        onView(withId(R.id.heatMapToggle)).perform(click());

        testLayerVisibility(NONE, HEATMAP_LAYER_ID);

        waitForSentinelAndSetToZero();
    }

    @Test
    public void clickMyCurrentLocationTargetsMyLatLng() throws Throwable {
        testMapVisible();

        LatLng circleLatLng = mapFragment.getUserLocation();
        double exp_lat = circleLatLng.getLatitude();
        double exp_lon = circleLatLng.getLongitude();

        onView(withId(R.id.myCurrentLocationToggle)).perform(click());
        sleep(5000);

        LatLng cameraLatLng = mapFragment.getMap().getCameraPosition().target;
        double act_lat = cameraLatLng.getLatitude();
        double act_lon = cameraLatLng.getLongitude();

        double precision = 1;
        assertEquals(exp_lat, act_lat, precision);
        assertEquals(exp_lon, act_lon, precision);
    }


    ////////////////////////////////// Helper functions //////////////////////////////////////

    private void testLayerVisibility(String visibility, String layerId) throws Throwable {
        sTestLayerVisibility(visibility, layerId, mapFragment, sentinel, activityRule);
    }

    static void sTestLayerVisibility(String visibility, String layerId, MapFragment mapFragment, AtomicInteger sentinel, ActivityTestRule<MapActivity> activityRule) throws Throwable {
        activityRule.runOnUiThread(() -> {
            mapFragment.getMap().getStyle(style -> {
                Layer layer = style.getLayer(layerId);
                assertNotNull(layer);
                assertEquals(visibility, layer.getVisibility().getValue());
            });
            sentinel.incrementAndGet();
        });
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

    private void testMapVisible() throws Throwable {
        sTestMapVisible(mapFragment, sentinel, mockLocationBroker, activityRule);
    }

    static void sTestMapVisible(MapFragment mapFragment, AtomicInteger sentinel, MockConnectivityBroker mockConnectivityBroker, ActivityTestRule<MapActivity> activityRule) throws Throwable {
        sTestMapLoadCorrectly(mapFragment, sentinel, activityRule);
        mockConnectivityBroker.setFakeLocation(buildLocation(46, 55));
        mapFragment.onMapVisible(sentinel::incrementAndGet);
        sWaitForSentinelAndSetToZero(sentinel);
        sleep(1000);
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

}
