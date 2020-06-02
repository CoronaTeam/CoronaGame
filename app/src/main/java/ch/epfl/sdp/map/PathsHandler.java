package ch.epfl.sdp.map;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;

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
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
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
import ch.epfl.sdp.contamination.databaseIO.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.databaseIO.GridFirestoreInteractor;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.identity.Account;
import ch.epfl.sdp.identity.AuthenticationManager;
import ch.epfl.sdp.location.LocationUtils;
import ch.epfl.sdp.map.fragment.MapFragment;

import static ch.epfl.sdp.contamination.Carrier.InfectionStatus.INFECTED;
import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.HISTORY_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.HISTORY_POSITIONS_COLL;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMESTAMP_TAG;
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
    private static final String YESTERDAY_INFECTED_SOURCE_ID = "points-source-one";
    private static final String BEFORE_INFECTED_SOURCE_ID = "points-source-two";
    public static final String YESTERDAY_INFECTED_LAYER_ID = "pointslayer-one";
    public static final String BEFORE_INFECTED_LAYER_ID = "pointslayer-two";

    private static final String YESTERDAY_PATH_SOURCE_ID = "line-source-one";
    private static final String BEFORE_PATH_SOURCE_ID = "line-source-two";
    public static final String YESTERDAY_PATH_LAYER_ID = "linelayer-one";
    public static final String BEFORE_PATH_LAYER_ID = "linelayer-two";

    private List<Point> yesterdayPathCoordinates;
    private List<Point> beforeYesterdayPathCoordinates;
    private List<Point> yesterdayInfectedMet;
    private List<Point> beforeYesterdayInfectedMet;

    private double latitudeYesterday;
    private double latitudeBefore;
    private double longitudeYesterday;
    private double longitudeBefore;

    private LatLngBounds yesterdayLLB;
    private LatLngBounds beforeLLB;

    private boolean pathLocationSet1; // yesterday
    private boolean pathLocationSet2; // before yesterday

    private String yesterdayString;
    private String beforeYesterdayString;

    private static final int ZOOM = 13;
    private MapboxMap map;
    private MapFragment parentClass;
    private ConcreteDataReceiver concreteDataReceiver = new ConcreteDataReceiver(
            new GridFirestoreInteractor());

    public PathsHandler(@NonNull MapFragment parentClass, @NonNull MapboxMap map) {
        this.parentClass = parentClass;
        this.map = map;
        setCalendar();
        initFirestorePathRetrieval().thenAccept(this::getPathCoordinates);
    }

    private CompletableFuture<Iterator<QueryDocumentSnapshot>> initFirestorePathRetrieval() {
        String userPath = getUserId(); //"USER_ID_X42"; coronaId: 109758096484534641167
        return FirestoreInteractor.taskToFuture(
                collectionReference(HISTORY_COLL + userPath + HISTORY_POSITIONS_COLL)
                        .orderBy(TIMESTAMP_TAG).get())
                .thenApply(collection -> {
                    if (collection.isEmpty()) {
                        throw new RuntimeException("Collection doesn't contain any document");
                    } else {
                        return collection.iterator();
                    }
                })
                .exceptionally(e -> Collections.emptyIterator());
    }

    private void getPathCoordinates(Iterator<QueryDocumentSnapshot> iterator) {
        initLists();

        if (TEST_NON_EMPTY_LIST) {
            fakeInitialization();
            setLayers();
            return;
        }
        if (TEST_EMPTY_PATH) {
            return;
        }

        for (; iterator.hasNext(); ) {
            QueryDocumentSnapshot qs = iterator.next();
            try {
                GeoPoint geoPoint = (GeoPoint) ((Map) qs.get("Position")).get(GEOPOINT_TAG);
                double lat = geoPoint.getLatitude();
                double lon = geoPoint.getLongitude();
                Timestamp timestamp = (Timestamp) ((Map) qs.get("Position")).get(TIMESTAMP_TAG);

                String pathLocalDate = dateToSimpleString(timestamp.toDate());

                if (pathLocalDate.equals(yesterdayString)) {
                    yesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, yesterdayInfectedMet);
                } else if (pathLocalDate.equals(beforeYesterdayString)) {
                    beforeYesterdayPathCoordinates.add(Point.fromLngLat(lon, lat));
                    addInfectedMet(lat, lon, timestamp, beforeYesterdayInfectedMet);
                }
            } catch (NullPointerException ignored) {
            }
        }
        setLayers();
    }

    private void setCalendar() {
        Date rightNow = new Date(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(rightNow);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yes = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date bef = cal.getTime();

        yesterdayString = dateToSimpleString(yes);//"2020/05/13"; //this is for demo only, should be replaced by: dateToSimpleString(yes);
        beforeYesterdayString = dateToSimpleString(bef);//"2020/05/12";//this is for demo only, should be replaced by: dateToSimpleString(bef);
    }

    private String dateToSimpleString(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf =
                new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(date);
    }

    public void setCameraPosition(int day) {
        boolean pathLocationSet = day == R.string.yesterday ? pathLocationSet1 : pathLocationSet2;
        if (pathLocationSet) {
            double lat = day == R.string.yesterday ? latitudeYesterday : latitudeBefore;
            double lon = day == R.string.yesterday ? longitudeYesterday : longitudeBefore;
            CameraPosition position = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(ZOOM)
                    .build();
            if (map != null) {
                map.easeCamera(CameraUpdateFactory.newCameraPosition(position), 2000);
            }
        }
    }

    public void seeWholePath(int day) {
        LatLngBounds latLngBounds = day == R.string.yesterday ? yesterdayLLB : beforeLLB;

        if (map != null) {
            map.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100), 2000);
        }
    }

    private LatLngBounds setLatLngBounds(int day) {
        List<Point> pathCoord = day == R.string.yesterday ? yesterdayPathCoordinates :
                beforeYesterdayPathCoordinates;
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (int i = 0; i<pathCoord.size(); ++i) {
            double lat = pathCoord.get(i).latitude();
            double lon = pathCoord.get(i).longitude();
            boundsBuilder.include(new LatLng(lat, lon));
        }
        return boundsBuilder.build();
    }

    private void initLists() {
        yesterdayPathCoordinates = new ArrayList<>();
        beforeYesterdayPathCoordinates = new ArrayList<>();
        yesterdayInfectedMet = new ArrayList<>();
        beforeYesterdayInfectedMet = new ArrayList<>();
    }

    private void setLayers() {
        setInfectedLayerIfNotEmpty(yesterdayInfectedMet, YESTERDAY_INFECTED_LAYER_ID, YESTERDAY_INFECTED_SOURCE_ID);
        setInfectedLayerIfNotEmpty(beforeYesterdayInfectedMet, BEFORE_INFECTED_LAYER_ID, BEFORE_INFECTED_SOURCE_ID);

        if (!yesterdayPathCoordinates.isEmpty()) {
            setPathLayer(YESTERDAY_PATH_LAYER_ID, YESTERDAY_PATH_SOURCE_ID, yesterdayPathCoordinates);
            latitudeYesterday = yesterdayPathCoordinates.get(0).latitude();
            longitudeYesterday = yesterdayPathCoordinates.get(0).longitude();
            yesterdayLLB = setLatLngBounds(R.string.yesterday);
            pathLocationSet1 = true;
        }
        if (!beforeYesterdayPathCoordinates.isEmpty()) {
            setPathLayer(BEFORE_PATH_LAYER_ID, BEFORE_PATH_SOURCE_ID, beforeYesterdayPathCoordinates);
            latitudeBefore = beforeYesterdayPathCoordinates.get(0).latitude();
            longitudeBefore = beforeYesterdayPathCoordinates.get(0).longitude();
            beforeLLB = setLatLngBounds(R.string.before_yesterday);
            pathLocationSet2 = true;
        }
    }

    private void setInfectedLayerIfNotEmpty(List<Point> infectedMet, String infectedLayerId, String infectedSourceId) {
        if (!infectedMet.isEmpty()) {
            setInfectedPointsLayer(infectedLayerId, infectedSourceId,
                    infectedMet);
        }
    }

    private void addInfectedMet(double lat, double lon, Timestamp timestamp, List<Point> infected) {
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
    }

    private void mapStyle(Layer layer, Geometry geometry, String sourceId) {
        layer.setProperties(visibility(NONE));
        map.getStyle(style -> {
            style.addSource(new GeoJsonSource(sourceId,
                    FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(geometry)})));
            style.addLayer(layer);
        });
    }

    private String getUserId() {
        Account account = AuthenticationManager.getAccount(getActivity());
        return account.getId();
    }

    private void fakeInitialization() {
        for (double i = 0; i < 10; ++i) {
            yesterdayPathCoordinates.add(Point.fromLngLat(i, i));
            if (i % 3 == 0) {
                yesterdayInfectedMet.add(Point.fromLngLat(i, i));
            }
        }
        pathLocationSet1 = true;
    }

    @VisibleForTesting
    public static boolean TEST_EMPTY_PATH;

    @VisibleForTesting
    public static boolean TEST_NON_EMPTY_LIST;

    @VisibleForTesting
    public List<Point> getYesterdayPathCoordinates() {
        return yesterdayPathCoordinates;
    }

    @VisibleForTesting
    public List<Point> getYesterdayInfectedMet() {
        return yesterdayInfectedMet;
    }

    @VisibleForTesting
    public double getLatitudeYesterday() {
        return latitudeYesterday;
    }

    @VisibleForTesting
    public double getLongitudeYesterday() {
        return longitudeYesterday;
    }

    @VisibleForTesting
    public boolean isPathLocationSet1() {
        return pathLocationSet1;
    }

    @VisibleForTesting
    public String getSimpleDateFormat(Date date) {
        return dateToSimpleString(date);
    }

}
