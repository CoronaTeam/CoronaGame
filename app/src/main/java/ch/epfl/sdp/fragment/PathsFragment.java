package ch.epfl.sdp.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.epfl.sdp.BuildConfig;
import ch.epfl.sdp.R;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

/**
 * This fragment is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsFragment extends Fragment {
    private MapView mapView;
    public MapboxMap map; // made public for testing
    public List<Point> pathCoordinates; // made public for testing
    public Iterator<QueryDocumentSnapshot> qsIterator;
    public ConcreteFirestoreInteractor cfi = new ConcreteFirestoreInteractor();
    public QueryHandler firestoreQueryHandler;
    public List<String> test;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Mapbox access token is configured here.
        Mapbox.getInstance(getContext(), BuildConfig.mapboxAPIKey);

        View view = inflater.inflate(R.layout.fragment_paths, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this::onMapReady);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
    }

    private void getPathCoordinates(@NonNull Iterator<QueryDocumentSnapshot> qsIterator) {
        // TODO: RETRIEVE FROM CACHE IF AVAILABLE
        // TODO: get path for given day
        // CREATE FAKE FIRESTORE TO RETRIEVE FOR DEMO IF NEEDED
        // NEED TO RETRIEVE POSITIONS ON SPECIFIC DAY TIME
        pathCoordinates = new ArrayList<>();

        for (; qsIterator.hasNext(); ) {
            QueryDocumentSnapshot qs = qsIterator.next();
            try {
                pathCoordinates.add(Point.fromLngLat(((GeoPoint) (qs.get("geoPoint"))).getLongitude(),
                        ((GeoPoint) (qs.get("geoPoint"))).getLatitude()
                ));
            } catch (NullPointerException ignored) {
            }
        }

        setCameraPosition(pathCoordinates.get(0).latitude(), pathCoordinates.get(0).longitude());
        setMapStyle(map);
    }

    private void setCameraPosition(double latitude, double longitude) {
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(11)
                .build();
        if (map != null) {
            map.setCameraPosition(position);
        }
    }

    private void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        //getPathCoordinates();
        initFirestoreRetrieval();
    }

    private void setMapStyle(MapboxMap mapboxMap) {
        mapboxMap.setStyle(Style.OUTDOORS, style -> {

            // Create the LineString from the list of coordinates and then make a GeoJSON
            // FeatureCollection so we can add the line to our map as a layer.
            style.addSource(new GeoJsonSource("line-source",
                    FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(
                            LineString.fromLngLats(pathCoordinates)
                    )})));

            // The layer properties for our line. This is where we make the line dotted, set the
            // color, etc.
            style.addLayer(new LineLayer("linelayer", "line-source").withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineColor(Color.parseColor("maroon"))
            ));
        });
    }

    private void initFirestoreRetrieval() {

        firestoreQueryHandler = new QueryHandler<QuerySnapshot>() {

            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                qsIterator = snapshot.iterator();
                getPathCoordinates(qsIterator);
            }

            @Override
            public void onFailure() {

            }
        };
        cfi.readCollection("History/USER_PATH_DEMO/Positions", firestoreQueryHandler);
    }

    private QueryHandler getQueryHandler() {
        return new QueryHandler<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                qsIterator = snapshot.iterator();
                getPathCoordinates(qsIterator);
            }
            @Override
            public void onFailure() {
                //Toast.makeText(parentClass.getActivity(), "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
            }
        };
    }
}
