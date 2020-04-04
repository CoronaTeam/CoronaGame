package ch.epfl.sdp.firestore;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.QueryHandler;

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

    public void readCollection(CollectionReference collectionReference, QueryHandler handler) {
        try {
            serverIdlingResource.increment();
            collectionReference
                    .get()
                    .addOnCompleteListener(querySnapshotOnCompleteListener(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocument(DocumentReference documentReference, Callback callback) {
        try {
            serverIdlingResource.increment();
            documentReference
                    .get()
                    .addOnCompleteListener(documentSnapshotOnCompleteListener(callback));
        } finally {
            serverIdlingResource.decrement();
        }
    }
}