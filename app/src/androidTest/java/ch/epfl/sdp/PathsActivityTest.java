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
        CameraPosition currentCameraPosition = mActivityRule.getActivity().pathsFragment.map.getCameraPosition();
        LatLng expectedLatLng = new LatLng(33.39767645465177, -118.39439114221236); // seems like rounding is done at 14 digits after decimal point /!\
        LatLng actualLatLng = currentCameraPosition.target;
        System.out.println("TARGET" + actualLatLng.toString());
        assertEquals(expectedLatLng, actualLatLng);
    }

}
