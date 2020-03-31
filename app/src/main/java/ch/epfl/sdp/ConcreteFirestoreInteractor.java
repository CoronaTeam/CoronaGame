package ch.epfl.sdp;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class ConcreteFirestoreInteractor extends FirestoreInteractor {
    final CountingIdlingResource serverIdlingResource;
    private final FirestoreWrapper db;

    public ConcreteFirestoreInteractor(FirestoreWrapper firestoreFirestoreWrapper,
                                       CountingIdlingResource firestoreServerIdlingResource) {
        this.db = firestoreFirestoreWrapper;
        this.serverIdlingResource = firestoreServerIdlingResource;
    }

    public void writeDocument(String path, Map<String, Object> document, Callback<String> callback) {
        writeDocument(path, document, onSuccessBuilder(callback), onFailureBuilder(callback));
    }

    public void writeDocument(String path, Map<String, Object> document,
                              OnSuccessListener onSuccess, OnFailureListener onFailure) {
        try {
            serverIdlingResource.increment();
            db.collection(path)
                    .add(document)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                                    Callback<String> callback) {
        writeDocumentWithID(path, documentID, document, onSuccessBuilder(callback),
                onFailureBuilder(callback));
    }

    public void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                                    OnSuccessListener onSuccess, OnFailureListener onFailure) {
        try {
            serverIdlingResource.increment();
            db.collection(path).document(documentID)
                    .set(document)
                    .addOnSetSuccessListener(onSuccess)
                    .addOnSetFailureListener(onFailure);
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocument(String path, Callback<String> callback) {
        readDocument(path, queryHandlerBuilder(callback));
    }

    public void readDocument(String path, QueryHandler handler) {
        try {
            serverIdlingResource.increment();
            db.collection(path)
                    .get()
                    .addOnCompleteListener(onCompleteBuilder(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    public void readDocumentWithID(String path, String documentID, Callback<String> callback) {
        readDocumentWithID(path, documentID, queryHandlerBuilder(callback));
    }

    public void readDocumentWithID(String path, String documentID, QueryHandler handler) {
        try {
            serverIdlingResource.increment();
            db.collection(path).document(documentID)
                    .get()
                    .addOnCompleteListener(onCompleteBuilder(handler));
        } finally {
            serverIdlingResource.decrement();
        }
    }

    private QueryHandler queryHandlerBuilder(Callback<String> callback) {
        return new QueryHandler() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                for (QueryDocumentSnapshot qs : snapshot) {
                    callback.onCallback(qs.getId() + " => " + qs.getData());
                }
            }

            @Override
            public void onFailure() {
                callback.onCallback("Error getting firestone documents.");
            }
        };
    }

    private OnSuccessListener onSuccessBuilder(Callback<String> callback) {
        return e -> callback.onCallback(
                "Document snapshot successfully added to firestore.");
    }

    private OnFailureListener onFailureBuilder(Callback<String> callback) {
        return e -> callback.onCallback(
                "Error adding document to firestore.");
    }

    private OnCompleteListener<QuerySnapshot> onCompleteBuilder(QueryHandler handler){
        return task -> {
            if (task.isSuccessful()) {
                handler.onSuccess(task.getResult());
            } else {
                handler.onFailure();
            }
        };
    }
}