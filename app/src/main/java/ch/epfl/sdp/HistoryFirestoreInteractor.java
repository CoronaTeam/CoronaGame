package ch.epfl.sdp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class HistoryFirestoreInteractor extends FirestoreInteractor {

    private FirestoreWrapper wrapper;
    private Account user;

    HistoryFirestoreInteractor(FirestoreWrapper wrapper, Account user) {
        this.wrapper = wrapper;
        this.user = user;
    }

    @Override
    public void read(QueryHandler handler) {
        String path = "History/" + user.getId() + "/Positions";
        wrapper.collection(path)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handler.onSuccess(task.getResult());
                    } else {
                        handler.onFailure();
                    }
                });
    }

    @Override
    public void write(Map<String, PositionRecord> content, OnSuccessListener success, OnFailureListener failure) {
        String path = "History/" + user.getId() + "/Positions";
        PositionRecord posRec = (PositionRecord)content.values().toArray()[0];
        Map<String, Object> lastPos = new HashMap<>();
        lastPos.put("geoPoint", posRec.getGeoPoint());
        lastPos.put("timeStamp", posRec.getTimestamp());
        wrapper.collection(path)
                .document(posRec.calculateID())
                .set(content)
                .addOnSetSuccessListener(success)
                .addOnSetFailureListener(failure);


        wrapper.collection("LastPositions").document(user.getId()).set(lastPos);
    }
}
