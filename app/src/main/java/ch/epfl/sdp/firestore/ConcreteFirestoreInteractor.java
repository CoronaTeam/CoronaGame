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

    private static Map<String, Map<String, Object>> parseCollection(QuerySnapshot collection) {
        List<DocumentSnapshot> list = collection.getDocuments();
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (DocumentSnapshot doc : list) {
            result.put(doc.getId(), doc.getData());
        }
        return result;
    }

    @Override
    public CompletableFuture<Map<String, Object>> readDocument(DocumentReference documentReference) {

        return taskToFuture(documentReference.get())
                .thenApply(
                        doc -> {
                            if (doc.exists()) {
                                return doc.getData();
                            } else {
                                // Document does not exist
                                return Collections.emptyMap();
                            }
                        });
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
                        return parseCollection(collection);
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