package ch.epfl.sdp;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;

public class FirestoreInteractor {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public FirestoreInteractor(FirebaseFirestore firebaseFirestore){
        db = firebaseFirestore;
    }

    public void addUser(final Callback callback) {
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
                        callback.onCallback("Document snapshot successfully added");
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onCallback("Error adding document");
                Log.w(TAG, "Error adding document", e);
            }
        });
    }




}
