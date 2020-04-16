package ch.epfl.sdp.firestore;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

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

    public <T> CompletableFuture<T> readDocument(DocumentReference documentReference,
                                                 Class<T> classType) {
        CompletableFuture<DocumentSnapshot> completableFuture;
        try {
            serverIdlingResource.increment();
            Task<DocumentSnapshot> task = documentReference.get();
            completableFuture = taskToFuture(task);
        } finally {
            serverIdlingResource.decrement();
        }
        //return completableFuture.thenApplyAsync(doc -> doc.toObject(classType));
        return completableFuture.thenApply(
                doc -> {
                    if (doc.exists()) {
                        // FIXME: doc.toObject fails silently
                        //return doc.toObject(classType);
                        // -> java.lang.RuntimeException: Could not deserialize object. Class java.util.Map has generic type parameters, please use GenericTypeIndicator instead
                        // but GenericTypeIndicator is not implemented in the library, see https://github.com/firebase/firebase-android-sdk/issues/222
                        return (T)doc.getData();
                    } else {
                        throw new RuntimeException("Document does not exist ");
                    }
                });
    }
}