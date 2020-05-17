package ch.epfl.sdp.map;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.location.LocationUtils;
import ch.epfl.sdp.map.fragment.MapFragment;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.map.HeatMapHandler.adjustHeatMapColorRange;
import static ch.epfl.sdp.map.HeatMapHandler.adjustHeatMapWeight;
import static ch.epfl.sdp.map.HeatMapHandler.adjustHeatmapIntensity;
import static ch.epfl.sdp.map.HeatMapHandler.adjustHeatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * This class is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsHandler extends Fragment {
    static final String POINTS_SOURCE_ID = "points-source";
    static final String POINTS_LAYER_ID = "pointslayer";
    static final String PATH_SOURCE_ID = "line-source";
    private static final int ZOOM = 7;
    public static final String PATH_LAYER_ID = "linelayer"; // public for testing
    public List<Point> pathCoordinates;
    public List<Point> infected_met;
    private MapboxMap map;
    private FirestoreInteractor fsi = new ConcreteFirestoreInteractor();
    private MapFragment parentClass;
    private double latitude;
    private double longitude;


    public PathsHandler(@NonNull MapFragment parentClass, @NonNull MapboxMap map) {
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
        infected_met = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> doc : stringMapMap.entrySet()) {
            try {
                GeoPoint geoPoint = (GeoPoint) ((Map) doc.getValue().get("Position")).get(GEOPOINT_TAG);
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                pathCoordinates.add(Point.fromLngLat(lon, lat));
                // check infected met around this point of the path
                Timestamp timestamp = (Timestamp) ((Map) doc.getValue().get("Position")).get(
                        "timestamp");
                addInfectedMet(lat, lon, timestamp);
            } catch (NullPointerException e) {
                Log.d("ERROR ADDING POINT", String.valueOf(e));
            }
        }

        Log.d("PATH COORD LENGTH: ", String.valueOf(pathCoordinates.size()));
        Log.d("IS PATH COORD NULL? ", (pathCoordinates == null) ? "YES" : "NO");
        latitude = pathCoordinates.get(0).latitude();
        longitude = pathCoordinates.get(0).longitude();
        setPathLayer();
        setInfectedPointsLayer();
    }

    private void addInfectedMet(double lat, double lon, Timestamp timestamp) {
        ConcreteDataReceiver concreteDataReceiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        Location location = LocationUtils.buildLocation(lat, lon);
        concreteDataReceiver
                .getUserNearbyDuring(location, timestamp.toDate(), timestamp.toDate())
                .thenAccept(carrierIntegerMap -> {
                    //Log.d("ADD INFECTED", "got future value");
                    Carrier carrier;
                    Point point;
                    for (Map.Entry<Carrier, Integer> entry : carrierIntegerMap.entrySet()) {
                        carrier = entry.getKey();
                        if (carrier.getInfectionStatus().equals(INFECTED)) {
                            point = Point.fromLngLat(lon, lat);
                            infected_met.add(point);
                        }
                    }
                });
    }

    private void setPathLayer() {
        Layer layer = new LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineColor(Color.parseColor("maroon"))
        );
        LineString geometry = LineString.fromLngLats(pathCoordinates);
        mapStyle(layer, geometry, PATH_SOURCE_ID);
        layer.setProperties(visibility(NONE));
    }

    private void setInfectedPointsLayer() {
        Layer layer = new HeatmapLayer(POINTS_LAYER_ID, POINTS_SOURCE_ID);
        layer.setProperties(
                adjustHeatMapColorRange(),
                adjustHeatMapWeight(),
                adjustHeatmapIntensity(),
                adjustHeatmapRadius()
        );
        MultiPoint geometry = MultiPoint.fromLngLats(infected_met);
        mapStyle(layer, geometry, POINTS_SOURCE_ID);
        layer.setProperties(visibility(NONE));
    }

    private void mapStyle(Layer layer, Geometry geometry, String sourceId) {
        map.getStyle(style -> {
            style.addSource(new GeoJsonSource(sourceId,
                    FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(geometry)})));
            style.addLayer(layer);
        });
    }

    private CompletableFuture<Map<String, Map<String, Object>>> initFirestorePathRetrieval() {
        return FirestoreInteractor.taskToFuture(
                collectionReference("History/USER_PATH_DEMO" + "/Positions")
                        .orderBy("Position" + ".timestamp").get())
                .thenApply(collection -> {
                    if (collection.isEmpty()) {
                        throw new RuntimeException("Collection doesn't contain any document");
                    } else {
                        List<DocumentSnapshot> list = collection.getDocuments();
                        Map<String, Map<String, Object>> result = new HashMap<>();
                        for (DocumentSnapshot doc : list) {
                            result.put(doc.getId(), doc.getData());
                        }
                        return result;
                    }
                })
                .exceptionally(e -> {
                    Toast.makeText(parentClass.getActivity(),
                            R.string.cannot_retrieve_positions,
                            Toast.LENGTH_LONG).show();
                    return Collections.emptyMap();
                });
    }
}
