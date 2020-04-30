package ch.epfl.sdp.Map;

import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;

/**
 * This class is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsHandler extends Fragment {
    private MapboxMap map;
    private List<Point> pathCoordinates;
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); // we don't use ConcreteFirestoreInteractor because we want to do more specific op
    private MapFragment parentClass;
    private double latitude;
    private double longitude;

    static final String PATH_LAYER_ID = "linelayer";
    static final String PATH_SOURCE_ID = "line-source";

    PathsHandler(@NonNull MapFragment parentClass, @NonNull MapboxMap map) {
        this.parentClass = parentClass;
        this.map = map;
        initFirestorePathRetrieval(this::getPathCoordinates);
    }

    private void getPathCoordinates(@NonNull Iterator<QueryDocumentSnapshot> qsIterator) {
        // TODO: RETRIEVE FROM CACHE IF AVAILABLE
        // TODO: get path for given day
        // NEED TO RETRIEVE POSITIONS ON SPECIFIC DAY TIME
        pathCoordinates = new ArrayList<>();

        for (; qsIterator.hasNext(); ) {
            QueryDocumentSnapshot qs = qsIterator.next();
            try {
                GeoPoint geoPoint = (GeoPoint)((Map)qs.get("Position")).get("geoPoint");
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                pathCoordinates.add(Point.fromLngLat(lon, lat));
            } catch (NullPointerException ignored) {
                Log.d("ERROR ADDING POINT", String.valueOf(ignored));
            }
        }

        Log.d("PATH COORD LENGTH: ", String.valueOf(pathCoordinates.size())); // is 0
        Log.d("IS PATH COORD NULL? ", (pathCoordinates == null) ? "YES" : "NO"); // is not null
        //latitude = pathCoordinates.get(0).latitude(); // pathCoordinate has no index 0 here
        //longitude = pathCoordinates.get(0).longitude();
        setPathLayer();
    }

    public void setCameraPosition() {
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(20)
                .build();
        if (map != null) {
            map.setCameraPosition(position);
        }
    }

    private void setPathLayer() {
        Layer layer = new LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineWidth(10f),
                PropertyFactory.lineColor(Color.parseColor("maroon"))
        );
        map.getStyle(style -> {

            // Create the LineString from the list of coordinates and then make a GeoJSON
            // FeatureCollection so we can add the line to our map as a layer.
            style.addSource(new GeoJsonSource(PATH_SOURCE_ID,
                    FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(
                            LineString.fromLngLats(pathCoordinates)
                    )})));

            style.addLayer(layer);
        });
    }

    // public for testing
    public void initFirestorePathRetrieval(Callback<Iterator<QueryDocumentSnapshot>> callback) {

        //firestoreQueryHandler = getQueryHandler();
        //cfi.readCollection("History/USER_PATH_DEMO/Positions", firestoreQueryHandler).limit(); // read all positions for this user
        db.collection("History/USER_PATH_DEMO/Positions")
                //.orderBy("timestamp")
                //.limit(50)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onCallback(task.getResult().iterator());
                    } else {
                        //Toast.makeText(parentClass.getActivity(), "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void getInfectedMet() {
        ConcreteDataReceiver concreteDataReceiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        //concreteDataReceiver.getUserNearbyDuring();
    }

}
