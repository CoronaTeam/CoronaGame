package ch.epfl.sdp.firestore;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConcreteFirestoreInteractor extends FirestoreInteractor {
    private final CountingIdlingResource serverIdlingResource;

    public ConcreteFirestoreInteractor() {
        this.serverIdlingResource = new CountingIdlingResource("firestoreCountingResource");
    }

    public void writeDocument(CollectionReference collectionReference, Object document,
                              OnSuccessListener successListener, OnFailureListener failureListener) {
        try {
            serverIdlingResource.increment();
            collectionReference
                    .add(document)
                    .addOnSuccessListener(successListener)
                    .addOnFailureListener(failureListener);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void writeDocumentWithID(DocumentReference documentReference, Object document,
                                    OnSuccessListener successListener, OnFailureListener failureListener) {
        try {
            serverIdlingResource.increment();
            documentReference
                    .set(document).addOnSuccessListener(successListener)
                    .addOnFailureListener(failureListener);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    @Override
    public void readDocument(DocumentReference documentReference, QueryHandler<DocumentReference> handler) {

    }

    public void readCollection(CollectionReference collectionReference, QueryHandler handler) {
        try {
            serverIdlingResource.increment();
            collectionReference
                    .get()
                    .addOnCompleteListener(onCompleteListenerBuilder(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    /**
     *
     * @param documentReference A reference to a firestore Document
     * @return A map containing all the pairs (fieldName, fieldValue).
     */
    public CompletableFuture<Map<String, Object>> readDocument(DocumentReference documentReference) {
        try {
            serverIdlingResource.increment();
            Task<DocumentSnapshot> task = documentReference.get();
            CompletableFuture<DocumentSnapshot> completableFuture = taskToFuture(task);
            return completableFuture.thenApply(
                    doc -> {
                        if (doc.exists()) {
                            return doc.getData();
                        } else {
                            throw new RuntimeException("Document does not exist ");
                        }
                    }).handle(((stringObjectMap, throwable) -> stringObjectMap != null ?
                    stringObjectMap : new HashMap<>()));
        } finally {
            serverIdlingResource.decrement();
        }

    }

    /**
     * @param collectionReference A reference to a firestore Collection
     * @return A map containing all the pairs (DocumentID, DocumentData).
     */
    public CompletableFuture<Map<String, Map<String, Object>>> readCollection(CollectionReference collectionReference) {
        try {
            serverIdlingResource.increment();
            Task<QuerySnapshot> collectionTask = collectionReference.get();
            CompletableFuture<QuerySnapshot> completableFuture = taskToFuture(collectionTask);
            return completableFuture.thenApply(
                    collection -> {
                        if (collection.isEmpty()){
                            throw new RuntimeException("Collection doesn't contain any document");
                        } else{
                            List<DocumentSnapshot> list = collection.getDocuments();
                            Map<String, Map<String, Object>> result = new HashMap<>();
                            for (DocumentSnapshot doc : list) {
                                result.put(doc.getId(), doc.getData());
                            }
                            return result;
                        }
                    }
            ).handle((stringMapMap, throwable) -> stringMapMap != null ? stringMapMap :
                    new HashMap<>());
        } finally {
            serverIdlingResource.decrement();
        }
    }
}