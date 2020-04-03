package ch.epfl.sdp;

import androidx.annotation.VisibleForTesting;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.HashMap;
import java.util.Map;

public class HistoryFirestoreInteractor {

    private FirestoreInteractor fsi;
    private Account user;

    HistoryFirestoreInteractor(FirestoreWrapper firestoreWrapper, Account user) {
        this.fsi = new ConcreteFirestoreInteractor(firestoreWrapper, new CountingIdlingResource(
                "HistoryCalls"));
        this.user = user;
    }

    public void read(QueryHandler handler) {
        String path = "History/" + user.getId() + "/Positions";
        fsi.readDocument(path, handler);

        /*wrapper.collection(path)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handler.onSuccess(task.getResult());
                    } else {
                        handler.onFailure();
                    }
                });*/
    }


    public void write(Map<String, Object> content, OnSuccessListener success,
                      OnFailureListener failure) {
        String path = "History/" + user.getId() + "/Positions";
        PositionRecord posRec = (PositionRecord) content.values().toArray()[0];

        Map<String, Object> lastPos = new HashMap<>();
        lastPos.put("geoPoint", posRec.getGeoPoint());
        lastPos.put("timeStamp", posRec.getTimestamp());

        fsi.writeDocumentWithID(path, posRec.calculateID(), content, success, failure);
        fsi.writeDocumentWithID("LastPositions", user.getId(), lastPos, success, failure);

        /*wrapper.collection(path)
                .document(posRec.calculateID())
                .set(content)
                .addOnSetSuccessListener(success)
                .addOnSetFailureListener(failure);


        wrapper.collection("LastPositions").document(user.getId()).set(lastPos);*/
    }

    @VisibleForTesting
    public void setFirestoreInteractor(FirestoreInteractor interactor) {
        fsi = interactor;
    }
}
