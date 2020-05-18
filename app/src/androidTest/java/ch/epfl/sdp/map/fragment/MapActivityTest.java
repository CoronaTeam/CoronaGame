package ch.epfl.sdp.map.fragment;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

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
import ch.epfl.sdp.map.HeatMapHandler;
import ch.epfl.sdp.map.MockLocationBroker;
import ch.epfl.sdp.testActivities.MapActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.location.LocationUtils.buildLocation;
import static ch.epfl.sdp.map.HeatMapHandler.HEATMAP_LAYER_ID;
import static ch.epfl.sdp.map.MapFragment.TESTING_MODE;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_INFECTED_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_PATH_LAYER_ID;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class MapActivityTest {

    private MapFragment mapFragment;
    private AtomicInteger sentinel;
    private MockLocationBroker mockLocationBroker;
    private int pathCoordIsEmpty = 1; // 1 if empty, 0 otherwise
    private int infectedCoordIsEmpty = 1; // 1 if empty, 0 otherwise

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);

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

    @BeforeClass
    public static void preSetup() {
        AccountFragment.IN_TEST = true;
    }

    @AfterClass
    public static void postClean() {
        AccountFragment.IN_TEST = false;
    }

    @Test(timeout = 30000)
    public void testMapLoadCorrectly() throws Throwable {
        while (mapFragment.getMap() == null) {
            sleep(500);
        }

        activityRule.runOnUiThread(() -> {
            mapFragment.getMap().getStyle((s) -> sentinel.incrementAndGet());
        }); // The sentinel value will only increase when the style has completely loaded

        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);
    }


    @Test(timeout = 50000)
    public void testHeatMapLoadCorrectly() throws Throwable {
        testMapLoadCorrectly();

        while (mapFragment.getHeatMapHandler() == null) {
            sleep(500);
        }

        activityRule.runOnUiThread(() -> {
            mapFragment.getHeatMapHandler().onHeatMapDataLoaded(() -> sentinel.incrementAndGet());
        }); // The sentinel value will only increase when the heatmap has completely loaded

        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);
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

        testHeatMapLoadCorrectly();

        mockLocationBroker.setFakeLocation(buildLocation(46, 55));

        mapFragment.onMapVisible(() -> sentinel.incrementAndGet());

        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);

        testLayerVisibility(VISIBLE, HEATMAP_LAYER_ID);

        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);
        onView(withId(R.id.heatMapToggle)).perform(click());

        testLayerVisibility(NONE, HEATMAP_LAYER_ID);

        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);
    }

    ////////////////////////////////// Tests for PathsHandler //////////////////////////////////////

    @Test(timeout = 50000)
    public void yesterdayPathLayerIsSetWhenNotEmpty() throws Throwable {
        testMapLoadCorrectly();

        pathGetsInstantiated();

        if (!mapFragment.getPathsHandler().getYesterdayPathCoordinates().isEmpty()) {
            activityRule.runOnUiThread(() -> mapFragment.onLayerLoaded(() -> sentinel.incrementAndGet(),
                    YESTERDAY_PATH_LAYER_ID)); // The sentinel value will only increase if the layer on the map is not null

            while (sentinel.get() == 0) {
                sleep(500);
            }
            pathCoordIsEmpty = 0;
        }

        sentinel.set(0);
    }

    @Test(timeout = 50000)
    public void yesterdayInfectedLayerIsSetWhenNotEmpty() throws Throwable {
        testMapLoadCorrectly();

        pathGetsInstantiated();

        if (!mapFragment.getPathsHandler().getYesterdayInfectedMet().isEmpty()) {
            activityRule.runOnUiThread(() -> mapFragment.onLayerLoaded(() -> sentinel.incrementAndGet(),
                    YESTERDAY_INFECTED_LAYER_ID)); // The sentinel value will only increase if the layer on the map is not null

            while (sentinel.get() == 0) {
                sleep(500);
            }
            infectedCoordIsEmpty = 0;
        }

        sentinel.set(0);
    }

    @Test(timeout = 30000)
    public void pathGetsInstantiated() {
        while (mapFragment.getPathsHandler() == null) {
            sleep(500);
        }
        while (mapFragment.getPathsHandler().getYesterdayPathCoordinates() == null) {
            sleep(500);
        }
    }

    @Test
    public void pathsButtonLayoutIsDisplayedWhenPressed() {
        onView(withId(R.id.history_rfab)).perform(click());
        onView(withId(R.id.history_rfal)).check(matches(isDisplayed()));
    }

    // Since we want to test functions dealing with Calendar,
    // we don't use calendar for a more objective test:
    // we hardcode dates w.r.t. the day on which this test is ran
    @Test(timeout = 20000)
    public void datesFormattedAsYYYYmmDD() { // their expected format is defined as "yyyy/MM/dd"
        while (mapFragment.getPathsHandler() == null) {
            sleep(500);
        }
        String expected = "2020/05/18";
        Date date = new GregorianCalendar(2020, Calendar.MAY, 18).getTime();
        String actual = mapFragment.getPathsHandler().getSimpleDateFormat(date);

        assertEquals(expected, actual);
    }

    @Test(timeout = 100000)
    public void toggleYesterdayPathChangesVisibilityWhenNotEmpty() throws Throwable {
        testMapLoadCorrectly();
        mapFragment.onMapVisible(() -> sentinel.incrementAndGet());

        while (sentinel.get() == 0) {
            sleep(500);
        }
        sentinel.set(0);

        if (pathCoordIsEmpty == 0) {
            testLayerVisibility(NONE, YESTERDAY_PATH_LAYER_ID);

            while (sentinel.get() == 0) {
                sleep(500);
            }
            sentinel.set(0);

            if (infectedCoordIsEmpty == 0) {
                testLayerVisibility(NONE, YESTERDAY_INFECTED_LAYER_ID);

                while (sentinel.get() == 0) {
                    sleep(500);
                }
                sentinel.set(0);
            }

            onView(withId(R.id.history_rfab)).perform(click());
            List<RFACLabelItem> pathItems = ((RapidFloatingActionContentLabelList) mapFragment.getRfabHelper().obtainRFAContent()).getItems();
            activityRule.runOnUiThread(() -> mapFragment.onRFACItemIconClick(0, pathItems.get(0)));

            testLayerVisibility(VISIBLE, YESTERDAY_PATH_LAYER_ID);

            while (sentinel.get() == 0) {
                sleep(500);
            }
            sentinel.set(0);

            if (infectedCoordIsEmpty == 0) {
                testLayerVisibility(VISIBLE, YESTERDAY_INFECTED_LAYER_ID);

                while (sentinel.get() == 0) {
                    sleep(500);
                }
                sentinel.set(0);
            }
        }
    }

    @Test
    public void cameraTargetsPathWhenToggle() throws Throwable {
        testHeatMapLoadCorrectly();

        TESTING_MODE = true;

        mockLocationBroker.setFakeLocation(buildLocation(46, 55));

        onView(withId(R.id.history_rfab)).perform(click());
        List<RFACLabelItem> pathItems = ((RapidFloatingActionContentLabelList) mapFragment.getRfabHelper().obtainRFAContent()).getItems();

        activityRule.runOnUiThread(() -> mapFragment.onRFACItemIconClick(0, pathItems.get(0)));
        sleep(5000);
        double act_lat = mapFragment.getMap().getCameraPosition().target.getLatitude();
        double act_lon = mapFragment.getMap().getCameraPosition().target.getLongitude();

        double exp_lat = 46;
        double exp_lon = 55;
        double precision = 1;

        if (mapFragment.getPathsHandler().isPathLocationSet1() || PathsHandler.TEST_NON_EMPTY_LIST) {
            exp_lat = mapFragment.getPathsHandler().getLatitudeYesterday();
            exp_lon = mapFragment.getPathsHandler().getLongitudeYesterday();
        }

        assertEquals(exp_lat, act_lat, precision);
        assertEquals(exp_lon, act_lon, precision);

        TESTING_MODE = false;
    }

    @Test
    public void testsForNonEmptyPathAndInfected() throws Throwable {
        PathsHandler.TEST_NON_EMPTY_LIST = true;

        yesterdayPathLayerIsSetWhenNotEmpty();
        yesterdayInfectedLayerIsSetWhenNotEmpty();
        toggleYesterdayPathChangesVisibilityWhenNotEmpty();
        cameraTargetsPathWhenToggle();

        PathsHandler.TEST_NON_EMPTY_LIST = false;
    }

}
