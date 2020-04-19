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
    private QueryHandler fireBaseHandler;
    private MapFragment parentClass;
    private ArrayList<Circle> otherUsersPositionMarkers;
    private CircleManager positionMarkerManager;
    private ConcreteFirestoreInteractor db;
    private CompletableFuture<DocumentReference> fireBaseHandlerSuccess;
    private CompletableFuture<DocumentReference> fireBaseFailure;

    HeatMapHandler(MapFragment parentClass, ConcreteFirestoreInteractor db,
                   CircleManager positionMarkerManager) {
        this.parentClass = parentClass;
        this.db = db;
        this.positionMarkerManager = positionMarkerManager;
        otherUsersPositionMarkers = new ArrayList<>();
        initFireBaseQueryHandler();
    }

    private void updatePositionMarkersList(Iterator<Map.Entry<String, Map<String, Object>>> qsIterator, @NotNull Iterator<Circle> pmIterator) {
        while (pmIterator.hasNext()) {
            if (qsIterator.hasNext()) {
                Map<String, Object> qs = qsIterator.next().getValue();
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

    private void addMarkersToMarkerList(@NotNull Iterator<QueryDocumentSnapshot> qsIterator) {
        while (qsIterator.hasNext()) {
            QueryDocumentSnapshot qs = qsIterator.next();
            try {
                LatLng otherUserPos = new LatLng(((GeoPoint) (qs.get("geoPoint"))).getLatitude(),
                        ((GeoPoint) (qs.get("geoPoint"))).getLongitude());
                if (!otherUserPos.equals(parentClass.getPreviousLocation())) {
                    Circle pm = positionMarkerManager.create(new CircleOptions()
                            .withLatLng(new LatLng(
                                    ((GeoPoint) (qs.get("geoPoint"))).getLatitude(),
                                    ((GeoPoint) (qs.get("geoPoint"))).getLongitude()))
                            .withCircleColor("#ff6219")
                    );
                    otherUsersPositionMarkers.add(pm);
                }
            } catch (NullPointerException ignored) {
            }

        }
    }

    private void initFireBaseQueryHandler() {

        fireBaseHandlerSuccess = new CompletableFuture<DocumentReference>();

        fireBaseHandler = new QueryHandler<QuerySnapshot>() {

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
                ///////updatePositionMarkersList(qsIterator, pmIterator);
                // Run if there is more elements than in the last run
                addMarkersToMarkerList(qsIterator);

                //refresh map data
                positionMarkerManager.update(otherUsersPositionMarkers);
            }

            @Override
            public void onFailure() {
                Toast.makeText(parentClass.getActivity(), "Cannot retrieve positions from database", Toast.LENGTH_LONG).show();
            }
        };

        startTimer();
    }

    private void startTimer() {
        class UpdatePosTask extends TimerTask {

            public void run() {
                if (db != null && fireBaseHandler != null) {
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
                                    //////addMarkersToMarkerList(entryIterator);

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
