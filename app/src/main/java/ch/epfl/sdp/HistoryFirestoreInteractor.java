package ch.epfl.sdp;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Map;

import ch.epfl.sdp.firestore.ConcreteFirestoreInteractor;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

public class HistoryFirestoreInteractor {

    private FirestoreInteractor fsi;
    private Account user;

    HistoryFirestoreInteractor(Account user) {
        this.fsi = new ConcreteFirestoreInteractor();
        this.user = user;
    }

    public void read(QueryHandler handler) {
        String path = "History/" + user.getId() + "/Positions";
        fsi.readCollection(path, handler);
    }


    public void write(Map<String, Object> content, OnSuccessListener success,
                      OnFailureListener failure) {
        String path = "History/" + user.getId() + "/Positions";
        PositionRecord posRec = (PositionRecord) content.values().toArray()[0];

        fsi.writeDocumentWithID(path, posRec.calculateID(), content, success, failure);
    }

    @VisibleForTesting
    public void setFirestoreInteractor(FirestoreInteractor interactor) {
        fsi = interactor;
    }
}
