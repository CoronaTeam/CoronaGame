package ch.epfl.sdp;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;

import static ch.epfl.sdp.firestore.FirestoreLabels.GEOPOINT_TAG;
import static ch.epfl.sdp.firestore.FirestoreLabels.LAST_POSITIONS_DOC;
import static ch.epfl.sdp.firestore.FirestoreLabels.TIMESTAMP_TAG;

public class HistoryFirestoreInteractor extends ConcreteFirestoreInteractor {

    private Account user;

    HistoryFirestoreInteractor(Account user) {
        super();
        this.user = user;
    }

    public CompletableFuture<Map<String, Map<String, Object>>> readHistory() {
        return readCollection(collectionReference(historyPositionsPath()));
    }

    private String historyPositionsPath(){
        return "History/" + user.getId() + "/Positions";
    }


    public CompletableFuture<Void> writePositions(Map<String, Object> content) {
        PositionRecord posRec = (PositionRecord) content.values().toArray()[0];

        Map<String, Object> lastPos = new HashMap<>();
        lastPos.put(GEOPOINT_TAG, posRec.getGeoPoint());
        lastPos.put(TIMESTAMP_TAG, posRec.getTimestamp());

        return writeDocumentWithID(documentReference(historyPositionsPath(), posRec.calculateID()), content).thenRun(() ->
                writeDocumentWithID(documentReference(LAST_POSITIONS_DOC, user.getId()), lastPos));
    }
}
