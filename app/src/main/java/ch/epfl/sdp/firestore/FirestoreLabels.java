package ch.epfl.sdp.firestore;

public interface FirestoreLabels {

    // TODO: @Ulysse, @Adrien, @Kevin, @Lucas, @Lucie only use Firestore attribute names that are
    // written here too (we have to progressively get rid of random Strings with Firestore paths
    // in the rest of the code)

    // LastPositions collection
    String LAST_POSITIONS_COLL = "LastPositions";

    // LastPositions/[hashOfCarrier]/
    String GEOPOINT_TAG = "geoPoint";
    String TIMESTAMP_TAG = "timeStamp";

    // LiveGrid collection
    String LIVE_GRID_COLL = "LiveGrid";

    // LiveGrid/Grid#[lat]#[long]/[unixTime]/
    String ILLNESS_PROBABILITY_TAG = "illnessProbability";
    String INFECTION_STATUS_TAG = "infectionStatus";
    String UNIQUE_ID_TAG = "uniqueId";

    // LiveGrid/Grid#[lat]#[long]/
    String TIMES_LIST_COLL = "Times";

    // LiveGrid/Grid#[lat]#[long]/Times/[unixTime]
    String UNIXTIME_TAG = "Time";

    String publicUserFolder = "publicUser/";
    String publicAlertAttribute = "recentlySickMeetingCounter";
    String privateUserFolder = "privateUser/";
    String privateRecoveryCounter = "recoveryCounter";
}
