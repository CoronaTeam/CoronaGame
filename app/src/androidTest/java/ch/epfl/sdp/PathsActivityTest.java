package ch.epfl.sdp;

import androidx.test.rule.ActivityTestRule;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.Map.PathsActivity;
import ch.epfl.sdp.Map.PathsHandler;

import static org.junit.Assert.assertEquals;

public class PathsActivityTest {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Rule
    public final ActivityTestRule<PathsActivity> mActivityRule = new ActivityTestRule<>(PathsActivity.class);

    /*@Test
    public void cameraTargetsOnCurrentPath() {
        PathsFragment pathsFragment = mActivityRule.getActivity().pathsFragment;
        LatLng expectedLatLng = new LatLng(latitude, longitude);
        CameraPosition currentCameraPosition = pathsFragment.map.getCameraPosition();
        LatLng actualLatLng = currentCameraPosition.target;

        System.out.println("TARGET: " + actualLatLng.toString());
        assertEquals(expectedLatLng, actualLatLng);
    }*/

    @Test
    public void pathSuccessfullyRetrieved() {
        PathsHandler pathsHandler = mActivityRule.getActivity().pathsHandler;
        pathsHandler.initFirestorePathRetrieval(Assert::assertNotNull);
    }

}
