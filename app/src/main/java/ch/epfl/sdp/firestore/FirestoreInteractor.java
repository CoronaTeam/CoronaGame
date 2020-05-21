package ch.epfl.sdp.firestore;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This class represent an abstraction over the firestore database. It contains abstract methods
 * to be implemented in the instances to interact with firestore and a couple of static
 * utilities.
 */
public abstract class FirestoreInteractor {
    public static final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // TODO: @Ulysse, @Adrien, @Kevin, @Lucas, @Lucie use this function to get something from
    // maps returned by Firestore

    /**
     * Retrieves information taken from Firestore and stored in the map 'fields'
     *
     * @param fields
     * @param tagName The string to search for
     * @param tagType The type to which the information corresponds
     * @param <A>
     * @return
     * @throws ClassCastException       if the Object cannot be cast to A
     * @throws IllegalArgumentException if tagName does not exist
     */
    public static <A> A getTag(Map<String, Object> fields, String tagName, Class<A> tagType) throws ClassCastException {
        Object data = fields.getOrDefault(tagName, null);
        if (data == null) {
            throw new IllegalArgumentException(String.format("Tag %s not found", tagName));
        }
        return tagType.cast(data);
    }

    /**
     * Convert a task into a completableFuture
     *
     * @param task The task we want to convert to a CompletableFuture
     * @param <T>  The type of future required
     * @return a future behaving as the original task
     */
    public static <T> CompletableFuture<T> taskToFuture(Task<T> task) {

        if (task.isComplete()) {
            if (task.isSuccessful()) {
                return CompletableFuture.completedFuture(task.getResult());
            } else {
                CompletableFuture<T> exceptionFuture = new CompletableFuture<>();
                exceptionFuture.completeExceptionally(task.getException());
                return exceptionFuture;
            }
        } else {
            CompletableFuture<T> future = new CompletableFuture<>();
            task.addOnSuccessListener(value -> future.complete(value))
                    .addOnFailureListener(ex -> future.completeExceptionally(ex));
            return future;
        }
    }

    /**
     * Build a collectionReference from a string
     *
     * @param path a string formatted as xxx/xxx/xxx that represent the path to a collection in a
     *             firestore database.
     * @return a reference to that collection stored on firestore
     */
    public static CollectionReference collectionReference(String path) {
        return firestore.collection(path);
    }

    /**
     * Build  a documentReference from a string
     *
     * @param path       a string formatted as xxx/xxx/xxx that represent the path to a collection in a
     *                   firestore database.
     * @param documentID the id of the document
     * @return a reference to a document stored on firestore
     */
    public static DocumentReference documentReference(String path, String documentID) {
        return collectionReference(path).document(documentID);
    }

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
}