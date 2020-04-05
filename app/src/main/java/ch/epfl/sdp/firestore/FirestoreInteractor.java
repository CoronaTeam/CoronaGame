package ch.epfl.sdp.firestore;

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

import ch.epfl.sdp.Callback;

public abstract class FirestoreInteractor {
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * Add a new document to Firestore at the desired location. DocumentID will be automatically
     * created
     *
     * @param collectionReference a Firestore reference to a collection
     * @param document            data that will be uploaded at the address referred by collectionReference,
     *                            ideally it should be a Map<String, Object>. If not, each custom class must
     *                            have a public constructor that takes no arguments, and in addition, the
     *                            class must include a public getter for each property
     * @param successListener     the behaviour in case of success
     * @param failureListener     the behaviour in case of failure
     */
    public abstract void writeDocument(CollectionReference collectionReference, Object document,
                                       OnSuccessListener successListener, OnFailureListener failureListener);

    /**
     * Create or overwrite a single document at the desired location
     *
     * @param docRef          a Firestore reference to a document
     * @param document        data that will be uploaded at the address referred by collectionReference,
     *                        *                 ideally it should be a Map<String, Object>. If not, each custom class must
     *                        *                 have a public constructor that takes no arguments, and in addition, the
     *                        *                 class must include a public getter for each property
     * @param successListener the behaviour in case of success
     * @param failureListener the behaviour in case of failure
     */
    public abstract void writeDocumentWithID(DocumentReference docRef,
                                             Object document,
                                             OnSuccessListener successListener,
                                             OnFailureListener failureListener);

    /**
     * Read all the documents saved in the specified collection
     *
     * @param collectionReference a Firestore reference to a collection
     * @param handler             model the behaviour in case of success or failure
     */
    public abstract void readCollection(CollectionReference collectionReference,
                                        QueryHandler<QuerySnapshot> handler);

    /**
     * Read a single document
     *
     * @param documentReference a Firestone reference to a document
     * @param handler           model the behaviour in case of success or failure
     */
    public abstract void readDocument(DocumentReference documentReference,
                                      QueryHandler<DocumentReference> handler);

    //////////////////////////////////////////////////////////////

    public CollectionReference collectionReference(String path) {
        return firestore.collection(path);
    }

    public DocumentReference documentReference(String path, String documentID) {
        return collectionReference(path).document(documentID);
    }

    public void writeDocument(String path, Object document,
                              OnSuccessListener successListener,
                              OnFailureListener failureListener) {
        writeDocument(collectionReference(path), document, successListener, failureListener);
    }

    public void writeDocument(String path, Object document, Callback<Optional<Exception>> callback) {
        writeDocument(path, document, onSuccessBuilder(callback), onFailureBuilder(callback));
    }

    public void writeDocument(CollectionReference collectionReference, Object document,
                              Callback<Optional<Exception>> callback) {
        writeDocument(collectionReference, document, onSuccessBuilder(callback), onFailureBuilder(callback));
    }

    public void writeDocumentWithID(String path, String documentID,
                                    Object document,
                                    OnSuccessListener successListener,
                                    OnFailureListener failureListener) {
        writeDocumentWithID(documentReference(path, documentID), document, successListener,
                failureListener);
    }

    public void writeDocumentWithID(String path, String documentID, Object document,
                                    Callback<Optional<Exception>> callback) {
        writeDocumentWithID(path, documentID, document, onSuccessBuilder(callback),
                onFailureBuilder(callback));
    }

    public void writeDocumentWithID(DocumentReference documentReference, Object document,
                                    Callback<Optional<Exception>> callback) {
        writeDocumentWithID(documentReference, document, onSuccessBuilder(callback),
                onFailureBuilder(callback));
    }

    public void readCollection(String path, QueryHandler handler) {
        readCollection(collectionReference(path), handler);
    }

    public void readCollection(String path, Callback<Map<String, Map<String, Object>>> callback) {
        readCollection(path, queryHandlerBuilder(callback));
    }

    public void readCollection(CollectionReference collectionReference,
                               Callback<Map<String, Map<String, Object>>> callback) {
        readCollection(collectionReference, queryHandlerBuilder(callback));
    }

    public void readDocument(String path, String documentID, Callback callback) {
        readDocument(documentReference(path, documentID), callback);
    }

    public void readDocument(DocumentReference documentReference, Callback callback) {
        readDocument(documentReference, singleQueryHandlerBuilder(callback));
    }

    public void readDocument(String path, String documentID, QueryHandler<DocumentReference> handler) {
        readDocument(documentReference(path, documentID), handler);
    }

    private QueryHandler<QuerySnapshot> queryHandlerBuilder(Callback<Map<String, Map<String, Object>>> callback) {
        return new QueryHandler<QuerySnapshot>() {
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

    private QueryHandler<DocumentSnapshot> singleQueryHandlerBuilder(Callback<Map<String, Object>> callback) {
        return new QueryHandler<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot snapshot) {
                callback.onCallback(snapshot.getData());
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

    OnCompleteListener onCompleteListenerBuilder(QueryHandler handler) {
        return task -> {
            if (task.isSuccessful()) {
                handler.onSuccess(task.getResult());
            } else {
                handler.onFailure();
            }
        };
    }
}