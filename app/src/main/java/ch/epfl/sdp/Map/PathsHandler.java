package ch.epfl.sdp.Map;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.location.LocationUtils;

import static ch.epfl.sdp.Map.HeatMapHandler.adjustHeatMapColorRange;
import static ch.epfl.sdp.Map.HeatMapHandler.adjustHeatMapWeight;
import static ch.epfl.sdp.Map.HeatMapHandler.adjustHeatmapIntensity;
import static ch.epfl.sdp.Map.HeatMapHandler.adjustHeatmapRadius;
import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * This class is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsHandler extends Fragment {
    static final String YESTERDAY_POINTS_SOURCE_ID = "points-source-one";
    static final String BEFORE_POINTS_SOURCE_ID = "points-source-two";
    static final String YESTERDAY_POINTS_LAYER_ID = "pointslayer-one";
    static final String BEFORE_POINTS_LAYER_ID = "pointslayer-two";
    static final String YESTERDAY_PATH_SOURCE_ID = "line-source-one";
    static final String BEFORE_PATH_SOURCE_ID = "line-source-two";
    private static final int ZOOM = 13;
    static final String YESTERDAY_PATH_LAYER_ID = "linelayer-one";
    static final String BEFORE_PATH_LAYER_ID = "linelayer-two";
    public List<Point> yesterdayPathCoordinates;
    public List<Point> beforeYesterdayPathCoordinates;
    public List<Point> yesterdayInfectedMet;
    public List<Point> beforeYesterdayInfectedMet;
    private MapboxMap map;
    private MapFragment parentClass;
    private double latitudeYesterday;
    private double latitudeBefore;
    private double longitudeYesterday;
    private double longitudeBefore;


    PathsHandler(@NonNull MapFragment parentClass, @NonNull MapboxMap map) {
        this.parentClass = parentClass;
        this.map = map;
        initFirestorePathRetrieval().thenAccept(this::getPathCoordinates);
    }

    @VisibleForTesting
    public static String getYesterdayPathLayerId() {
        return YESTERDAY_PATH_LAYER_ID;
    }

    @VisibleForTesting
    public List<Point> getPathCoordinatesAttribute() {
        return yesterdayPathCoordinates;
    }

    @VisibleForTesting
    public double getLatitudeYesterday() {
        return latitudeYesterday;
    }

    @VisibleForTesting
    public double getLongitudeYesterday() {
        return longitudeYesterday;
    }

    // public for now, could be package-private, depending on how we finally decide to organize files
    public void setCameraPosition(String day) {
        double lat = day.equals("yesterday") ? latitudeYesterday : latitudeBefore;
        double lon = day.equals("yesterday") ? longitudeYesterday : longitudeBefore;
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(lat, lon))
                .zoom(ZOOM)
                .build();
        if (map != null) {
            map.setCameraPosition(position);
        }
    }

    private void getPathCoordinates(Iterator<QueryDocumentSnapshot> iterator) {
        // TODO: get path for given day, NEED TO RETRIEVE POSITIONS ON SPECIFIC DAY TIME
        yesterdayPathCoordinates = new ArrayList<>();
        yesterdayInfectedMet = new ArrayList<>();

        for (; iterator.hasNext(); ) {
            QueryDocumentSnapshot qs = iterator.next();
            try {
                GeoPoint geoPoint = (GeoPoint) ((Map) qs.get("Position")).get("geoPoint");
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                Timestamp timestamp = (Timestamp) ((Map) qs.get("Position")).get(
                        "timestamp");

                if (true/*timestamp is yesterday*/) {
                    yesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, yesterdayInfectedMet);
                } else if (true/*timestamp is before yesterday*/) {
                    beforeYesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, beforeYesterdayInfectedMet);
                }
            } catch (NullPointerException e) {
                Log.d("ERROR ADDING POINT", String.valueOf(e));
            }
        }

        Log.d("PATH COORD LENGTH: ", String.valueOf(yesterdayPathCoordinates.size()));
        Log.d("IS PATH COORD NULL? ", (yesterdayPathCoordinates == null) ? "YES" : "NO");
        latitudeYesterday = yesterdayPathCoordinates.get(0).latitude();
        longitudeYesterday = yesterdayPathCoordinates.get(0).longitude();
        setPathLayer(YESTERDAY_PATH_LAYER_ID, YESTERDAY_PATH_SOURCE_ID, yesterdayPathCoordinates);
        setPathLayer(BEFORE_PATH_LAYER_ID, BEFORE_PATH_SOURCE_ID, beforeYesterdayPathCoordinates);
        setInfectedPointsLayer(YESTERDAY_PATH_LAYER_ID, YESTERDAY_PATH_SOURCE_ID, yesterdayInfectedMet);
        setInfectedPointsLayer(BEFORE_PATH_LAYER_ID, BEFORE_PATH_SOURCE_ID, beforeYesterdayInfectedMet);
    }

    private void addInfectedMet(double lat, double lon, Timestamp timestamp, List<Point> infected) {
        ConcreteDataReceiver concreteDataReceiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        Location location = LocationUtils.buildLocation(lat, lon);
        concreteDataReceiver
                .getUserNearbyDuring(location, timestamp.toDate(), timestamp.toDate())
                .thenAccept(carrierIntegerMap -> {
                    Log.d("ADD INFECTED", "got future value");
                    Carrier carrier;
                    Point point;
                    for (Map.Entry<Carrier, Integer> entry : carrierIntegerMap.entrySet()) {
                        carrier = entry.getKey();
                        if (carrier.getInfectionStatus().equals(INFECTED)) {
                            point = Point.fromLngLat(lon, lat);
                            infected.add(point);
                        }
                    }
                });
    }

    private void setPathLayer(String layerId, String sourceId, List<Point> path) {
        Layer layer = new LineLayer(layerId, sourceId).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineColor(Color.parseColor("maroon"))
        );
        LineString geometry = LineString.fromLngLats(path);
        mapStyle(layer, geometry, sourceId);
        layer.setProperties(visibility(NONE));
    }

    private void setInfectedPointsLayer(String layerId, String sourceId, List<Point> infected) {
        Layer layer = new HeatmapLayer(layerId, sourceId);
        layer.setProperties(
                adjustHeatMapColorRange(),
                adjustHeatMapWeight(),
                adjustHeatmapIntensity(),
                adjustHeatmapRadius()
        );
        MultiPoint geometry = MultiPoint.fromLngLats(infected);
        mapStyle(layer, geometry, sourceId);
        layer.setProperties(visibility(NONE));
    }

    private void mapStyle(Layer layer, Geometry geometry, String sourceId) {
        map.getStyle(style -> {
            style.addSource(new GeoJsonSource(sourceId,
                    FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(geometry)})));
            style.addLayer(layer);
        });
    }

    private CompletableFuture<Iterator<QueryDocumentSnapshot>> initFirestorePathRetrieval() {
        return FirestoreInteractor.taskToFuture(
                collectionReference("History/THAT_BETTER_PATH" + "/Positions")
                        .orderBy("Position" + ".timestamp").get())
                .thenApply(collection -> {
                    if (collection.isEmpty()) {
                        throw new RuntimeException("Collection doesn't contain any document");
                    } else {
                        return collection.iterator();
                    }
                })
                .exceptionally(e -> {
                    Toast.makeText(parentClass.getActivity(),
                            R.string.cannot_retrieve_positions,
                            Toast.LENGTH_LONG).show();
                    return Collections.emptyIterator();
                });
    }
}
