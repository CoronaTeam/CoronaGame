package ch.epfl.sdp.map.fragment;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

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
import ch.epfl.sdp.map.MockConnectivityBroker;
import ch.epfl.sdp.map.PathsHandler;
import ch.epfl.sdp.testActivities.MapActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.location.LocationUtils.buildLocation;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_INFECTED_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_PATH_LAYER_ID;
import static ch.epfl.sdp.map.fragment.MapActivityTest.sTestLayerVisibility;
import static ch.epfl.sdp.map.fragment.MapActivityTest.sTestMapLoadCorrectly;
import static ch.epfl.sdp.map.fragment.MapActivityTest.sTestMapVisible;
import static ch.epfl.sdp.map.fragment.MapActivityTest.sWaitForSentinelAndSetToZero;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class PathHandlerTest {

    @Rule
    public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);
    private final int FOCUS_PATH_BUTTON_POSITION = 2;
    private final int YESTERDAY_PATH_BUTTON_POSITION = 3;
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);
    private MapFragment mapFragment;
    private AtomicInteger sentinel;
    private MockConnectivityBroker mockConnectivityBroker;
    private boolean pathCoordIsEmpty;
    private boolean infectedCoordIsEmpty;

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
        mockConnectivityBroker = new MockConnectivityBroker(activityRule);
        mockConnectivityBroker.setProviderStatus(true);
        mapFragment = (MapFragment) activityRule.getActivity().getFragment();
        mapFragment.setConnectivityBroker(mockConnectivityBroker);
        sentinel = new AtomicInteger(0);

        testMapLoadCorrectly();
        mapFragment.resetPathsHandler(sentinel::incrementAndGet);
        waitForSentinelAndSetToZero();
    }

    @After
    public void clean() {
        sentinel = new AtomicInteger(0);
    }


    ////////////////////////////////// Tests //////////////////////////////////////

    @Test(timeout = 20000)
    public void yesterdayPathLayerIsSetWhenNotEmpty() throws Throwable {
        pathGetsInstantiated();

        if (!mapFragment.getPathsHandler().getYesterdayPathCoordinates().isEmpty()) {
            testLayerIsSet(YESTERDAY_PATH_LAYER_ID);
            pathCoordIsEmpty = false;
        } else {
            pathCoordIsEmpty = true;
        }
    }

    @Test(timeout = 20000)
    public void yesterdayInfectedLayerIsSetWhenNotEmpty() throws Throwable {
        pathGetsInstantiated();

        if (!mapFragment.getPathsHandler().getYesterdayInfectedMet().isEmpty()) {
            testLayerIsSet(YESTERDAY_INFECTED_LAYER_ID);
            infectedCoordIsEmpty = false;
        } else {
            infectedCoordIsEmpty = true;
        }
    }

    @Test(timeout = 30000)
    public void pathGetsInstantiated() throws Throwable {
        activityRule.runOnUiThread(() -> mapFragment.getPathsHandler().onPathDataLoaded(sentinel::incrementAndGet));
        waitForSentinelAndSetToZero();
    }

    @Test
    public void pathsButtonLayoutIsDisplayedWhenPressed() {
        onView(withId(R.id.more_rfab)).perform(click());
        onView(withId(R.id.more_rfal)).check(matches(isDisplayed()));
    }

    @Test(timeout = 8000)
    public void datesFormattedAsYYYYmmDD() {
        while (mapFragment.getPathsHandler() == null) {
            sleep(500);
        }
        String expected = "2020/05/18";
        Date date = new GregorianCalendar(2020, Calendar.MAY, 18).getTime();
        String actual = mapFragment.getPathsHandler().getSimpleDateFormat(date);

        assertEquals(expected, actual);
    }

    @Test(timeout = 30000)
    public void toggleYesterdayPathChangesVisibilityWhenNotEmpty() throws Throwable {
        yesterdayPathLayerIsSetWhenNotEmpty();

        testClickChangesVisibility(pathCoordIsEmpty, YESTERDAY_PATH_LAYER_ID);

    }

    @Test(timeout = 30000)
    public void infectedLayerVisibilityChangesWhenNotEmpty() throws Throwable {
        yesterdayInfectedLayerIsSetWhenNotEmpty();

        testClickChangesVisibility(infectedCoordIsEmpty, YESTERDAY_INFECTED_LAYER_ID);
    }

    @Test
    public void cameraTargetsPathWhenToggle() throws Throwable {
        testMapVisible();

        mockConnectivityBroker.setFakeLocation(buildLocation(46, 55));

        clickToSeePath();

        double exp_lat = 46;
        double exp_lon = 55;
        double precision = 1;

        if (mapFragment.getPathsHandler().isPathLocationSet1()) {
            exp_lat = mapFragment.getPathsHandler().getLatitudeYesterday();
            exp_lon = mapFragment.getPathsHandler().getLongitudeYesterday();
        }

        double act_lat = mapFragment.getMap().getCameraPosition().target.getLatitude();
        double act_lon = mapFragment.getMap().getCameraPosition().target.getLongitude();
        assertEquals(exp_lat, act_lat, precision);
        assertEquals(exp_lon, act_lon, precision);
    }

    @Test(timeout = 40000)
    public void testsForNonEmptyPathAndInfected() throws Throwable {
        pathGetsInstantiated();
        yesterdayPathLayerIsSetWhenNotEmpty();
        yesterdayInfectedLayerIsSetWhenNotEmpty();

        activityRule.runOnUiThread(() -> {
            mapFragment.getPathsHandler().resetPaths(PathsHandler.TestOP.TEST_NON_EMPTY_LIST,
                    () -> sentinel.incrementAndGet());
        });
        waitForSentinelAndSetToZero();

        if (pathCoordIsEmpty) {
            toggleYesterdayPathChangesVisibilityWhenNotEmpty();
            cameraTargetsPathWhenToggle();
        }

        if (infectedCoordIsEmpty) {
            infectedLayerVisibilityChangesWhenNotEmpty();
        }

    }

    @Test
    public void seeWholePathButtonAppearsWhenSeeingNonEmptyPath() throws Throwable {
        PathButtonTestBody(PathsHandler.TestOP.TEST_NON_EMPTY_LIST);

    }

    private void PathButtonTestBody(PathsHandler.TestOP testNonEmptyList) throws Throwable {
        pathGetsInstantiated();
        activityRule.runOnUiThread(() -> {
            mapFragment.getPathsHandler().resetPaths(testNonEmptyList,
                    () -> sentinel.incrementAndGet());
        });
        waitForSentinelAndSetToZero();

        testMapVisible();
        clickToSeePath();

        onView(withId(R.id.more_rfab)).perform(click());
        List<RFACLabelItem> pathItems = ((RapidFloatingActionContentLabelList)
                mapFragment.getRfabHelper().obtainRFAContent()).getItems();
        activityRule.runOnUiThread(
                () -> mapFragment.onRFACItemIconClick(FOCUS_PATH_BUTTON_POSITION, pathItems.get(FOCUS_PATH_BUTTON_POSITION)));
    }

    @Test(timeout = 30000)
    public void seeWholePathButtonInvisibleWhenNoPath() throws Throwable {
        PathButtonTestBody(PathsHandler.TestOP.TEST_EMPTY_PATH);

    }

    @Test(timeout = 35000)
    public void clickSeeWholePathMakeZoomAdjust() throws Throwable {
        pathGetsInstantiated();

        activityRule.runOnUiThread(() -> {
            mapFragment.getPathsHandler().resetPaths(PathsHandler.TestOP.TEST_NON_EMPTY_LIST,
                    () -> sentinel.incrementAndGet());
        });
        waitForSentinelAndSetToZero();

        testMapVisible();

        pathGetsInstantiated();

        clickToSeePath();

        double zoom_before = mapFragment.getMap().getCameraPosition().zoom;

        onView(withId(R.id.more_rfab)).perform(click());
        List<RFACLabelItem> pathItems = ((RapidFloatingActionContentLabelList)
                mapFragment.getRfabHelper().obtainRFAContent()).getItems();
        activityRule.runOnUiThread(
                () -> mapFragment.onRFACItemIconClick(FOCUS_PATH_BUTTON_POSITION, pathItems.get(FOCUS_PATH_BUTTON_POSITION)));

        sleep(5000);
        double zoom_after = mapFragment.getMap().getCameraPosition().zoom;

        assertTrue(zoom_before != zoom_after);

    }


    ////////////////////////////////// Helper functions //////////////////////////////////////

    private void testLayerVisibility(String visibility, String layerId) throws Throwable {
        sTestLayerVisibility(visibility, layerId, mapFragment, sentinel, activityRule);
    }

    private void clickToSeePath() throws Throwable {
        onView(withId(R.id.more_rfab)).perform(click());
        List<RFACLabelItem> pathItems = ((RapidFloatingActionContentLabelList)
                mapFragment.getRfabHelper().obtainRFAContent()).getItems();

        activityRule.runOnUiThread(
                () -> mapFragment.onRFACItemIconClick(YESTERDAY_PATH_BUTTON_POSITION, pathItems.get(YESTERDAY_PATH_BUTTON_POSITION)));
        sleep(15000);
    }

    private void testLayerIsSet(String layerId) throws Throwable {
        activityRule.runOnUiThread(() -> mapFragment.onLayerLoaded(()
                -> sentinel.incrementAndGet(), layerId));
        // The sentinel value will only increase if the layer on the map is not null

        waitForSentinelAndSetToZero();
    }

    private void testLayerVisibilityIfNotEmpty(String visibility, boolean listIsEmpty, String layerId) throws Throwable {
        if (!listIsEmpty) {
            testLayerVisibility(visibility, layerId);
            waitForSentinelAndSetToZero();
        }
    }

    private void testClickChangesVisibility(boolean coordListIsEmpty, String layerId) throws Throwable {
        mockConnectivityBroker.setFakeLocation(buildLocation(46, 55));

        mapFragment.onMapVisible(() -> sentinel.incrementAndGet());
        waitForSentinelAndSetToZero();

        testLayerVisibilityIfNotEmpty(NONE, coordListIsEmpty, layerId);

        onView(withId(R.id.more_rfab)).perform(click());
        List<RFACLabelItem> pathItems = ((RapidFloatingActionContentLabelList)
                mapFragment.getRfabHelper().obtainRFAContent()).getItems();
        activityRule.runOnUiThread(() ->
                mapFragment.onRFACItemIconClick(YESTERDAY_PATH_BUTTON_POSITION, pathItems.get(YESTERDAY_PATH_BUTTON_POSITION)));

        testLayerVisibilityIfNotEmpty(VISIBLE, coordListIsEmpty, layerId);
    }

    private void waitForSentinelAndSetToZero() {
        sWaitForSentinelAndSetToZero(sentinel);
    }

    private void testMapLoadCorrectly() throws Throwable {
        sTestMapLoadCorrectly(mapFragment, sentinel, activityRule);
    }

    private void testMapVisible() throws Throwable {
        sTestMapVisible(mapFragment, sentinel, mockConnectivityBroker, activityRule);
    }
}
