package ch.epfl.sdp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static android.content.ContentValues.TAG;

public class MockFireBaseInteractor {
    private FirebaseFirestore db;

    public MockFireBaseInteractor(FirebaseFirestore firebaseFirestore) {
        db = firebaseFirestore;
    }

    public void addFirestoreUser(final Callback callback) {
        //Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Bob Bobby");
        int age = new Random().nextInt();
        user.put("Age", 24);
        user.put("Infected", false);

        // Add a new document with a generated ID
        db.collection("Players")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        callback.onCallback("Document snapshot successfully added to firestore.");
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onCallback("Error adding document to firestore.");
                Log.w(TAG, "Error adding document", e);
            }
        });
    }

    public void readFirestoreData(final Callback callback) {
        db.collection("LastPositions")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                callback.onCallback(document.getId() + " => " + document.getData());
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                        } else {
                            callback.onCallback("Error getting firestone documents.");
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }
}
