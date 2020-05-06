package ch.epfl.sdp.firestore;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This class is the main and more generic implementation of the FirestoreInteractor, every other
 * implementation should inherit from this one.
 */
public class ConcreteFirestoreInteractor extends FirestoreInteractor {

    /**
     * Default constructor
     */
    public ConcreteFirestoreInteractor() {
    }

    @Override
    public CompletableFuture<Map<String, Object>> readDocument(DocumentReference documentReference) {
        Task<DocumentSnapshot> task = documentReference.get();
        CompletableFuture<DocumentSnapshot> completableFuture = taskToFuture(task);
        return completableFuture.thenApply(doc -> {
            if (doc.exists()) {
                doc.getData();
                return doc.getData();
            } else {
                throw new RuntimeException("Document doesn't exist");
            }
        }).exceptionally(e -> Collections.emptyMap());
    }

    @Override
    public CompletableFuture<Map<String, Map<String, Object>>> readCollection(CollectionReference collectionReference) {
        Task<QuerySnapshot> collectionTask = collectionReference.get();
        CompletableFuture<QuerySnapshot> completableFuture = taskToFuture(collectionTask);
        return completableFuture
                .thenApply(collection -> {
                    if (collection.isEmpty()) {
                        throw new RuntimeException("Collection doesn't contain any document");
                    } else {
                        List<DocumentSnapshot> list = collection.getDocuments();
                        Map<String, Map<String, Object>> result = new HashMap<>();
                        for (DocumentSnapshot doc : list) {
                            result.put(doc.getId(), doc.getData());
                        }
                        return result;
                    }
                })
                .exceptionally(e -> Collections.emptyMap());
    }

    @Override
    public CompletableFuture<DocumentReference> writeDocument(CollectionReference collectionReference, Object document) {
        Task<DocumentReference> documentReferenceTask = collectionReference.add(document);
        return taskToFuture(documentReferenceTask);
    }

    @Override
    public CompletableFuture<Void> writeDocumentWithID(DocumentReference documentReference, Object document) {
        Task<Void> writeTask = documentReference.set(document);
        return taskToFuture(writeTask);
    }
}