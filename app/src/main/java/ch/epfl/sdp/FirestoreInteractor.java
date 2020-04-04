package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class FirestoreInteractor {
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();


    public abstract void writeDocument(CollectionReference collectionReference, Map<String, Object> document,
                                       OnSuccessListener successListener, OnFailureListener failureListener);

    public abstract void writeDocumentWithID(DocumentReference docRef,
                                             Map<String, Object> document,
                                             OnSuccessListener successListener,
                                             OnFailureListener failureListener);

    public abstract void readDocument(CollectionReference collectionReference, QueryHandler handler);

    public abstract void readDocumentWithID(DocumentReference docRef, Callback callback);


    //////////////////////////////////////////////////////////////

    public CollectionReference collectionReference(String path) {
        return firestore.collection(path);
    }

    public DocumentReference documentReference(String path, String documentID) {
        return collectionReference(path).document(documentID);
    }

    public void writeDocument(String path, Map<String, Object> document,
                              OnSuccessListener successListener,
                              OnFailureListener failureListener) {
        writeDocument(collectionReference(path), document, successListener, failureListener);
    }

    public void writeDocument(String path, Map<String, Object> document, Callback<Optional<Exception>> callback) {
        writeDocument(path, document, onSuccessBuilder(callback), onFailureBuilder(callback));
    }

    public void writeDocument(CollectionReference collectionReference, Map<String, Object> document,
                              Callback<Optional<Exception>> callback) {
        writeDocument(collectionReference, document, onSuccessBuilder(callback), onFailureBuilder(callback));
    }

    public void writeDocumentWithID(String path, String documentID,
                                    Map<String, Object> document,
                                    OnSuccessListener successListener,
                                    OnFailureListener failureListener) {
        writeDocumentWithID(documentReference(path, documentID), document, successListener,
                failureListener);
    }

    public void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                                    Callback<Optional<Exception>> callback) {
        writeDocumentWithID(path, documentID, document, onSuccessBuilder(callback),
                onFailureBuilder(callback));
    }

    public void writeDocumentWithID(DocumentReference documentReference, Map<String, Object> document,
                                    Callback<Optional<Exception>> callback) {
        writeDocumentWithID(documentReference, document, onSuccessBuilder(callback),
                onFailureBuilder(callback));
    }

    public void readDocument(String path, QueryHandler handler) {
        readDocument(collectionReference(path), handler);
    }

    public void readDocument(String path, Callback<Map<String, Map<String, Object>>> callback) {
        readDocument(path, queryHandlerBuilder(callback));
    }

    public void readDocument(CollectionReference collectionReference,
                             Callback<Map<String, Map<String, Object>>> callback) {
        readDocument(collectionReference, queryHandlerBuilder(callback));
    }

    public void readDocumentWithID(String path, String documentID, Callback callback) {
        readDocumentWithID(documentReference(path, documentID), callback);
    }

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