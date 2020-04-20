package ch.epfl.sdp.Map;

import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;

public class HeatMapHandler {
    private static final int OTHER_USERS_UPDATE_INTERVAL_MILLISECS = 2500;
    private Timer updateOtherPosTimer;
    private MapFragment parentClass;
    private ArrayList<Circle> otherUsersPositionMarkers;
    private CircleManager positionMarkerManager;
    private ConcreteFirestoreInteractor db;

    HeatMapHandler(MapFragment parentClass, ConcreteFirestoreInteractor db,
                   CircleManager positionMarkerManager) {
        this.parentClass = parentClass;
        this.db = db;
        this.positionMarkerManager = positionMarkerManager;
        otherUsersPositionMarkers = new ArrayList<>();
    }

    private void updatePositionMarkersList(Iterator<Map.Entry<String, Map<String, Object>>> entrySetIterator, @NotNull Iterator<Circle> pmIterator) {
        while (pmIterator.hasNext()) {
            if (entrySetIterator.hasNext()) {
                Map<String, Object> qs = entrySetIterator.next().getValue();
                Circle pm = pmIterator.next();
                System.out.println(qs);

                try {
                    LatLng otherUserPos =
                            new LatLng(((GeoPoint) (qs.get("geoPoint"))).getLatitude(),
                                    ((GeoPoint) (qs.get("geoPoint"))).getLongitude());
                    if (!otherUserPos.equals(parentClass.getPreviousLocation())) {
                        pm.setLatLng(otherUserPos); // add point only if that's not the user
                    }
                } catch (NullPointerException ignored) {
                }

            } else { // if some points were deleted remove them from the list
                positionMarkerManager.delete(pmIterator.next());
                pmIterator.remove();
            }
        }
    }

    private void addMarkersToMarkerList(@NotNull Iterator<Map.Entry<String, Map<String, Object>>> entryIterator) {
        while (entryIterator.hasNext()) {
            Map.Entry<String, Map<String, Object>> mapEntry = entryIterator.next();
            try {
                LatLng otherUserPos = new LatLng(((GeoPoint) (mapEntry.getValue().get("geoPoint"))).getLatitude(),
                        ((GeoPoint) (mapEntry.getValue().get("geoPoint"))).getLongitude());
                if (!otherUserPos.equals(parentClass.getPreviousLocation())) {
                    Circle pm = positionMarkerManager.create(new CircleOptions()
                            .withLatLng(new LatLng(
                                    ((GeoPoint) (mapEntry.getValue().get("geoPoint"))).getLatitude(),
                                    ((GeoPoint) (mapEntry.getValue().get("geoPoint"))).getLongitude()))
                            .withCircleColor("#ff6219")
                    );
                    otherUsersPositionMarkers.add(pm);
                }
            } catch (NullPointerException ignored) {
            }
        }
    }

    private void startTimer() {
        class UpdatePosTask extends TimerTask {

            public void run() {
                if (db != null) {
                    //db.read(fireBaseHandler);
                    db.readCollection(collectionReference("LastPositions"))
                            .whenComplete((result, throwable) -> {
                                if (throwable == null) {
                                    /* The idea here is to reuse the Circle objects to not recreate the datastructure from
                                    scratch on each update. It's now overkill but will be usefull for the heatmaps
                                    It's also necessary to keep the Circle objects around because recreating them each time
                                    there is new data make the map blink like a christmas tree
                                     */
                                    Iterator<Map.Entry<String, Map<String, Object>>> entryIterator =
                                            result.entrySet().iterator(); //
                                    // data from firebase
                                    Iterator<Circle> pmIterator = otherUsersPositionMarkers.iterator(); // local list of position marker

                                    // update the Arraylist contents first
                                    updatePositionMarkersList(entryIterator, pmIterator);
                                    // Run if there is more elements than in the last run
                                    addMarkersToMarkerList(entryIterator);

                                    //refresh map data
                                    positionMarkerManager.update(otherUsersPositionMarkers);
                                } else {
                                    Toast.makeText(parentClass.getActivity(), "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
                                }
                            });
                }
            }
        }

        if (updateOtherPosTimer != null) {
            updateOtherPosTimer.cancel();
        }
        updateOtherPosTimer = new Timer();
        updateOtherPosTimer.scheduleAtFixedRate(new UpdatePosTask(), 0, OTHER_USERS_UPDATE_INTERVAL_MILLISECS);
    }

    private void stopTimer() {
        updateOtherPosTimer.cancel();
    }
}
