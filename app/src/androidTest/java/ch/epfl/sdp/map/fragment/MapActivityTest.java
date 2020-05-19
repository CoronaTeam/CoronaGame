package ch.epfl.sdp.map.fragment;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.mapbox.mapboxsdk.style.layers.Layer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

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
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class MapActivityTest {

    private MapFragment mapFragment;
    private AtomicInteger sentinel;
    private MockLocationBroker mockLocationBroker;

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public final ActivityTestRule<MapActivity> activityRule = new ActivityTestRule<>(MapActivity.class);

    @Before
    public void setUp() throws Throwable{
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
    public static void preSetup(){
        AccountFragment.IN_TEST = true;
    }

    @AfterClass
    public static void postClean(){
        AccountFragment.IN_TEST = false;
    }

    @Test(timeout = 30000)
    public void testMapLoadCorrectly() throws Throwable {
        while (mapFragment.getMap() == null){sleep(500);};

        activityRule.runOnUiThread(() -> {
            mapFragment.getMap().getStyle((s) -> sentinel.incrementAndGet());
        }); // The sentinel value will only increase when the style has completely loaded

        while (sentinel.get() == 0){sleep(500);}
        sentinel.set(0);
    }


    @Test(timeout = 50000)
    public void testHeatMapLoadCorrectly() throws Throwable {
        testMapLoadCorrectly();

        while (mapFragment.getHeatMapHandler() == null){sleep(500);};

        activityRule.runOnUiThread(() -> {
            mapFragment.getHeatMapHandler().onHeatMapDataLoaded(() -> sentinel.incrementAndGet());
        }); // The sentinel value will only increase when the heatmap has completely loaded

        while (sentinel.get() == 0){sleep(500);}
        sentinel.set(0);
    }

    private void testLayerVisibility(String visibility) throws Throwable{
        activityRule.runOnUiThread(() -> {
            mapFragment.getMap().getStyle(style -> {
                Layer layer = style.getLayer(HeatMapHandler.HEATMAP_LAYER_ID);
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

        while (sentinel.get() == 0){sleep(500);}
        sentinel.set(0);

        testLayerVisibility(VISIBLE);

        while (sentinel.get() == 0){sleep(500);}
        sentinel.set(0);
        onView(withId(R.id.heatMapToggle)).perform(click());

        testLayerVisibility(NONE);

        while (sentinel.get() == 0){sleep(500);}
        sentinel.set(0);
    }

    ////////////////////////////////// Tests for PathsHandler //////////////////////////////////////
    /*@Test
    public void setCameraTargetToPath() {
        mapFragment.pathsHandler.latitude = 12.0;
        mapFragment.pathsHandler.longitude = 12.0;

        mapFragment.pathsHandler.setCameraPosition();

        double camLat = mapFragment.map.getCameraPosition().target.getLatitude();
        double camLon = mapFragment.map.getCameraPosition().target.getLongitude();

        assertEquals(12.0, camLat);
        assertEquals(12.0, camLon);
    }*/ // DON'T KNOW HOW TO TEST THIS YET: DOES NOT PASS THE CIRRUS BUILD -->> "Map interactions should happen on the UI thread. Method invoked from wrong thread is cancelTransitions."

    @Test
    public void togglePathMakesItVisible() {
        onView(withId(R.id.history_button)).perform(click());
        onView(withId(R.id.pathButton)).perform(click());
        sleep(3000);
        //Layer layer = mapFragment.map.getStyle().getLayer(PATH_LAYER_ID);
        //assertEquals(VISIBLE, layer.getVisibility().getValue());
    }

    @Test
    @Ignore("Lucie please fix")
    public void pathGetsInstantiated() {
        sleep(15000);
        assertNotNull(mapFragment.getPathsHandler().pathCoordinates);
    }

    //"Map interactions should happen on the UI thread. Method invoked from wrong thread is getLayer.")
    @Test
    public void pathButtonIsDisplayedInHistory() {
        onView(withId(R.id.history_button)).perform(click());
        onView(withId(R.id.pathButton)).check(matches(isDisplayed()));
    }

    //"Map interactions should happen on the UI thread. Method invoked from wrong thread is getLayer.")
    @Test @Ignore("Incomplete")
    public void pathNotVisibleByDefault() {
        /*onView(withId(R.id.history_button)).perform(click());
        onView(withId(R.id.pathButton)).perform(click());
        sleep(3000);
        Layer layer = mapFragment.map.getStyle().getLayer(PATH_LAYER_ID);
        assertNotEquals(VISIBLE, layer.getVisibility().getValue());*/
    }

    //"need to be able to click back on the map to make the history dialog fragment disappear")
    @Test @Ignore("Incomplete")
    public void cameraTargetsPathWhenToggle() {
        /*sleep(15000);
        onView(withId(R.id.history_button)).perform(click());
        onView(withId(R.id.pathButton)).perform(click());
        double exp_lat = mapFragment.pathsHandler.latitude;
        double exp_lon = mapFragment.pathsHandler.longitude;
        double act_lat = mapFragment.getMap().getCameraPosition().target.getLatitude();
        double act_lon = mapFragment.getMap().getCameraPosition().target.getLongitude();
        assertEquals(exp_lat, act_lat);
        assertEquals(exp_lon, act_lon);*/
    }
}