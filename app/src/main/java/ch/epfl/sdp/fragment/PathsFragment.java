package ch.epfl.sdp.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.epfl.sdp.BuildConfig;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.ConcreteDataReceiver;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

import static com.google.firebase.firestore.Source.CACHE;

/**
 * This fragment is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsFragment extends Fragment {
    private MapView mapView;
    public MapboxMap map; // made public for testing
    public List<Point> pathCoordinates; // made public for testing
    private FirebaseFirestore db = FirebaseFirestore.getInstance(); // we don't use FirestoreInteractor because we want to do more specific op
    //public QueryHandler firestoreQueryHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Mapbox access token is configured here.
        Mapbox.getInstance(getContext(), BuildConfig.mapboxAPIKey);

        View view = inflater.inflate(R.layout.fragment_paths, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this:: onMapReady);

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
        initFirestorePathRetrieval(value -> getPathCoordinates(value));
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

    private void initFirestorePathRetrieval(Callback<Iterator<QueryDocumentSnapshot>> callback) {

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
/*
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
    }*/

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        System.err.println("stop");
        mapView.onStop();
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        System.err.println("resume");
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        System.err.println("pause");
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
        System.err.println("lowmem");
    }

    @Override
    public void onDestroy() {
        System.err.println("destroy");
        mapView.onDestroy();
        super.onDestroy();

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        System.err.println("sis");
        mapView.onSaveInstanceState(outState);
    }
}
