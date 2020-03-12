package ch.epfl.sdp;

import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
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
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import static ch.epfl.sdp.LocationBroker.Provider.GPS;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;

public class MapActivity extends AppCompatActivity implements LocationListener{

    private MapView mapView;
    private MapboxMap map;

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private LocationBroker locationBroker;

    private LatLng prevLocation = new LatLng(0, 0);

    CircleManager symbolManager;
    Circle symbol;

    @VisibleForTesting
    void setLocationBroker(LocationBroker testBroker) {
        locationBroker = testBroker;
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
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


// Add symbol at specified lat/lon

        if (map != null && map.getStyle() != null) {
            symbol.setLatLng(location);
            symbolManager.update(symbol);
            //symbol = symbolManager.create(new CircleOptions().withLatLng(location));
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                        symbolManager = new CircleManager(mapView, map, style);

                        symbol = symbolManager.create(new CircleOptions()
                                .withLatLng(prevLocation));

                        //initSpaceStationSymbolLayer(style);
                        updateMarkerPosition(prevLocation);
                        //Toast.makeText(SpaceStationLocationActivity.this, R.string.space_station_toast, Toast.LENGTH_SHORT).show();
                        // Map is set up and the style has loaded. Now you can add data or make other map adjustments.
                    }

                });
            }
        });
    }

    private void initSpaceStationSymbolLayer(@NonNull Style style) {
        style.addImage("space-station-icon-id",
                BitmapFactory.decodeResource(
                        this.getResources(), R.drawable.location));

        style.addSource(new GeoJsonSource("source-id"));

        style.addLayer(new SymbolLayer("layer-id", "source-id").withProperties(
                iconImage("space-station-icon-id"),
                iconIgnorePlacement(true),
                iconAllowOverlap(true),
                iconSize(.2f)
        ));
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
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
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
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    protected void OnDidFinishLoadingMapListener(MapView.OnDidFinishLoadingMapListener listener){
        mapView.addOnDidFinishLoadingMapListener(listener);
    }
}
