package ch.epfl.sdp.map.fragment;

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
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionButton;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionHelper;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionLayout;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RFACLabelItem;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RapidFloatingActionContentLabelList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import ch.epfl.sdp.BuildConfig;
import ch.epfl.sdp.R;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.location.LocationBroker;
import ch.epfl.sdp.location.LocationService;
import ch.epfl.sdp.map.HeatMapHandler;
import ch.epfl.sdp.map.PathsHandler;

import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;
import static ch.epfl.sdp.map.HeatMapHandler.HEATMAP_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.BEFORE_INFECTED_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.BEFORE_PATH_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_INFECTED_LAYER_ID;
import static ch.epfl.sdp.map.PathsHandler.YESTERDAY_PATH_LAYER_ID;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * Instantiate one instance of MapBox
 * Used to display the pathLayers and heatMapLayer
 * Add listeners to update the user's position on the map and react on floating button click
 */
public class MapFragment extends Fragment implements LocationListener, View.OnClickListener, RapidFloatingActionContentLabelList.OnRapidFloatingActionContentLabelListListener {

    //TODO: some constant are dupicated from locationService
    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;
    private PathsHandler pathsHandler;
    private MapView mapView;
    private MapboxMap map;
    private LocationBroker locationBroker;
    private LatLng prevLocation = new LatLng(0, 0);
    private ConcreteFirestoreInteractor db;
    private CircleManager positionMarkerManager;
    private Circle userLocation;
    private HeatMapHandler heatMapHandler;
    private MapFragment classPointer;
    private ServiceConnection conn;
    private Callable onMapVisible;

    private RapidFloatingActionHelper rfabHelper;

    private View view;

    @VisibleForTesting
    static boolean TESTING_MODE;

    @VisibleForTesting
    public void onLayerLoaded(Callable func, String layerId) {
        map.getStyle(style -> {
            if (style.getLayer(layerId) != null) {
                callDataLoaded(func);
            }
        });
    }

    private void callDataLoaded(Callable func) {
        try {
            func.call();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        classPointer = this;

        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                locationBroker = ((LocationService.LocationBinder) service).getService().getBroker();
                goOnline();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // TODO: Check in code that the service does not become null
                locationBroker = null;
            }
        };

        // startService() overrides the default service lifetime that is managed by
        // bindService(Intent, ServiceConnection, int):
        // it requires the service to remain running until stopService(Intent) is called,
        // regardless of whether any clients are connected to it.
        ComponentName myService = getActivity().startService(new Intent(getContext(), LocationService.class));
        getActivity().bindService(new Intent(getContext(), LocationService.class), conn, Context.BIND_AUTO_CREATE);

        db = new ConcreteFirestoreInteractor();

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(getContext(), BuildConfig.mapboxAPIKey);

        // This contains the MapView in XML and needs to be called after the access token is configured.
        view = inflater.inflate(R.layout.fragment_map, container, false);

