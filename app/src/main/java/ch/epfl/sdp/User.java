package ch.epfl.sdp;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class User {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Map<String, Object> user = new HashMap<>();
    private static final String TAG = "UserInfectionActivity";

    public User(String userName, int userAge, boolean infected) {
        user.put("Name", userName);
        user.put("Age", userAge);
        user.put("Infected", infected);
        db.collection("Users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    public void modifyUserInfectionStatus(String userPath, Boolean infected, Callback callback) {
        user.put("Infected", infected);
        db.collection("Users").document(userPath)
                .set(user, SetOptions.merge());

        DocumentReference userRef = db.collection("Users").document(userPath);

        userRef
                .update("Infected", infected)
                .addOnSuccessListener(documentReference -> callback.onCallback("User infection status successfully updated!"))
                .addOnFailureListener(e -> callback.onCallback("Error updating user infection status."));
    }


}
