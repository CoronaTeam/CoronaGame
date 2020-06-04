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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.epfl.sdp.BuildConfig;
import ch.epfl.sdp.R;
import ch.epfl.sdp.connectivity.ConnectivityBroker;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.location.LocationService;
import ch.epfl.sdp.map.HeatMapHandler;
import ch.epfl.sdp.map.PathsHandler;
import ch.epfl.sdp.utilities.RotatedRapidFloatingActionButton;

import static android.view.View.INVISIBLE;
import static ch.epfl.sdp.connectivity.ConnectivityBroker.Provider.GPS;
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
public class MapFragment extends Fragment implements LocationListener, RapidFloatingActionContentLabelList.OnRapidFloatingActionContentLabelListListener {

    //TODO: some constant are duplicated from locationService
    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;
    private static final double MIN_LAT_LONG_CHANGE_RECENTER = 1;
    private PathsHandler pathsHandler;
    private MapView mapView;
    private MapboxMap map;
    private ConnectivityBroker connectivityBroker;
    private LatLng prevLocation = new LatLng(0, 0);
    private ConcreteFirestoreInteractor db;
    private CircleManager positionMarkerManager;
    private Circle userLocation;
    private HeatMapHandler heatMapHandler;
    private MapFragment classPointer;
    private ServiceConnection conn;
    private Callable onMapVisible;
    private int CURRENT_PATH;

