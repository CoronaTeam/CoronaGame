package ch.epfl.sdp;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class User {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Map<String, Object> user = new HashMap<>();

    public User() {}

    public User(String userName, int userAge, boolean infected) {
        user.put("Name", userName);
        user.put("Age", userAge);
        user.put("Infected", infected);
        db.collection("Users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    public void modifyUserInfectionStatus(String userPath, Boolean infected) {
        Map<String, Object> data = new HashMap<>();
        data.put("Infected", infected);

        db.collection("Users").document(userPath)
                .set(data, SetOptions.merge());
    }


}
