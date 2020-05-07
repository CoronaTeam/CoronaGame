package ch.epfl.sdp.Map;

import android.graphics.Color;
import android.media.JetPlayer;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.FirestoreInteractor;

import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * This class is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsHandler extends Fragment {
    // default access restriction for now, could be package-private, depending on how we finally decide to organize files
    public static final String PATH_LAYER_ID = "linelayer"; // public for testing
    static final String PATH_SOURCE_ID = "line-source";
    private static final int ZOOM = 7;
    public List<Point> pathCoordinates;
    private MapboxMap map;
    private FirestoreInteractor fsi = new ConcreteFirestoreInteractor();
    private MapFragment parentClass;
    private double latitude;
    private double longitude;

    public PathsHandler(@NonNull MapFragment parentClass, @NonNull MapboxMap map) { // public for testing
        this.parentClass = parentClass;
        this.map = map;
        initFirestorePathRetrieval().thenAccept(this::getPathCoordinates);
    }

    @VisibleForTesting
    public static String getPathLayerId() {
        return PATH_LAYER_ID;
    }

    @VisibleForTesting
    public List<Point> getPathCoordinatesAttribute() {
        return pathCoordinates;
    }

    @VisibleForTesting
    public double getLatitude() {
        return latitude;
    }

    @VisibleForTesting
    public double getLongitude() {
        return longitude;
    }

    // public for now, could be package-private, depending on how we finally decide to organize files
    public void setCameraPosition() {
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(ZOOM)
                .build();
        if (map != null) {
            map.setCameraPosition(position);
        }
    }

    private void getPathCoordinates(Map<String, Map<String, Object>> stringMapMap) {
        // TODO: get path for given day, NEED TO RETRIEVE POSITIONS ON SPECIFIC DAY TIME
        pathCoordinates = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> doc : stringMapMap.entrySet()) {
            try {
                GeoPoint geoPoint = (GeoPoint) ((Map) doc.getValue().get("Position")).get("geoPoint");
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                pathCoordinates.add(Point.fromLngLat(lon, lat));
            } catch (NullPointerException ignored) {
                Log.d("ERROR ADDING POINT", String.valueOf(ignored));
            }
        }

        Log.d("PATH COORD LENGTH: ", String.valueOf(pathCoordinates.size()));
        Log.d("IS PATH COORD NULL? ", (pathCoordinates == null) ? "YES" : "NO");
        latitude = pathCoordinates.get(0).latitude();
        longitude = pathCoordinates.get(0).longitude();
        setPathLayer();
    }

    private void setPathLayer() {
        Layer layer = new LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineWidth(5f),
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
        layer.setProperties(visibility(NONE));
    }

    private CompletableFuture<Map<String, Map<String, Object>>> initFirestorePathRetrieval() {
        return fsi.readCollection(collectionReference("History/USER_PATH_DEMO/Positions"))
                .thenApply(stringMapMap -> {
                    if (stringMapMap.isEmpty()) {
                        throw new RuntimeException("Collection doesn't contain any document");
                    } else {
                        return stringMapMap;
                    }
                })
                .exceptionally(e -> {
                    Toast.makeText(parentClass.getActivity(),
                            R.string.cannot_retrieve_positions,
                            Toast.LENGTH_LONG).show();
                    return Collections.emptyMap();
                });
    }


    /*private void getInfectedMet() { // This function is not done yet
        ConcreteDataReceiver concreteDataReceiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        //concreteDataReceiver.getUserNearbyDuring();
    }*/

}
