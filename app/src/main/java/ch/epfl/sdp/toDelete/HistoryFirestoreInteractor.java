package ch.epfl.sdp.toDelete;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.utilities.Account;
import ch.epfl.sdp.utilities.PositionRecord;
import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;

public class HistoryFirestoreInteractor extends ConcreteFirestoreInteractor {

    private Account user;

    public HistoryFirestoreInteractor(Account user) {
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
        lastPos.put("geoPoint", posRec.getGeoPoint());
        lastPos.put("timeStamp", posRec.getTimestamp());

        return writeDocumentWithID(documentReference(historyPositionsPath(), posRec.calculateID()), content).thenRun(() ->
                writeDocumentWithID(documentReference("LastPositions", user.getId()), lastPos));
    }
}