    private List<Runnable> buttonsActions = new ArrayList<>();


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
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        classPointer = this;

        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                connectivityBroker = ((LocationService.LocationBinder) service).getService().getBroker();
                goOnline();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                // TODO: Check in code that the service does not become null
                connectivityBroker = null;
            }
        };

        // startService() overrides the default service lifetime that is managed by
        // bindService(Intent, ServiceConnection, int):
        // it requires the service to remain running until stopService(Intent) is called,
        // regardless of whether any clients are connected to it.
        requireActivity().startService(new Intent(getContext(), LocationService.class));
        requireActivity().bindService(new Intent(getContext(), LocationService.class), conn, Context.BIND_AUTO_CREATE);

        db = new ConcreteFirestoreInteractor();

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(requireActivity(), BuildConfig.mapboxAPIKey);

        // This contains the MapView in XML and needs to be called after the access token is configured.
        view = inflater.inflate(R.layout.fragment_map, container, false);

        view.findViewById(R.id.mapFragment).setVisibility(View.INVISIBLE);

        mapView = view.findViewById(R.id.mapFragment);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;

            mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
                positionMarkerManager = new CircleManager(mapView, map, style);

                userLocation = positionMarkerManager.create(new CircleOptions()
                        .withLatLng(prevLocation));

                updateUserMarkerPosition(prevLocation, true);
                heatMapHandler = new HeatMapHandler(classPointer, db, map);
                pathsHandler = new PathsHandler(classPointer, map);
            });
        });

        setMoreRFAButton();

        return view;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (connectivityBroker.hasPermissions(GPS)) {
            LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            updateUserMarkerPosition(newLocation, false);
            prevLocation = newLocation;

            view.findViewById(R.id.mapFragment).setVisibility(View.VISIBLE);
            view.findViewById(R.id.heapMapLoadingSpinner).setVisibility(View.GONE);

            callOnMapVisible();
        } else {
            Toast.makeText(requireActivity(), R.string.missing_permission, Toast.LENGTH_LONG).show();
        }
    }

    private void updateUserMarkerPosition(LatLng location, boolean initCamera) {
        // This method is where we update the marker position once we have new coordinates. First we
        // check if this is the first time we are executing this handler, the best way to do this is
        // check if marker is null

        if (map != null && map.getStyle() != null) {
            userLocation.setLatLng(location);
            positionMarkerManager.update(userLocation);

            if(initCamera || Math.abs(prevLocation.getLatitude() - location.getLatitude()) +
                    Math.abs(prevLocation.getLatitude() - location.getLatitude()) > MIN_LAT_LONG_CHANGE_RECENTER){
                map.animateCamera(CameraUpdateFactory.newLatLng(location));
            }
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
        if (connectivityBroker.isProviderEnabled(GPS) && connectivityBroker.hasPermissions(GPS)) {
            connectivityBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, classPointer);
        } else if (connectivityBroker.isProviderEnabled(GPS)) {
            // Must ask for permissions
            askGPSPermission();
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
            requireActivity().unbindService(conn);
        }

        super.onDestroy();

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void setCameraToCurrentLocation() {
        CameraPosition position = new CameraPosition.Builder()
                .target(prevLocation)
                .build();
        if (map != null) {
            map.easeCamera(CameraUpdateFactory.newCameraPosition(position), 2000);
        }
    }

    private void toggleHeatMap() {
        toggleLayer(HEATMAP_LAYER_ID);
    }

    /**
     * Make the layer visible/invisible on the map, according to its current visibility.
     * @param layerId identifies the layer
     * @return true if the layer is set to visible, false otherwise
     */
    private AtomicBoolean toggleLayer(String layerId) {
        AtomicBoolean res = new AtomicBoolean();
        map.getStyle(style -> {
            Layer layer = style.getLayer(layerId);
            if (layer != null) {
                if (VISIBLE.equals(layer.getVisibility().getValue())) {
                    layer.setProperties(visibility(NONE));
                } else {
                    layer.setProperties(visibility(VISIBLE));
                    if (layerId.equals(YESTERDAY_PATH_LAYER_ID)) {
                        CURRENT_PATH = R.string.yesterday;
                        showToast();
                    } else if (layerId.equals(BEFORE_PATH_LAYER_ID)) {
                        CURRENT_PATH = R.string.before_yesterday;
                        showToast();
                    }
                    res.set(true);
                }
            } else if (layerId.equals(YESTERDAY_PATH_LAYER_ID) || layerId.equals(BEFORE_PATH_LAYER_ID)){
                Toast.makeText(getActivity(), R.string.no_path_to_show, Toast.LENGTH_LONG).show();
            }
        });
        return res;
    }

    private void showToast() {
        if (!TESTING_MODE) {
            Toast.makeText(getContext(), R.string.click_to_see_whole_path, Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePath(int day) {
        String pathLayerId = day == R.string.yesterday ? YESTERDAY_PATH_LAYER_ID : BEFORE_PATH_LAYER_ID;
        String infectedLayerId = day == R.string.yesterday ? YESTERDAY_INFECTED_LAYER_ID : BEFORE_INFECTED_LAYER_ID;
        AtomicBoolean isVisible = toggleLayer(pathLayerId);
        toggleLayer(infectedLayerId);
        // Don't make camera focus on path when clicking to disable its visibility
        if (isVisible.get()) {
            pathsHandler.setCameraPosition(day);
        }

    }


    private void setMoreRFAButton() {
        RapidFloatingActionLayout rfaLayout = view.findViewById(R.id.more_rfal);
        RotatedRapidFloatingActionButton rfaBtn = view.findViewById(R.id.more_rfab);
        rfaBtn.setMaxRotation(90f); // turn 1/4th of a circle

        RapidFloatingActionContentLabelList rfaContent = new RapidFloatingActionContentLabelList(getContext());
        rfaContent.setOnRapidFloatingActionContentLabelListListener(this);
        List<RFACLabelItem> items = new ArrayList<>();
        setItems(items, new Item[] {
                new Item(
                        R.string.toggle_heatmap_label, R.drawable.visibility, R.color.colorAccent,
                        this::toggleHeatMap),
                new Item(
                        R.string.my_location_label, R.drawable.my_location, R.color.colorAccent,
                        this::setCameraToCurrentLocation),
                new Item(
                        R.string.focus_path_label, R.drawable.search, R.color.colorAccent,
                        this::focusPath),
                new Item(R.string.yesterday_label, R.drawable.calendar, R.color.colorAccent,
                        () -> togglePath(R.string.yesterday)),
                new Item(R.string.before_yesterday_label, R.drawable.calendar, R.color.colorAccent,
                        () -> togglePath(R.string.before_yesterday))
        });
        rfaContent.setItems(items).setIconShadowColor(getContext().getColor(R.color.colorPrimary));
        rfabHelper = new RapidFloatingActionHelper(getContext(), rfaLayout, rfaBtn, rfaContent).build();
    }

    private static class Item {
        int label, icon, color;
        Runnable callback;
        Item(int label, int icon, int color, Runnable callback) {
            this.label = label;
            this.icon = icon;
            this.color = color;
            this.callback = callback;
        }
    }

    private void setItems(List<RFACLabelItem> container, Item[] items) {
        buttonsActions.clear();
        int position = 0;
        for (Item item : items) {
            container.add(new RFACLabelItem<Integer>()
                    .setLabel(getString(item.label))
                    .setResId(item.icon)
                    .setIconNormalColor(getContext().getColor(item.color))
                    .setLabelColor(getContext().getColor(item.color))
                    .setWrapper(position++));
            buttonsActions.add(item.callback);
        }
    }

    @Override
    public void onRFACItemLabelClick(int position, RFACLabelItem item) {
        onRFACItemIconClick(position, item);
    }

    @Override
    public void onRFACItemIconClick(int position, RFACLabelItem item) {
        try {
            buttonsActions.get(position).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
        rfabHelper.toggleContent();
    }

    private void focusPath() {
        try {
            pathsHandler.seeWholePath(CURRENT_PATH);
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.no_path_to_show, Toast.LENGTH_LONG).show();
        }
    }

    private void callOnMapVisible() {

        try {
            if (onMapVisible != null) {onMapVisible.call();}
            onMapVisible = null;
        } catch (Exception ignored) {
        }
    }

    private void askGPSPermission(){
        connectivityBroker.requestPermissions(requireActivity(), LOCATION_PERMISSION_REQUEST);
        if (!connectivityBroker.hasPermissions(GPS)){
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.missing_GPS_Perm_Dialog_title)
                    .setMessage(R.string.missing_GPS_Perm_Dialog_text)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, id) -> goOnline())
                    .setNegativeButton(android.R.string.no,
                            (dialog, id) -> {
                                requireActivity().finishAndRemoveTask();
                                System.exit(0);
                            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            Timer updatePosTimer = new Timer();
            class UpdatePosTask extends TimerTask {
                public void run() {
                    if(connectivityBroker.hasPermissions(GPS)){
                        classPointer.requireActivity().runOnUiThread(() -> {
                            alertDialog.dismiss();
                            updatePosTimer.cancel();
                            goOnline();
                        });
                    }
                }
            }
            updatePosTimer.scheduleAtFixedRate(new UpdatePosTask(), 0, 100);
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
    void setConnectivityBroker(ConnectivityBroker connectivityBroker) {
        if (connectivityBroker != null && conn != null) {
            requireActivity().unbindService(conn);
            requireActivity().stopService(new Intent(getContext(), LocationService.class));
            conn = null;
        }
        this.connectivityBroker = connectivityBroker;
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
    void resetPathsHandler(Callable onResetDone){
        mapView.getMapAsync(mapboxMap -> mapboxMap.getStyle(style -> {

            pathsHandler = new PathsHandler(classPointer, map);
            try {
                onResetDone.call();
            } catch (Exception ignore){}
        }));
    }

    @VisibleForTesting
    RapidFloatingActionHelper getRfabHelper() {
        return rfabHelper;
    }

    @VisibleForTesting
    LatLng getUserLocation() {
        return userLocation.getLatLng();
    }
}
