package ch.epfl.sdp.firestore;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.Callback;

public abstract class FirestoreInteractor {
    static FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    /**
     * Read a single document
     *
     * @param documentReference A reference to a Firestore document
     * @return A future map containing all the pairs (fieldID, filedValue).
     */
    public abstract CompletableFuture<Map<String, Object>> readDocument(
            DocumentReference documentReference);

    /**
     * Read all the documents saved in the specified collection
     *
     * @param collectionReference A reference to a firestore Collection
     * @return A future map containing all the pairs (DocumentID, DocumentData).
     */
    public abstract CompletableFuture<Map<String, Map<String, Object>>> readCollection(
            CollectionReference collectionReference);

    /**
     * Create or overwrite a single document at the desired location
     *
     * @param documentReference A reference to a Firestore document
     * @param document          data that will be uploaded at the address referred by
     *                          collectionReference, ideally it should be a Map<String, Object>. If not,
     *                          each custom class must have a public constructor that takes no arguments,
     *                          and in addition, the
     * @return a future notification of how the success/failure of the operation
     */
    public abstract CompletableFuture<Void> writeDocumentWithID(DocumentReference documentReference,
                                                                Object document);

    /**
     * Add a new document to Firestore at the desired location. DocumentID will be automatically
     *
     * @param collectionReference A reference to a firestore Collection
     * @param document            data that will be uploaded at the address referred by
     *                            collectionReference, ideally it should be a Map<String, Object>. If not,
     *                            each custom class must have a public constructor that takes no arguments,
     *                            and in addition, the
     * @return a future documentReference of the uploaded document
     */
    public abstract CompletableFuture<DocumentReference> writeDocument(
            CollectionReference collectionReference, Object document);

    //////////////////////////////////////////////////////////////

    public static <T> CompletableFuture<T> taskToFuture(Task<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        task.addOnSuccessListener(value -> future.complete(value));
        task.addOnFailureListener(ex -> future.completeExceptionally(ex));
        return future;
    }

    public static CollectionReference collectionReference(String path) {
        return firestore.collection(path);
    }

    public static DocumentReference documentReference(String path, String documentID) {
        return collectionReference(path).document(documentID);
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