package ch.epfl.sdp.map;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.Map.MapActivity;
import ch.epfl.sdp.Map.MapFragment;
import ch.epfl.sdp.Map.PathsHandler;

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



    ////////////////////////////////// Tests for PathsHandler //////////////////////////////////////

    @Test
    public void setCameraTargetToPath() {
        mapFragment.pathsHandler.latitude = 12.0;
        mapFragment.pathsHandler.longitude = 12.0;

        mapFragment.pathsHandler.setCameraPosition();

        double camLat = mapFragment.map.getCameraPosition().target.getLatitude();
        double camLon = mapFragment.map.getCameraPosition().target.getLongitude();

        assertEquals(12.0, camLat);
        assertEquals(12.0, camLon);
    }

    @Test
    public void pathGetsInstantiated() {
        //PathsHandler pathsHandler = new PathsHandler(mapFragment, mapFragment.map);
        sleep(15000);
        assertNotNull(mapFragment.pathsHandler.pathCoordinates);
    }

}
