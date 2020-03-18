package ch.epfl.sdp;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ConcreteFirestoreInteractor extends FirestoreInteractor {
    final CountingIdlingResource serverIdlingResource;
    private final FirestoreWrapper db;

    public ConcreteFirestoreInteractor(FirestoreWrapper firestoreFirestoreWrapper,
                                       CountingIdlingResource firestoreServerIdlingResource) {
        this.db = firestoreFirestoreWrapper;
        this.serverIdlingResource = firestoreServerIdlingResource;
    }

    public void writeDocument(Callback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);
        // Add a new document with a generated ID
        try {
            serverIdlingResource.increment();
            db.collection("Players")
                    .add(user)
                    .addOnSuccessListener(documentReference -> callback.onCallback(
                            "Document snapshot successfully added to firestore."))
                    .addOnFailureListener(e -> callback.onCallback(
                            "Error adding document to firestore."));
        } finally {
            serverIdlingResource.decrement();
        }
    }


    public void readDocument(Callback callback) {
        try{
            serverIdlingResource.increment();
            db.collection("LastPositions")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                callback.onCallback(document.getId() + " => " + document.getData());
                            }
                        } else {
                            callback.onCallback("Error getting firestone documents.");
                        }
                    });
        } finally {
            serverIdlingResource.decrement();
        }

    }
}