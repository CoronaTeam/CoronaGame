package ch.epfl.sdp.Map;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
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

import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.BuildConfig;
import ch.epfl.sdp.R;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.fragment.AccountFragment;
import ch.epfl.sdp.fragment.HistoryDialogFragment;
import ch.epfl.sdp.location.LocationBroker;
import ch.epfl.sdp.location.LocationService;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;

public class MapFragment extends Fragment implements LocationListener, View.OnClickListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;
    private MapView mapView;
    private MapboxMap map;
    private LocationBroker locationBroker;

    private LatLng prevLocation = new LatLng(0, 0);

    private ConcreteFirestoreInteractor db;

    private CircleManager positionMarkerManager;
    private Circle userLocation;

    private HeatMapHandler heatMapHandler;

    private Account userAccount;
    private MapFragment classPointer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        classPointer = this;

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                locationBroker = ((LocationService.LocationBinder)service).getService().getBroker();
                goOnline();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // TODO: Check in code that the service does not become null
                locationBroker = null;
            }
        };

        getActivity().bindService(new Intent(getContext(), LocationService.class), conn, Context.BIND_AUTO_CREATE);

        userAccount = AccountFragment.getAccount(getActivity());

        db = new ConcreteFirestoreInteractor();

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

                        updateUserMarkerPosition(prevLocation);
                        heatMapHandler = new HeatMapHandler(classPointer, db, style);
                    }

                });
            }
        });

        view.findViewById(R.id.history_button).setOnClickListener(this);

        return view;
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        if (locationBroker.hasPermissions(GPS)) {
            prevLocation = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
            updateUserMarkerPosition(prevLocation);

        } else {
            Toast.makeText(getActivity(), "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void updateUserMarkerPosition(LatLng location) {
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void goOnline() {
        if (locationBroker.isProviderEnabled(GPS) && locationBroker.hasPermissions(GPS)) {
            locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
        } else if (locationBroker.isProviderEnabled(GPS)) {
            // Must ask for permissions
            locationBroker.requestPermissions(getActivity(), LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        goOnline();
    }

    @Override
    public void onProviderDisabled(String provider) {
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

    void OnDidFinishLoadingMapListener(MapView.OnDidFinishLoadingMapListener listener) {
        mapView.addOnDidFinishLoadingMapListener(listener);
    }

    protected LatLng getPreviousLocation() {
        return prevLocation;
    }

    private void onClickHistory() {
        HistoryDialogFragment dialog = HistoryDialogFragment.newInstance();
        dialog.show(getActivity().getSupportFragmentManager(), "history_dialog_fragment");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.history_button: {
                onClickHistory();
            } break;
            default: break;
        }
    }
}
