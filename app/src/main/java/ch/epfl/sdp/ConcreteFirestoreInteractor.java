package ch.epfl.sdp;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ConcreteFirestoreInteractor extends FirestoreInteractor {
    final CountingIdlingResource serverIdlingResource;
    private final FirebaseFirestore db;

    public ConcreteFirestoreInteractor(FirebaseFirestore firebaseFirestore,
                                       CountingIdlingResource firestoreServerIdlingResource) {
        this.db = firebaseFirestore;
        this.serverIdlingResource = firestoreServerIdlingResource;
    }

    public void writeDocument(String path, Map<String, Object> document,
                              OnSuccessListener successListener, OnFailureListener failureListener) {
        try {
            serverIdlingResource.increment();
            db.collection(path)
                    .add(document)
                    .addOnSuccessListener(successListener)
                    .addOnFailureListener(failureListener);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                                    OnSuccessListener successListener, OnFailureListener failureListener) {
        try {
            serverIdlingResource.increment();
            db.collection(path).document(documentID)
                    .set(document).addOnSuccessListener(successListener)
                    .addOnFailureListener(failureListener);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocument(String path, QueryHandler handler) {
        try {
            serverIdlingResource.increment();
            db.collection(path)
                    .get()
                    .addOnCompleteListener(querySnapshotOnCompleteListener(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocumentWithID(String path, String documentID, Callback callback) {
        try {
            serverIdlingResource.increment();
            db.collection(path).document(documentID)
                    .get()
                    .addOnCompleteListener(documentSnapshotOnCompleteListener(callback));
        } finally {
            serverIdlingResource.decrement();
        }
    }
}