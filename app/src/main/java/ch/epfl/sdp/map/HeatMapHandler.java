package ch.epfl.sdp.map;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.firebase.firestore.GeoPoint;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.MultiPoint;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import ch.epfl.sdp.R;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.map.fragment.MapFragment;

import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.INFECTION_STATUS_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_COLL;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight;

/**
 * Is used to display the sick users last's positions on a heat map
 */
public class HeatMapHandler {
    public static final String HEATMAP_LAYER_ID = "lastPositions-heat";
    private static final String LASTPOSITIONS_SOURCE_ID = "lastPositions";
    private static final String HEATMAP_LAYER_SOURCE = "lastPositions";
    private final MapFragment parentClass;
    private final ConcreteFirestoreInteractor db;
    private final MapboxMap map;
    private Callable onHeatMapDataLoaded;


    public HeatMapHandler(@NonNull MapFragment parentClass, @NonNull ConcreteFirestoreInteractor db,
                          @NonNull MapboxMap map) {
        this.parentClass = parentClass;
        this.db = db;
        this.map = map;
        initQuery();
    }

    @NotNull
    // Color ramp for heatmap.  Domain is 0 (low) to 1  (high).
    // Begin color ramp at 0-stop with a 0-transparency color
    // to create a blur-like effect.
    static PropertyValue<Expression> adjustHeatMapColorRange() {
        return heatmapColor(
                interpolate(
                        linear(), heatmapDensity(),
                        literal(0), rgba(33, 102, 172, 0),
                        literal(0.2), rgba(103, 169, 207, 0.5),
                        literal(0.6), rgba(209, 229, 240, 0.5),
                        literal(0.85), rgba(253, 219, 199, 0.6),
                        literal(0.9), rgba(239, 138, 98, 0.65),
                        literal(1), rgba(178, 24, 43, 0.7)
                )
        );
    }

    @NotNull
    // Increase the heatmap weight based on frequency and property magnitude
    static PropertyValue<Expression> adjustHeatMapWeight() {
        return heatmapWeight(
                interpolate(
                        exponential(2), zoom(),
                        stop(8, 0.0001),
                        stop(18, 0.3)
                )
        );
    }

    @NotNull
    // Increase the heatmap color weight weight by zoom level
    // heatmap-intensity is a multiplier on top of heatmap-weight
    static PropertyValue<Expression> adjustHeatmapIntensity() {
        return heatmapIntensity(
                interpolate(
                        linear(), zoom(),
                        stop(8, 2),
                        stop(18, 100)
                )
        );
    }

    @NotNull
    // Adjust the heatmap radius by zoom level
    static PropertyValue<Expression> adjustHeatmapRadius() {
        return heatmapRadius(
                interpolate(
                        linear(), zoom(),
                        stop(8, 2),
                        stop(18, 60)
                )
        );
    }

    private void initQuery() {
        db.readCollection(collectionReference(LAST_POSITIONS_COLL))
                .thenAccept(this::createGeoJson)
                .exceptionally(e -> {
                    Toast.makeText(parentClass.requireActivity(), R.string.cannot_retr_pos_from_db, Toast.LENGTH_LONG).show();
                    return null;
                });
    }

    private void createGeoJson(@NotNull Map<String, Map<String, Object>> stringMapMap) {
        List<Point> infectionHeatMapPoints = new ArrayList<>();
        Set<Map.Entry<String, Map<String, Object>>> entrySet = stringMapMap.entrySet();

        for (Map.Entry<String, Map<String, Object>> entry : entrySet) {
            try {
                if (entry.getValue().get(INFECTION_STATUS_TAG).equals("INFECTED")) {
                    GeoPoint geoPoint = (GeoPoint) entry.getValue().get(GEOPOINT_TAG);
                    infectionHeatMapPoints.add(Point.fromLngLat(
                            geoPoint.getLongitude(),
                            geoPoint.getLatitude()
                    ));
                }
            } catch (NullPointerException ignored) {
            }
        }

        GeoJsonSource lastPos = new GeoJsonSource(LASTPOSITIONS_SOURCE_ID,
                FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(
                        MultiPoint.fromLngLats(infectionHeatMapPoints)
                )}));

        map.getStyle(style -> {
            style.addSource(lastPos);
            addHeatmapLayer();
        });
    }

    private void addHeatmapLayer() {
        HeatmapLayer layer = new HeatmapLayer(HEATMAP_LAYER_ID, LASTPOSITIONS_SOURCE_ID);

        layer.setMaxZoom(17);
        layer.setMinZoom(8);
        layer.setSourceLayer(HEATMAP_LAYER_SOURCE);

        layer.setProperties(
                adjustHeatMapColorRange(),
                adjustHeatMapWeight(),
                adjustHeatmapIntensity(),
                adjustHeatmapRadius()
        );
        map.getStyle(style -> style.addLayerAbove(layer, "waterway-label"));

        callHeatmapDataLoaded();
    }

    private void callHeatmapDataLoaded() {
        try {
            if (onHeatMapDataLoaded != null) {
                onHeatMapDataLoaded.call();
            }
            onHeatMapDataLoaded = null;
        } catch (Exception ignored) {
        }
    }

    @VisibleForTesting
    public void onHeatMapDataLoaded(Callable func) {
        onHeatMapDataLoaded = func;

        map.getStyle(style -> {
            if (style.getLayer(HEATMAP_LAYER_ID) != null) {
                callHeatmapDataLoaded();
            }
        });
    }
}