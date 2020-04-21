package ch.epfl.sdp.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import ch.epfl.sdp.BuildConfig;
import ch.epfl.sdp.R;

/**
 * This fragment is used to display the user's last positions as a line on the map,
 * as well as points of met infected users.
 */
public class PathsFragment extends Fragment {
    private MapView mapView;
    public MapboxMap map; // made public for testing
    private List<Point> pathCoordinates;

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

    private void initPathCoordinates() {
        // Create a list to store our line coordinates.
        pathCoordinates = new ArrayList<>();
        pathCoordinates.add(Point.fromLngLat(-118.39439114221236, 33.397676454651766));
        pathCoordinates.add(Point.fromLngLat(-118.39421054012902, 33.39769799454838));
        pathCoordinates.add(Point.fromLngLat(-118.39408583869053, 33.39761901490136));
        pathCoordinates.add(Point.fromLngLat(-118.39388373635917, 33.397328225582285));
        pathCoordinates.add(Point.fromLngLat(-118.39372033447427, 33.39728514560042));
        pathCoordinates.add(Point.fromLngLat(-118.3930882271826, 33.39756875508861));
        pathCoordinates.add(Point.fromLngLat(-118.3928216241072, 33.39759029501192));
        pathCoordinates.add(Point.fromLngLat(-118.39227981785722, 33.397234885594564));
        pathCoordinates.add(Point.fromLngLat(-118.392021814881, 33.397005125197666));
        pathCoordinates.add(Point.fromLngLat(-118.39090810203379, 33.396814854409186));
        pathCoordinates.add(Point.fromLngLat(-118.39040499623022, 33.39696563506828));
        pathCoordinates.add(Point.fromLngLat(-118.39005669221234, 33.39703025527067));
        pathCoordinates.add(Point.fromLngLat(-118.38953208616074, 33.39691896489222));
        pathCoordinates.add(Point.fromLngLat(-118.38906338075398, 33.39695127501678));
        pathCoordinates.add(Point.fromLngLat(-118.38891287901787, 33.39686511465794));
        pathCoordinates.add(Point.fromLngLat(-118.38898167981154, 33.39671074380141));
        pathCoordinates.add(Point.fromLngLat(-118.38984598978178, 33.396064537239404));
        pathCoordinates.add(Point.fromLngLat(-118.38983738968255, 33.39582400356976));
        pathCoordinates.add(Point.fromLngLat(-118.38955358640874, 33.3955978295119));
        pathCoordinates.add(Point.fromLngLat(-118.389041880506, 33.39578092284221));
        pathCoordinates.add(Point.fromLngLat(-118.38872797688494, 33.3957916930261));
        pathCoordinates.add(Point.fromLngLat(-118.38817327048618, 33.39561218978703));
        pathCoordinates.add(Point.fromLngLat(-118.3872530598711, 33.3956265500598));
        pathCoordinates.add(Point.fromLngLat(-118.38653065153775, 33.39592811523983));
        pathCoordinates.add(Point.fromLngLat(-118.38638444985126, 33.39590657490452));
        pathCoordinates.add(Point.fromLngLat(-118.38638874990086, 33.395737842093304));
        pathCoordinates.add(Point.fromLngLat(-118.38723155962309, 33.395027006653244));
        pathCoordinates.add(Point.fromLngLat(-118.38734766096238, 33.394441819579285));
        pathCoordinates.add(Point.fromLngLat(-118.38785936686516, 33.39403972556368));
        pathCoordinates.add(Point.fromLngLat(-118.3880743693453, 33.393616088784825));
        pathCoordinates.add(Point.fromLngLat(-118.38791956755958, 33.39331092541894));
        pathCoordinates.add(Point.fromLngLat(-118.3874852625497, 33.39333964672257));
        pathCoordinates.add(Point.fromLngLat(-118.38686605540683, 33.39387816940854));
        pathCoordinates.add(Point.fromLngLat(-118.38607484627983, 33.39396792286514));
        pathCoordinates.add(Point.fromLngLat(-118.38519763616081, 33.39346171215717));
        pathCoordinates.add(Point.fromLngLat(-118.38523203655761, 33.393196040109466));
        pathCoordinates.add(Point.fromLngLat(-118.3849955338295, 33.393023711860515));
        pathCoordinates.add(Point.fromLngLat(-118.38355931726203, 33.39339708930139));
        pathCoordinates.add(Point.fromLngLat(-118.38323251349217, 33.39305243325907));
        pathCoordinates.add(Point.fromLngLat(-118.3832583137898, 33.39244928189641));
        pathCoordinates.add(Point.fromLngLat(-118.3848751324406, 33.39108499551671));
        pathCoordinates.add(Point.fromLngLat(-118.38522773650804, 33.38926830725471));
        pathCoordinates.add(Point.fromLngLat(-118.38508153482152, 33.38916777794189));
        pathCoordinates.add(Point.fromLngLat(-118.38390332123025, 33.39012280171983));
        pathCoordinates.add(Point.fromLngLat(-118.38318091289693, 33.38941192035707));
        pathCoordinates.add(Point.fromLngLat(-118.38271650753981, 33.3896129783018));
        pathCoordinates.add(Point.fromLngLat(-118.38275090793661, 33.38902416443619));
        pathCoordinates.add(Point.fromLngLat(-118.38226930238106, 33.3889451769069));
        pathCoordinates.add(Point.fromLngLat(-118.38258750605169, 33.388420985121336));
        pathCoordinates.add(Point.fromLngLat(-118.38177049662707, 33.388083490107284));
        pathCoordinates.add(Point.fromLngLat(-118.38080728551597, 33.38836353925403));
        pathCoordinates.add(Point.fromLngLat(-118.37928506795642, 33.38717870977523));
        pathCoordinates.add(Point.fromLngLat(-118.37898406448423, 33.3873079646849));
        pathCoordinates.add(Point.fromLngLat(-118.37935386875012, 33.38816247841951));
        pathCoordinates.add(Point.fromLngLat(-118.37794345248027, 33.387810620840135));
        pathCoordinates.add(Point.fromLngLat(-118.37546662390886, 33.38847843095069));
        pathCoordinates.add(Point.fromLngLat(-118.37091717142867, 33.39114243958559));
    }

    private void modifyCameraPosition(double latitude, double longitude) {
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .build();
        if (map != null) {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(position));
        }
    }

    private void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        mapboxMap.setStyle(Style.OUTDOORS, style -> {

            initPathCoordinates();

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
        modifyCameraPosition(33.397676454651766, -118.39439114221236);
    }
}
