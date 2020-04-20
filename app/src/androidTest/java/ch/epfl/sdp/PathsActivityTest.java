package ch.epfl.sdp;

import androidx.test.rule.ActivityTestRule;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.Map.PathsActivity;

import static org.junit.Assert.assertEquals;

public class PathsActivityTest {


    @Rule
    public final ActivityTestRule<PathsActivity> mActivityRule = new ActivityTestRule<>(PathsActivity.class);

    @Test
    public void cameraTargetsOnCurrentPath() {
        // load path at lat, long = (33.397676454651766, -118.39439114221236)
        // check equal
        CameraPosition currentCameraPosition = mActivityRule.getActivity().pathsFragment.map.getCameraPosition();
        CameraPosition expectedPosition = new CameraPosition.Builder()
                .target(new LatLng(33.397676454651766, -118.39439114221236))
                .build();
        assertEquals(expectedPosition, currentCameraPosition);
    }

}