        view.findViewById(R.id.mapFragment).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.heatMapToggle).setVisibility(View.GONE);

        mapView = view.findViewById(R.id.mapFragment);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;

            mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
                positionMarkerManager = new CircleManager(mapView, map, style);

                userLocation = positionMarkerManager.create(new CircleOptions()
                        .withLatLng(prevLocation));

                updateUserMarkerPosition(prevLocation);
                heatMapHandler = new HeatMapHandler(classPointer, db, map);
                pathsHandler = new PathsHandler(classPointer, map);
            });
        });


        view.findViewById(R.id.heatMapToggle).setOnClickListener(this);
        setHistoryRFAButton();

        return view;
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        if (locationBroker.hasPermissions(GPS)) {
            prevLocation = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
            updateUserMarkerPosition(prevLocation);

            view.findViewById(R.id.mapFragment).setVisibility(View.VISIBLE);
            view.findViewById(R.id.heatMapToggle).setVisibility(View.VISIBLE);
            view.findViewById(R.id.heapMapLoadingSpinner).setVisibility(View.GONE);
            view.findViewById(R.id.wholePath).setVisibility((View.INVISIBLE));

            callOnMapVisible();
        } else {
            Toast.makeText(getActivity(), "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void updateUserMarkerPosition(LatLng location) {
        // This method is where we update the marker position once we have new coordinates. First we
        // check if this is the first time we are executing this handler, the best way to do this is
        // check if marker is null

        if (map != null && map.getStyle() != null) {
            userLocation.setLatLng(prevLocation);
            positionMarkerManager.update(userLocation);
            map.animateCamera(CameraUpdateFactory.newLatLng(location));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();

        // Unbind service
        if (conn != null) {
            getActivity().unbindService(conn);
        }

        super.onDestroy();

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.heatMapToggle) {
            toggleHeatMap();
        } else if (view.getId() == R.id.wholePath) {
            //seeWholePath();
        }
    }

    private void toggleHeatMap() {
        toggleLayer(HEATMAP_LAYER_ID);
    }

    private void toggleLayer(String layerId) {
        map.getStyle(style -> {
            Layer layer = style.getLayer(layerId);
            if (layer != null) {
                if (VISIBLE.equals(layer.getVisibility().getValue())) {
                    layer.setProperties(visibility(NONE));
                    if (!layerId.equals(HEATMAP_LAYER_ID) && View.VISIBLE == view.findViewById(R.id.wholePath).getVisibility()) {
                        view.findViewById(R.id.wholePath).setVisibility(View.INVISIBLE);
                    }
                } else {
                    layer.setProperties(visibility(VISIBLE));
                    // button to see whole current path: appear when pressing to see some path, disappearing when pressing again
                    if (!layerId.equals(HEATMAP_LAYER_ID) && View.INVISIBLE == view.findViewById(R.id.wholePath).getVisibility()) {
                        view.findViewById(R.id.wholePath).setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void togglePath(int day) {
        String pathLayerId = day == R.string.yesterday ? YESTERDAY_PATH_LAYER_ID : BEFORE_PATH_LAYER_ID;
        String infectedLayerId = day == R.string.yesterday ? YESTERDAY_INFECTED_LAYER_ID : BEFORE_INFECTED_LAYER_ID;
        toggleLayer(pathLayerId);
        toggleLayer(infectedLayerId);
        pathsHandler.setCameraPosition(day);
    }


    private void setHistoryRFAButton() {
        RapidFloatingActionLayout rfaLayout = view.findViewById(R.id.history_rfal);
        RapidFloatingActionButton rfaBtn = view.findViewById(R.id.history_rfab);

        RapidFloatingActionContentLabelList rfaContent = new RapidFloatingActionContentLabelList(getContext());
        rfaContent.setOnRapidFloatingActionContentLabelListListener(this);
        List<RFACLabelItem> items = new ArrayList<>();
        addItems(items, "Yesterday path", 0xff056f00, 0xff0d5302, 0xff056f00, 0);
        addItems(items, "Before yesterday path", 0xff283593, 0xff1a237e, 0xff283593, 1);

        rfaContent
                .setItems(items)
                .setIconShadowColor(0xff888888)
        ;
        rfabHelper = new RapidFloatingActionHelper(
                getContext(),
                rfaLayout,
                rfaBtn,
                rfaContent
        ).build();
    }

    private void addItems(List<RFACLabelItem> items, String label, int normalColor, int pressedColor, int labelColor, int position) {
        items.add(new RFACLabelItem<Integer>()
                .setLabel(label)
                .setResId(R.drawable.fab_history)
                .setIconNormalColor(normalColor)
                .setIconPressedColor(pressedColor)
                .setLabelColor(labelColor)
                .setWrapper(position)
        );
    }

    @Override
    public void onRFACItemLabelClick(int position, RFACLabelItem item) {
        onRFACItemIconClick(position, item);
    }

    @Override
    public void onRFACItemIconClick(int position, RFACLabelItem item) {
        String day = position == 0 ? getString(R.string.yesterday) : getString(R.string.before_yesterday);
        int dayInt = position == 0 ? R.string.yesterday : R.string.before_yesterday;
        if (!TESTING_MODE) {
            Toast.makeText(getContext(), "Click on focus button : " + position + 1 + "to see whole" + day + "path.", Toast.LENGTH_SHORT).show();
        }
        togglePath(dayInt);

        rfabHelper.toggleContent();
    }

    private void callOnMapVisible() {

        try {
            onMapVisible.call();
            onMapVisible = null;
        } catch (Exception ignored) {
        }
    }

    @VisibleForTesting
    void onMapVisible(Callable func) {
        onMapVisible = func;

        if (view.findViewById(R.id.mapFragment).getVisibility() == View.VISIBLE) {
            callOnMapVisible();
        }
    }

    @VisibleForTesting
    void setLocationBroker(LocationBroker locationBroker) {
        if (locationBroker != null && conn != null) {
            getActivity().unbindService(conn);
            getActivity().stopService(new Intent(getContext(), LocationService.class));
            conn = null;
        }
        this.locationBroker = locationBroker;
        goOnline();
    }

    @VisibleForTesting
    public MapboxMap getMap() {
        return map;
    }

    @VisibleForTesting
    HeatMapHandler getHeatMapHandler() {
        return heatMapHandler;
    }

    @VisibleForTesting
    PathsHandler getPathsHandler() {
        return pathsHandler;
    }

    @VisibleForTesting
    RapidFloatingActionHelper getRfabHelper() {
        return rfabHelper;
    }
}
