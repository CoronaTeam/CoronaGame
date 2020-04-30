package ch.epfl.sdp;

import android.location.Location;
import android.util.Log;

import androidx.test.rule.ActivityTestRule;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.epfl.sdp.Map.PathsActivity;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;
import ch.epfl.sdp.fragment.PathsFragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PathsActivityTest {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


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

    @Test
    public void pathSuccessfullyRetrieved() {
        List<Point> path;
        PathsFragment pathsFragment = mActivityRule.getActivity().pathsFragment;
        pathsFragment.initFirestorePathRetrieval(value ->
                assertNotNull(value));
    }

}
