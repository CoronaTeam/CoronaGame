package ch.epfl.sdp.map;

import androidx.test.rule.ActivityTestRule;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.R;
import ch.epfl.sdp.testActivities.MapActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.sleep;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class MapActivityTest {

    private MapFragment mapFragment;

    @Rule
    public final ActivityTestRule<MapActivity> activityRule =
            new ActivityTestRule<>(MapActivity.class);

    @Before
    public void setUp() {
        mapFragment = (MapFragment) activityRule.getActivity().getFragment();
    }

    @Test
    public void pathGetsInstantiated() {
        sleep(15000);
        assertNotNull(mapFragment.getPathsHandler().getYesterdayPathCoordinatesAttribute());
    }

    @Test
    public void historyOptionsAreDisplayedWhenPressButton() {
        onView(withId(R.id.history_rfab)).perform(click());
        onView(withId(R.id.history_rfal)).check(matches(isDisplayed()));
    }

    // Since we want to test functions dealing with Calendar,
    // we don't use calendar for a more objective test:
    // we hardcode dates w.r.t. the day on which this test is ran
    @Test
    public void datesFormattedAsYYYYmmDD() { // their expected format is defined as "yyyy/MM/dd"
        String expected_yesterday = "2020/05/13";
        String expected_before = "2020/05/12";
        assertEquals(expected_yesterday, mapFragment.getPathsHandler().getYesterdayDate());
        assertEquals(expected_before, mapFragment.getPathsHandler().getBeforeYesterdayDate());
    }

    @Test @Ignore("Incomplete")
    public void togglePathMakesItVisible() {
        onView(withId(R.id.history_rfab)).perform(click());
        sleep(3000);
        //Layer layer = mapFragment.map.getStyle().getLayer(PATH_LAYER_ID);
        //assertEquals(VISIBLE, layer.getVisibility().getValue());
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
