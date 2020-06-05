package ch.epfl.sdp.contamination.databaseIO;

import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.contamination.InfectionAnalyst;

import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreInteractor.taskToFuture;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicAlertAttribute;
import static ch.epfl.sdp.firestore.FirestoreLabels.publicUserFolder;

public interface CachingDataSender {
    double ROUNDING_FACTOR = 100000d; //determines the GPS coordinates precision


    int MAX_CACHE_ENTRY_AGE = InfectionAnalyst.PRESYMPTOMATIC_CONTAGION_TIME;

    static double roundCoordinate(double coordinates) {
        return (double) Math.round(coordinates * ROUNDING_FACTOR) / ROUNDING_FACTOR;//fast rounding to 5 digits
    }

    /**
     * Rounds a location to 5 digits after the comma
     *
     * @param l
     * @return
     */
    static Location roundLocation(Location l) {
        if (l == null) {
            throw new IllegalArgumentException("Location can't be null");
        }
        double latitude = l.getLatitude();
        double longitude = l.getLongitude();
        latitude = roundCoordinate(latitude);
        longitude = roundCoordinate(longitude);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        return l;
    }

    /**
     * Sends the location and date to firebase, along with the userID of the user using the app.
     * Operation depending on the result of the operation can be chained to the returned future
     *
     * @param carrier  : the carrier present at location
     * @param location : location, rounded by ~1 meter
     * @param time     : date associated to that location
     * @return a future notification of success or failure.
     */
    CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time);

    /**
     * Notifies a user he has been close to an infected person
     *
     * @return
     */
    default CompletableFuture<Void> sendAlert(String userId) {
        return sendAlert(userId, 0f);
    }

    /**
     * If yesterday you were 90% surely sick, the person you met were already aware that they might be infected.
     * Thus, it is "less" scary for them to know you are know sick. This method implements just that.
     *
     * @param userId
     * @param previousIllnessProbability
     * @return a future notification of success or failure.
     */
    default CompletableFuture<Void> sendAlert(String userId, float previousIllnessProbability) {
        DocumentReference ref = documentReference(publicUserFolder, userId);
        Task<Void> task = ref.update(publicAlertAttribute,
                FieldValue.increment(1f - previousIllnessProbability));
        return taskToFuture(task);
    }

    default CompletableFuture<Void> resetSickAlerts(String userId) {
        DocumentReference ref = documentReference(publicUserFolder, userId);
        Task<Void> task = ref.update(publicAlertAttribute, FieldValue.delete());
        return taskToFuture(task);
    }

    /**
     * This is the cache part of the CachedSender.
     *
     * @return: positions send to firebase during the last UNINTENTIONAL_CONTAGION_TIME time.
     */
    SortedMap<Date, Location> getLastPositions();
}
