package ch.epfl.sdp.Map;

import android.annotation.SuppressLint;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
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
    static final String YESTERDAY_PATH_LAYER_ID = "linelayer-one";
    static final String BEFORE_PATH_LAYER_ID = "linelayer-two";

    public List<Point> yesterdayPathCoordinates;
    public List<Point> beforeYesterdayPathCoordinates;

    public List<Point> yesterdayInfectedMet;
    public List<Point> beforeYesterdayInfectedMet;

    private double latitudeYesterday;
    private double latitudeBefore;
    private double longitudeYesterday;
    private double longitudeBefore;
    private boolean pathLocationSet = false;

    private String yesterday;
    private String beforeYesterday;

    private static final int ZOOM = 13;
    private MapboxMap map;
    private MapFragment parentClass;


    PathsHandler(@NonNull MapFragment parentClass, @NonNull MapboxMap map) {
        this.parentClass = parentClass;
        this.map = map;
        setCalendar();
        initFirestorePathRetrieval().thenAccept(this::getPathCoordinates);
    }

    private void setCalendar() {
        Date rightNow = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(rightNow);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yes = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date bef = cal.getTime();

        yesterday = "2020/05/07"; //this is for demo only, should be replaced by: dateToSimpleString(yes);
        beforeYesterday = "2020/04/27";//this is for demo only, should be replaced by: dateToSimpleString(bef);
    }

    private String dateToSimpleString(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(date);
    }

    @VisibleForTesting
    public static String getYesterdayPathLayerId() {
        return YESTERDAY_PATH_LAYER_ID;
    }

    @VisibleForTesting
    public List<Point> getYesterdayPathCoordinatesAttribute() {
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

    void setCameraPosition(String day) {
        if (pathLocationSet) {
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
    }

    private void getPathCoordinates(Iterator<QueryDocumentSnapshot> iterator) {
        yesterdayPathCoordinates = new ArrayList<>();
        beforeYesterdayPathCoordinates = new ArrayList<>();
        yesterdayInfectedMet = new ArrayList<>();
        beforeYesterdayInfectedMet = new ArrayList<>();

        for (; iterator.hasNext(); ) {
            QueryDocumentSnapshot qs = iterator.next();
            try {
                GeoPoint geoPoint = (GeoPoint) ((Map) qs.get("Position")).get("geoPoint");
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                Timestamp timestamp = (Timestamp) ((Map) qs.get("Position")).get("timestamp");

                Date date = timestamp.toDate();
                String pathLocalDate = dateToSimpleString(date);
                Log.d("DATE:", pathLocalDate);

                if (pathLocalDate.equals(yesterday)) {
                    yesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, yesterdayInfectedMet);
                } else if (pathLocalDate.equals(beforeYesterday)) {
                    beforeYesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, beforeYesterdayInfectedMet);
                }
            } catch (NullPointerException e) {
                Log.d("ERROR ADDING POINT", String.valueOf(e));
            }
        }

        Log.d("PATH COORD LENGTH: ", String.valueOf(yesterdayPathCoordinates.size()));
        Log.d("IS PATH COORD NULL? ", (yesterdayPathCoordinates == null) ? "YES" : "NO");

        setUpPath(yesterdayPathCoordinates);
        setUpPath(beforeYesterdayPathCoordinates);

        if (!yesterdayInfectedMet.isEmpty()) {
            setInfectedPointsLayer(YESTERDAY_POINTS_LAYER_ID, YESTERDAY_POINTS_SOURCE_ID, yesterdayInfectedMet);
        }
        if (!beforeYesterdayInfectedMet.isEmpty()) {
            setInfectedPointsLayer(BEFORE_POINTS_LAYER_ID, BEFORE_POINTS_SOURCE_ID, beforeYesterdayInfectedMet);
        }
        if (!yesterdayPathCoordinates.isEmpty()) {
            latitudeYesterday = yesterdayPathCoordinates.get(0).latitude();
            longitudeYesterday = yesterdayPathCoordinates.get(0).longitude();
            pathLocationSet = true;
        }
        if (!beforeYesterdayPathCoordinates.isEmpty()) {
            latitudeBefore = beforeYesterdayPathCoordinates.get(0).latitude();
            longitudeBefore = beforeYesterdayPathCoordinates.get(0).longitude();
            pathLocationSet = true;
        }

    }

    private void setUpPath(List<Point> pathCoordinates) {
        String layerId = pathCoordinates.equals(yesterdayPathCoordinates) ? YESTERDAY_PATH_LAYER_ID : BEFORE_PATH_LAYER_ID;
        String sourceId = pathCoordinates.equals(yesterdayPathCoordinates) ? YESTERDAY_PATH_SOURCE_ID : BEFORE_PATH_SOURCE_ID;
        if (!pathCoordinates.isEmpty()) {
            setPathLayer(layerId, sourceId, pathCoordinates);
        } else {
            Toast.makeText(parentClass.getActivity(),
                    R.string.no_corresponding_path,
                    Toast.LENGTH_LONG).show();
        }
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
