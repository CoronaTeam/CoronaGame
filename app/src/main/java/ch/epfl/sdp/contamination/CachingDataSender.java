package ch.epfl.sdp.contamination;

import android.location.Location;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.util.Date;
import java.util.SortedMap;

import ch.epfl.sdp.firestore.FirestoreInteractor;

public interface CachingDataSender {
    int EXPAND_FACTOR = 100000; //determines the GPS coordinates precision
    String publicUserFolder = "publicUser/";
    String publicAlertAttribute = "recentlySickMeetingCounter";

    static Location RoundAndExpandLocation(Location l){
        int a = (int)(0.5 + l.getLatitude()*EXPAND_FACTOR);
        int b = (int)(0.5 + l.getLongitude()*EXPAND_FACTOR);
        l.setLongitude(b);
        l.setLatitude(a);
        return l;
    }
    /**
     *   Sends the location and date to firebase, along with the userID of the user using the app.
     * @param location : location, rounded by ~1 meter
     * @param time : date associated to that location
     */
    void registerLocation(Carrier carrier, Location location, Date time);
    /**
     * Notifies a user he has been close to an infected person
     */
    static void sendAlert(String userId){
        DocumentReference ref = FirestoreInteractor.documentReference(publicUserFolder,userId);
        ref.update(publicAlertAttribute, FieldValue.increment(1));
    }

    static void resetSickAlerts(String userId){
        DocumentReference ref = FirestoreInteractor.documentReference(publicUserFolder,userId);
        ref.update(publicAlertAttribute, FieldValue.delete());
    }
    /**
     *  This is the cache part of the CachedSender.
     * @return:  positions send to firebase during the last UNINTENTIONAL_CONTAGION_TIME time.
     */
    SortedMap<Date, Location> getLastPositions();
}
