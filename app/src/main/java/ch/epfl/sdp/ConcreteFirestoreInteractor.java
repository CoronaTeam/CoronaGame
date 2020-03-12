package ch.epfl.sdp;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.content.ContentValues.TAG;

public class ConcreteFirestoreInteractor implements FirestoreInteractor {
    private FirestoreWrapper db;

    public ConcreteFirestoreInteractor(FirestoreWrapper firestoreFirestoreWrapper) {
        db = firestoreFirestoreWrapper;
    }

    //TODO: extend with arg map
    public void writeDocument(Callback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);
        // Add a new document with a generated ID
        db.collection("Players")
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    callback.onCallback("Document snapshot successfully added to firestore.");
                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                }).addOnFailureListener(e -> {
            callback.onCallback("Error adding document to firestore.");
            Log.w(TAG, "Error while adding document", e);
        });
    }



    public void readDocument(Callback callback) {
        db.collection("LastPositions")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            callback.onCallback(document.getId() + " => " + document.getData());
                            Log.d(TAG, document.getId() + " => " + document.getData());
                        }
                    } else {
                        callback.onCallback("Error getting firestone documents.");
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }
}
