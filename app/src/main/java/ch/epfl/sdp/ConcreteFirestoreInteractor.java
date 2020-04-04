package ch.epfl.sdp;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

public class ConcreteFirestoreInteractor extends FirestoreInteractor {
    final CountingIdlingResource serverIdlingResource;

    public ConcreteFirestoreInteractor() {
        this.serverIdlingResource = new CountingIdlingResource("firestoreCountingResource");
    }

    public void writeDocument(CollectionReference collectionReference, Map<String, Object> document,
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

    public void writeDocumentWithID(DocumentReference documentReference, Map<String, Object> document,
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

    public void readDocument(CollectionReference collectionReference, QueryHandler handler) {
        try {
            serverIdlingResource.increment();
            collectionReference
                    .get()
                    .addOnCompleteListener(querySnapshotOnCompleteListener(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocumentWithID(DocumentReference documentReference, Callback callback) {
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