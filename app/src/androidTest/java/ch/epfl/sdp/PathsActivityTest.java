package ch.epfl.sdp;

import androidx.test.rule.ActivityTestRule;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import ch.epfl.sdp.Map.PathsActivity;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PathsActivityTest {


    @Rule
    public final ActivityTestRule<PathsActivity> mActivityRule = new ActivityTestRule<>(PathsActivity.class);
/*
    @Test
    public void cameraTargetsOnCurrentPath() {
        CameraPosition currentCameraPosition = mActivityRule.getActivity().pathsFragment.map.getCameraPosition();
        LatLng expectedLatLng = new LatLng(33.39767645465177, -118.39439114221236); // seems like rounding is done at 14 digits after decimal point /!\
        LatLng actualLatLng = currentCameraPosition.target;
        System.out.println("TARGET" + actualLatLng.toString());
        assertEquals(expectedLatLng, actualLatLng);
    }*/

    @Test
    public void pathGetsInstantiated() {
        List<Point> path = mActivityRule.getActivity().pathsFragment.pathCoordinates;
        assertNotNull(path);
    }

    @Test
    public void qsIteratorGetsInstantiated() {
        Iterator<QueryDocumentSnapshot> qsIterator = mActivityRule.getActivity().pathsFragment.qsIterator;
        assertNotNull(qsIterator);
    }

    @Test
    public void cfiNotNull() {
        ConcreteFirestoreInteractor cfi = mActivityRule.getActivity().pathsFragment.cfi;
        assertNotNull(cfi);
    }

    @Test
    public void qhandlerGetsInstantiated() {
        QueryHandler fireBaseHandler = mActivityRule.getActivity().pathsFragment.firestoreQueryHandler;
        assertNotNull(fireBaseHandler);
    }

}
