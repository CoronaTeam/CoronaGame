package ch.epfl.sdp.firestore;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

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
                    .addOnCompleteListener(onCompleteListenerBuilder(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocument(DocumentReference documentReference,
                             QueryHandler<DocumentReference> handler) {
        try {
            serverIdlingResource.increment();
            documentReference
                    .get()
                    .addOnCompleteListener(onCompleteListenerBuilder(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }
}