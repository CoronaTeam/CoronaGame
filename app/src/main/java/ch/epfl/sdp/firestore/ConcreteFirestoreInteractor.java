package ch.epfl.sdp.firestore;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ConcreteFirestoreInteractor extends FirestoreInteractor {
    private final CountingIdlingResource serverIdlingResource;

    public ConcreteFirestoreInteractor() {
        this.serverIdlingResource = new CountingIdlingResource("firestoreCountingResource");
    }

    public CompletableFuture<Map<String, Object>> readDocument(@NotNull DocumentReference documentReference) {
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
                    stringObjectMap : Collections.emptyMap()));
        } finally {
            serverIdlingResource.decrement();
        }

    }

    public CompletableFuture<Map<String, Map<String, Object>>> readCollection(@NotNull CollectionReference collectionReference) {
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
                    Collections.emptyMap());
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public CompletableFuture<DocumentReference> writeDocument(@NotNull CollectionReference collectionReference, Object document) {
        try {
            serverIdlingResource.increment();
            Task<DocumentReference> documentReferenceTask = collectionReference.add(document);
            return taskToFuture(documentReferenceTask);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public CompletableFuture<Void> writeDocumentWithID(@NotNull DocumentReference documentReference, Object document) {
        try {
            serverIdlingResource.increment();
            Task<Void> writeTask = documentReference.set(document);
            return taskToFuture(writeTask);
        } finally {
            serverIdlingResource.decrement();
        }
    }
}