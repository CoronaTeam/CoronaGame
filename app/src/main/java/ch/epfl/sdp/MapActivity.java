package ch.epfl.sdp;

import android.os.Bundle;

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
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import static ch.epfl.sdp.LocationBroker.Provider.GPS;

public class MapActivity extends AppCompatActivity implements LocationListener{

    private MapView mapView;
    private MapboxMap map;
    private LocationBroker locationBroker;
    private LatLng prevLocation = new LatLng(0, 0);

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;
    private static final int OTHER_USERS_UPDATE_INTERVAL_MILLISECS = 2500;

    private FirestoreInteractor db;
    private QueryHandler fireBaseHandler;

    CircleManager positionMarkerManager;
    Circle userPositionMarker;
    ArrayList<Circle> otherUsersPositionMarkers;

    Timer updateOtherPosTimer;

    MapActivity classPointer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        classPointer = this;
        otherUsersPositionMarkers = new ArrayList<>();
        FirestoreWrapper firestoreWrapper = new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance());
        db = new HistoryFirestoreInteractor(firestoreWrapper);

        // TODO: do not execute in production code
        if (locationBroker == null) {
            locationBroker = new ConcreteLocationBroker((LocationManager) getSystemService(Context.LOCATION_SERVICE), this);
        }

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, BuildConfig.mapboxAPIKey);

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_map);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                map = mapboxMap;

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        positionMarkerManager = new CircleManager(mapView, map, style);

                        userPositionMarker = positionMarkerManager.create(new CircleOptions()
                                .withLatLng(prevLocation));

                        updateMarkerPosition(prevLocation);
                        initFireBaseQueryHandler();
                    }

                });
            }
        });
    }

    private void initFireBaseQueryHandler() {

        fireBaseHandler = new QueryHandler() {
            Iterator<QueryDocumentSnapshot> qsIterator;
            Iterator<Circle> pmIterator;

            @Override
            public void onSuccess(QuerySnapshot snapshot) {

                /* The idea here is to reuse the Circle objects to not recreate the datastructure from
                scratch on each update. It's now overkill but will be usefull for the heatmaps
                It's also necessary to keep the Circle objects around because recreating them each time
                there is new data make the map blink like a christmas tree
                 */

                qsIterator = snapshot.iterator(); // data from firebase
                pmIterator = otherUsersPositionMarkers.iterator(); // local list of position marker

                // update the Arraylist contents first
                updatePositionMarkersList();
                // Run if there is new elements
                addMarkersToMarkerList();

                //refresh map data
                positionMarkerManager.update(otherUsersPositionMarkers);
            }

            @Override
            public void onFailure() {
                Toast.makeText(classPointer, "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
            }

            private void updatePositionMarkersList(){
                while (pmIterator.hasNext()){
                    if(qsIterator.hasNext()){
                        QueryDocumentSnapshot qs = qsIterator.next();
                        Circle pm = pmIterator.next();
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

            private void addMarkersToMarkerList(){
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

    @Override
    public void onLocationChanged(Location location) {
        if (locationBroker.hasPermissions(GPS)) {
            prevLocation =  new LatLng(location.getLatitude(), location.getLongitude());
            updateMarkerPosition(prevLocation);
            System.out.println("new location");
        } else {
            Toast.makeText(this, "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void updateMarkerPosition(LatLng location) {
        // This method is where we update the marker position once we have new coordinates. First we
        // check if this is the first time we are executing this handler, the best way to do this is
        // check if marker is null

        if (map != null && map.getStyle() != null) {
            userPositionMarker.setLatLng(location);
            positionMarkerManager.update(userPositionMarker);
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        startTimer();
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
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        startTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        stopTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        stopTimer();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void goOnline() {
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
        Toast.makeText(this, R.string.gps_status_on, Toast.LENGTH_SHORT).show();
    }

    private void goOffline() {
        Toast.makeText(this, R.string.gps_status_off, Toast.LENGTH_LONG).show();
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

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    protected void OnDidFinishLoadingMapListener(MapView.OnDidFinishLoadingMapListener listener){
        mapView.addOnDidFinishLoadingMapListener(listener);
    }

    private void startTimer(){
        class UpdatePosTask extends TimerTask {

            public void run() {
                if (db != null && fireBaseHandler != null){
                    db.read(fireBaseHandler);
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
}
