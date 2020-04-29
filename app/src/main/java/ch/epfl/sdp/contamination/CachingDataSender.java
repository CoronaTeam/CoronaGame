package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.SortedMap;

import ch.epfl.sdp.firestore.FirestoreInteractor;

public interface CachingDataSender {
    int EXPAND_FACTOR = 100000; //determines the GPS coordinates precision
    String publicUserFolder = "publicUser/";
    String publicAlertAttribute = "recentlySickMeetingCounter";
    String privateUserFolder = "privateUser";
    String privateRecoveryCounter = "recoveryCounter";

    static Location RoundAndExpandLocation(Location l){
        int a = (int)(0.5 + l.getLatitude()*EXPAND_FACTOR);
        int b = (int)(0.5 + l.getLongitude()*EXPAND_FACTOR);
        l.setLongitude(b);
        l.setLatitude(a);
        return l;
    }
    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     *   The default callback is executed after this operation
     * @param carrier : the carrier present at location
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     */
    void registerLocation(Carrier carrier, Location location, Date time);

    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     *   Call the appropriate listener depending on the result of the operation
     * @param carrier : the carrier present at location
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     * @param successListener : listener called in case of success
     * @param failureListener: listener called in case of failure
     */
    void registerLocation(Carrier carrier,
                          Location location,
                          Date time,
                          OnSuccessListener successListener,
                          OnFailureListener failureListener);
    /**
     * Notifies a user he has been close to an infected person
     */
    default void sendAlert(String userId){
        sendAlert(userId,0f);
    }

    /**
     * If yesterday you were 90% surely sick, the person you met were already aware that they might be infected.
     * Thus, it is "less" scary for them to know you are know sick. This method implements just that.
     * @param userId
     * @param previousIllnessProbability
     */
    default void sendAlert(String userId, float previousIllnessProbability){
        DocumentReference ref = FirestoreInteractor.documentReference(publicUserFolder,userId);
        ref.update(publicAlertAttribute, FieldValue.increment(1f-previousIllnessProbability));
    }

    default void resetSickAlerts(String userId){
        DocumentReference ref = FirestoreInteractor.documentReference(publicUserFolder,userId);
        ref.update(publicAlertAttribute, FieldValue.delete());
    }
    /**
     *  This is the cache part of the CachedSender.
     * @return:  positions send to firebase during the last UNINTENTIONAL_CONTAGION_TIME time.
     */
    SortedMap<Date, Location> getLastPositions();
}
