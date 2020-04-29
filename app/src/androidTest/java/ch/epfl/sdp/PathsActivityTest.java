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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PathsActivityTest {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();


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
        //assertNull(path);
    }

    @Test
    public void qsIteratorGetsInstantiated() {
        Iterator<QueryDocumentSnapshot> qsIterator = mActivityRule.getActivity().pathsFragment.qsIterator;
        assertNotNull(qsIterator);
        //assertNull(qsIterator);
    }

   /* @Test
    public void sendDataToHitory() {
        Location location = TestUtils.buildLocation(33.39767645465177, -118.39439114221236);
        Map<String, Object> position = new HashMap();
        position.put("Position", new PositionRecord(Timestamp.now(),
                new GeoPoint(location.getLatitude(), location.getLongitude())));
        db.collection("History/Test_upload/Positions").add(position)
                .addOnSuccessListener(l -> Log.d("UPLOAD DATA", "UPLOAD SUCCESS!"))
                .addOnFailureListener(f -> Log.d("UPLOAD DATA", "ERROR UPLOADING"));
    }

    @Test
    public void getDataFromHistory() {
        Log.d("GET DATA", "Testing to see logcat");
        db.collection("Tests").get()//.document("Test_upload").collection("Positions").get()
                .addOnSuccessListener(l ->
                        Log.d("GET DATA", "DOWNLOAD SUCCESS!"))
                .addOnFailureListener(f ->
                        Log.d("GET DATA", "ERROR DOWNLOADING"));
    }

    /*@Test
    public void cfiNotNull() {
        ConcreteFirestoreInteractor cfi = mActivityRule.getActivity().pathsFragment.cfi;
        assertNotNull(cfi);
    }

    @Test
    public void queryHandlerGetsInstantiated() {
        QueryHandler fireBaseHandler = mActivityRule.getActivity().pathsFragment.firestoreQueryHandler;
        assertNotNull(fireBaseHandler);
    }*/

}
