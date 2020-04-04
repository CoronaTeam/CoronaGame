package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO: This should become an interface again (is abstract class for legacy compatibility)
public abstract class FirestoreInteractor {

    public abstract void writeDocument(String path, Map<String, Object> document,
                                       OnSuccessListener successListener, OnFailureListener failureListener);

    public abstract void writeDocumentWithID(String path, String documentID,
                                             Map<String, Object> document,
                                             OnSuccessListener successListener, OnFailureListener failureListener);

    public abstract void readDocument(String path, QueryHandler handler);

    public abstract void readDocumentWithID(String path, String documentID, Callback callback);

    public void writeDocument(String path, Map<String, Object> document, Callback<Optional<Exception>> callback) {
        writeDocument(path, document, onSuccessBuilder(callback), onFailureBuilder(callback));
    }

    public void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                                    Callback<Optional<Exception>> callback) {
        writeDocumentWithID(path, documentID, document, onSuccessBuilder(callback),
                onFailureBuilder(callback));
    }

    public void readDocument(String path, Callback<Map<String, Map<String, Object>>> callback) {
        readDocument(path, queryHandlerBuilder(callback));
    }

    /*public void readDocumentWithID(String path, String documentID, Callback<Map<String, Map<String, Object>>> callback) {
        readDocumentWithID(path, documentID, queryHandlerBuilder(callback));
    }*/

    private QueryHandler queryHandlerBuilder(Callback<Map<String, Map<String, Object>>> callback) {
        return new QueryHandler() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                Map<String, Map<String, Object>> res = new HashMap<>();
                for (QueryDocumentSnapshot qs : snapshot) {
                    res.put(qs.getId(), qs.getData());
                }
                callback.onCallback(res);
            }

            @Override
            public void onFailure() {
                callback.onCallback(Collections.emptyMap());
            }
        };
    }

    private OnSuccessListener onSuccessBuilder(Callback<Optional<Exception>> callback) {
        return e -> callback.onCallback(Optional.empty());
    }

    private OnFailureListener onFailureBuilder(Callback<Optional<Exception>> callback) {
        return e -> callback.onCallback(Optional.of(e));
    }

    OnCompleteListener<QuerySnapshot> querySnapshotOnCompleteListener(QueryHandler handler) {
        return task -> {
            if (task.isSuccessful()) {
                handler.onSuccess(task.getResult());
            } else {
                handler.onFailure();
            }
        };
    }

    OnCompleteListener<DocumentSnapshot> documentSnapshotOnCompleteListener(Callback callback) {
        return task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    callback.onCallback(document.getData());
                } else {
                    callback.onCallback(task.getException());
                }
            }
        };
    }
}