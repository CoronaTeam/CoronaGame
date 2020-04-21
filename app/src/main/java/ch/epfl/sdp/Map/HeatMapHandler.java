package ch.epfl.sdp.Map;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight;

class HeatMapHandler {
    private QueryHandler fireBaseHandler;
    private MapFragment parentClass;

    private ConcreteFirestoreInteractor db;
    private MapboxMap map;

    private static final String EARTHQUAKE_SOURCE_ID = "earthquakes";
    private static final String HEATMAP_LAYER_ID = "earthquakes-heat";
    private static final String HEATMAP_LAYER_SOURCE = "earthquakes";


    HeatMapHandler(@NonNull MapFragment parentClass, @NonNull ConcreteFirestoreInteractor db,
                   @NonNull MapboxMap map) {
        this.parentClass = parentClass;
        this.db = db;
        this.map = map;
        initFireBaseQueryHandler();
    }


    private void createGeoJson(@NotNull Iterator<QueryDocumentSnapshot> qsIterator) {
        List<Point> infectionHeatMapPoints = new ArrayList<>();

        for (; qsIterator.hasNext(); ) {
            QueryDocumentSnapshot qs = qsIterator.next();
            try {
                infectionHeatMapPoints.add(Point.fromLngLat(((GeoPoint) (qs.get("geoPoint"))).getLongitude(),
                        ((GeoPoint) (qs.get("geoPoint"))).getLatitude()
                ));
            } catch (NullPointerException ignored) {
            }
        }

        GeoJsonSource a = new GeoJsonSource(EARTHQUAKE_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(
                        MultiPoint.fromLngLats(infectionHeatMapPoints)
                )}));
        // Do not delete the "a" variable. If the GeoJsonSource is created inside the addSource call
        // it takes a long time and make some tests crash. The compiler should have optimised this
        // variable out but it works. No crash occurs when not testing

        map.getStyle(style -> {
            style.addSource(a);
            addHeatmapLayer();
        });
    }

    private void addHeatmapLayer() {
        HeatmapLayer layer = new HeatmapLayer(HEATMAP_LAYER_ID, EARTHQUAKE_SOURCE_ID);
        //layer.setMinZoom(13);
        layer.setMaxZoom(17);
        layer.setMinZoom(8);
        layer.setSourceLayer(HEATMAP_LAYER_SOURCE);

        layer.setProperties(
                // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
                // Begin color ramp at 0-stop with a 0-transparency color
                // to create a blur-like effect.
                heatmapColor(
                        interpolate(
                                linear(), heatmapDensity(),
                                literal(0), rgba(33, 102, 172, 0),
                                literal(0.2), rgba(103, 169, 207, 0.5),
                                literal(0.6), rgba(209, 229, 240, 0.5),
                                literal(0.85), rgba(253, 219, 199, 0.6),
                                literal(0.9), rgba(239, 138, 98, 0.65),
                                literal(1), rgba(178, 24, 43, 0.7)
                        )
                ),


                // Increase the heatmap weight based on frequency and property magnitude
                heatmapWeight(
                        interpolate(
                                exponential(2), zoom(),
                                stop(8, 0.0001),
                                stop(18, 0.3)
                        )
                ),


                // Increase the heatmap color weight weight by zoom level
                // heatmap-intensity is a multiplier on top of heatmap-weight
                heatmapIntensity(
                        interpolate(
                                linear(), zoom(),
                                stop(8, 2),
                                stop(18, 100)
                        )
                ),

                // Adjust the heatmap radius by zoom level
                heatmapRadius(
                        interpolate(
                                linear(), zoom(),
                                stop(8, 2),
                                stop(18, 60)
                        )
                )


        );

        map.getStyle(style -> style.addLayerAbove(layer, "waterway-label"));
    }


    private void initFireBaseQueryHandler() {

        fireBaseHandler = new QueryHandler<QuerySnapshot>() {

            @Override
            public void onSuccess(QuerySnapshot snapshot) {

                /* The idea here is to reuse the Circle objects to not recreate the datastructure from
                scratch on each update. It's now overkill but will be usefull for the heatmaps
                It's also necessary to keep the Circle objects around because recreating them each time
                there is new data make the map blink like a christmas tree
                 */

                Iterator<QueryDocumentSnapshot> qsIterator = snapshot.iterator(); // data from firebase

                // Run if there is more elements than in the last run
                createGeoJson(qsIterator);

            }

            @Override
            public void onFailure() {
                Toast.makeText(parentClass.getActivity(), "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
            }
        };

        db.readCollection("LastPositions", fireBaseHandler);
    }
}