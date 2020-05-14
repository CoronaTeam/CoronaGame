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
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
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

import ch.epfl.sdp.Account;
import ch.epfl.sdp.AuthenticationManager;
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
    private static final String YESTERDAY_POINTS_SOURCE_ID = "points-source-one";
    private static final String BEFORE_POINTS_SOURCE_ID = "points-source-two";
    static final String YESTERDAY_POINTS_LAYER_ID = "pointslayer-one";
    static final String BEFORE_POINTS_LAYER_ID = "pointslayer-two";

    private static final String YESTERDAY_PATH_SOURCE_ID = "line-source-one";
    private static final String BEFORE_PATH_SOURCE_ID = "line-source-two";
    static final String YESTERDAY_PATH_LAYER_ID = "linelayer-one";
    static final String BEFORE_PATH_LAYER_ID = "linelayer-two";

    private List<Point> yesterdayPathCoordinates;

    private double latitudeYesterday;
    private double latitudeBefore;
    private double longitudeYesterday;
    private double longitudeBefore;

    private boolean pathLocationSet1 = false;
    private boolean pathLocationSet2 = false;

    private String yesterdayString;
    private String beforeYesterdayString;

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

        yesterdayString = dateToSimpleString(yes);//"2020/05/07"; //this is for demo only, should be replaced by: dateToSimpleString(yes);
        beforeYesterdayString = dateToSimpleString(bef);//"2020/04/27";//this is for demo only, should be replaced by: dateToSimpleString(bef);
    }

    private String dateToSimpleString(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(date);
    }

    @VisibleForTesting
    public List<Point> getYesterdayPathCoordinatesAttribute() {
        return yesterdayPathCoordinates;
    }

    @VisibleForTesting
    public String getYesterdayDate() {
        return yesterdayString;
    }

    @VisibleForTesting
    public String getBeforeYesterdayDate() {
        return beforeYesterdayString;
    }

    void setCameraPosition(int day) {
        boolean pathLocationSet = day == R.string.yesterday ? pathLocationSet1 : pathLocationSet2;
        if (pathLocationSet) {
            double lat = day == R.string.yesterday ? latitudeYesterday : latitudeBefore;
            double lon = day == R.string.yesterday ? longitudeYesterday : longitudeBefore;
            CameraPosition position = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(ZOOM)
                    .build();
            if (map != null) {
                //map.setCameraPosition(position);
                map.easeCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
            }
        }
    }

    private void getPathCoordinates(Iterator<QueryDocumentSnapshot> iterator) {
        yesterdayPathCoordinates = new ArrayList<>();
        List<Point> beforeYesterdayPathCoordinates = new ArrayList<>();
        List<Point> yesterdayInfectedMet = new ArrayList<>();
        List<Point> beforeYesterdayInfectedMet = new ArrayList<>();

        for (; iterator.hasNext(); ) {
            QueryDocumentSnapshot qs = iterator.next();
            try {
                GeoPoint geoPoint = (GeoPoint) ((Map) qs.get("Position")).get("geoPoint");
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                Timestamp timestamp = (Timestamp) ((Map) qs.get("Position")).get("timestamp");

                Date date = timestamp.toDate();
                String pathLocalDate = dateToSimpleString(date);

                if (pathLocalDate.equals(yesterdayString)) {
                    Log.d("(yesterday)DATE:", pathLocalDate);
                    yesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, yesterdayInfectedMet);
                } else if (pathLocalDate.equals(beforeYesterdayString)) {
                    Log.d("(before)DATE:", pathLocalDate);
                    beforeYesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, beforeYesterdayInfectedMet);
                }
            } catch (NullPointerException e) {
                Log.d("ERROR ADDING POINT", String.valueOf(e));
            }
        }

        Log.d("yesterday PATH COORD LENGTH: ", String.valueOf(yesterdayPathCoordinates.size()));
        Log.d("before PATH COORD LENGTH: ", String.valueOf(beforeYesterdayPathCoordinates.size()));

        if (!yesterdayInfectedMet.isEmpty()) {
            setInfectedPointsLayer(YESTERDAY_POINTS_LAYER_ID, YESTERDAY_POINTS_SOURCE_ID, yesterdayInfectedMet);
        }
        if (!beforeYesterdayInfectedMet.isEmpty()) {
            setInfectedPointsLayer(BEFORE_POINTS_LAYER_ID, BEFORE_POINTS_SOURCE_ID, beforeYesterdayInfectedMet);
        }
        if (!yesterdayPathCoordinates.isEmpty()) {
            setPathLayer(YESTERDAY_PATH_LAYER_ID, YESTERDAY_PATH_SOURCE_ID, yesterdayPathCoordinates);
            latitudeYesterday = yesterdayPathCoordinates.get(0).latitude();
            longitudeYesterday = yesterdayPathCoordinates.get(0).longitude();
            pathLocationSet1 = true;
        }
        if (!beforeYesterdayPathCoordinates.isEmpty()) {
            setPathLayer(BEFORE_PATH_LAYER_ID, BEFORE_PATH_SOURCE_ID, beforeYesterdayPathCoordinates);
            latitudeBefore = beforeYesterdayPathCoordinates.get(0).latitude();
            longitudeBefore = beforeYesterdayPathCoordinates.get(0).longitude();
            pathLocationSet2 = true;
        }

    }

    private void addInfectedMet(double lat, double lon, Timestamp timestamp, List<Point> infected) {
        ConcreteDataReceiver concreteDataReceiver = new ConcreteDataReceiver(new GridFirestoreInteractor());
        Location location = LocationUtils.buildLocation(lat, lon);
        concreteDataReceiver
                .getUserNearbyDuring(location, timestamp.toDate(), timestamp.toDate())
                .thenAccept(carrierIntegerMap -> {
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

    private String getUserName() {
        Account account = AuthenticationManager.getAccount(getActivity());
        return account.getDisplayName();
    }

    private CompletableFuture<Iterator<QueryDocumentSnapshot>> initFirestorePathRetrieval() {
        String userPath = "USER_ID_X42"; // should get path for current user: replace by getUserName()
        return FirestoreInteractor.taskToFuture(
                collectionReference("History/" + userPath + "/Positions")
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
