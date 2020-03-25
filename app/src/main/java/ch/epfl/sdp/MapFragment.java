package ch.epfl.sdp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static ch.epfl.sdp.LocationBroker.Provider.GPS;

public class MapFragment extends Fragment implements LocationListener {

    private MapView mapView;
    private MapboxMap map;

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;
    private static final int OTHER_USERS_UPDATE_INTERVAL_MILLISECS = 2500;

    private LocationBroker locationBroker;

    private LatLng prevLocation = new LatLng(0, 0);

    private HistoryFirestoreInteractor db;
    private QueryHandler fireBaseHandler;

    private CircleManager positionMarkerManager;
    private Circle userLocation;
    private ArrayList<Circle> otherUsersPositionMarkers;

    private Timer updateOtherPosTimer;

    @Override
    public void onLocationChanged(Location newLocation) {
        if (locationBroker.hasPermissions(GPS)) {
            prevLocation =  new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
            updateMarkerPosition(prevLocation);
            System.out.println("new location");

            Map<String, PositionRecord> element = new HashMap<>();
            element.put("Position", new PositionRecord(Timestamp.now(), new GeoPoint(newLocation.getLatitude(), newLocation.getLongitude())));
            db.write(element, o -> { }, e -> { });
        } else {
            Toast.makeText(getActivity(), "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void updateMarkerPosition(LatLng location) {
        // This method is where we update the marker position once we have new coordinates. First we
        // check if this is the first time we are executing this handler, the best way to do this is
        // check if marker is null

        if (map != null && map.getStyle() != null) {
            userLocation.setLatLng(location);
            positionMarkerManager.update(userLocation);
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void goOnline() {
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
        Toast.makeText(getActivity(), R.string.gps_status_on, Toast.LENGTH_SHORT).show();
    }

    private void goOffline() {
        Toast.makeText(getActivity(), R.string.gps_status_off, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (locationBroker.hasPermissions(GPS)) {
                goOnline();
            } else {
                locationBroker.requestPermissions(LOCATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            goOffline();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goOnline();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // TODO: do not execute in production code
        if (locationBroker == null) {
            locationBroker = new ConcreteLocationBroker((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE), getActivity());
        }
        otherUsersPositionMarkers = new ArrayList<>();

        FirestoreWrapper wrapper = new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance());
        db = new HistoryFirestoreInteractor(wrapper, AccountGetting.getAccount(getActivity()));

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(getContext(), BuildConfig.mapboxAPIKey);

        // This contains the MapView in XML and needs to be called after the access token is configured.
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                map = mapboxMap;

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        positionMarkerManager = new CircleManager(mapView, map, style);

                        userLocation = positionMarkerManager.create(new CircleOptions()
                                .withLatLng(prevLocation));

                        updateMarkerPosition(prevLocation);
                        initFireBaseQueryHandler();
                    }

                });
            }
        });

        return view;
    }

    private void initFireBaseQueryHandler() {

        fireBaseHandler = new QueryHandler() {

            @Override
            public void onSuccess(QuerySnapshot snapshot) {

                /* The idea here is to reuse the Circle objects to not recreate the datastructure from
                scratch on each update. It's now overkill but will be usefull for the heatmaps
                It's also necessary to keep the Circle objects around because recreating them each time
                there is new data make the map blink like a christmas tree
                 */

                Iterator<QueryDocumentSnapshot> qsIterator = snapshot.iterator(); // data from firebase
                Iterator<Circle> pmIterator = otherUsersPositionMarkers.iterator(); // local list of position marker

                // update the Arraylist contents first
                updatePositionMarkersList(qsIterator, pmIterator);
                // Run if there is more elements than in the last run
                addMarkersToMarkerList(qsIterator);

                //refresh map data
                positionMarkerManager.update(otherUsersPositionMarkers);
            }

            @Override
            public void onFailure() {
                Toast.makeText(getActivity(), "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
            }

            private void updatePositionMarkersList(Iterator<QueryDocumentSnapshot> qsIterator, Iterator<Circle> pmIterator){
                while (pmIterator.hasNext()){
                    if(qsIterator.hasNext()){
                        QueryDocumentSnapshot qs = qsIterator.next();
                        Circle pm = pmIterator.next();
                        System.out.println(qs);

                        try {
                            pm.setLatLng(new LatLng(((GeoPoint)(qs.get("geoPoint"))).getLatitude(),
                                    ((GeoPoint)(qs.get("geoPoint"))).getLongitude()));
                        } catch (NullPointerException ignored) { }

                    }
                    else{ // if some points were deleted remove them from the list
                        positionMarkerManager.delete(pmIterator.next());
                        pmIterator.remove();
                    }
                }
            }

            private void addMarkersToMarkerList(Iterator<QueryDocumentSnapshot> qsIterator){
                while (qsIterator.hasNext()){
                    QueryDocumentSnapshot qs = qsIterator.next();
                    try {
                        Circle pm = positionMarkerManager.create(new CircleOptions()
                                .withLatLng(new LatLng(
                                        ((GeoPoint)(qs.get("geoPoint"))).getLatitude(),
                                        ((GeoPoint)(qs.get("geoPoint"))).getLongitude()))
                                .withCircleColor("#ff6219")
                        );
                        otherUsersPositionMarkers.add(pm);
                    } catch (NullPointerException ignored) { }

                }
            }
        };

        startTimer();
    }

    private void startTimer(){
        class UpdatePosTask extends TimerTask {

            public void run() {
                if (db != null && fireBaseHandler != null){
                    db.readLastPositions(fireBaseHandler);
                }
            }
        }
        if(updateOtherPosTimer != null){
            updateOtherPosTimer.cancel();
        }
        updateOtherPosTimer = new Timer();
        updateOtherPosTimer.scheduleAtFixedRate(new UpdatePosTask(), 0, OTHER_USERS_UPDATE_INTERVAL_MILLISECS);
    }

    private void stopTimer(){
        updateOtherPosTimer.cancel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (locationBroker.isProviderEnabled(GPS) && locationBroker.hasPermissions(GPS)) {
            goOnline();
        } else if (locationBroker.isProviderEnabled(GPS)) {
            // Must ask for permissions
            locationBroker.requestPermissions(LOCATION_PERMISSION_REQUEST);
        } else {
            goOffline();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    void OnDidFinishLoadingMapListener(MapView.OnDidFinishLoadingMapListener listener){
        mapView.addOnDidFinishLoadingMapListener(listener);
    }
}
