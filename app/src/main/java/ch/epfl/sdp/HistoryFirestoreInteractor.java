package ch.epfl.sdp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Map;

public class HistoryFirestoreInteractor extends FirestoreInteractor {

    private FirestoreWrapper wrapper;
    private Account user;

    HistoryFirestoreInteractor(FirestoreWrapper wrapper, Account user) {
        this.wrapper = wrapper;
        this.user = user;
    }

    @Override
    void read(QueryHandler handler) {
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
    void write(Map<String, PositionRecord> content, OnSuccessListener success, OnFailureListener failure) {
        String path = "History/" + user.getId() + "/Positions";
        String documentID = content.get("Position").calculateID();
        wrapper.collection(path)
                .document(documentID)
                .set(content)
                .addOnSetSuccessListener(success)
                .addOnSetFailureListener(failure);
    }
}
