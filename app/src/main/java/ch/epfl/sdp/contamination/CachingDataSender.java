package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;

import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;
import static ch.epfl.sdp.firestore.FirestoreInteractor.taskToFuture;

public interface CachingDataSender {
    int EXPAND_FACTOR = 100000; //determines the GPS coordinates precision
    String publicUserFolder = "publicUser/";
    String publicAlertAttribute = "recentlySickMeetingCounter";
    String privateUserFolder = "privateUser/";
    String privateRecoveryCounter = "recoveryCounter";

    int MAX_CACHE_ENTRY_AGE = InfectionAnalyst.UNINTENTIONAL_CONTAGION_TIME;

    static Location RoundAndExpandLocation(Location l) {
        int a = (int) (0.5 + l.getLatitude() * EXPAND_FACTOR);
        int b = (int) (0.5 + l.getLongitude() * EXPAND_FACTOR);
        l.setLongitude(b);
        l.setLatitude(a);
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
     * @return a future notification of success or failure.
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

    /**
     * @param userId
     * @return
     */
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
