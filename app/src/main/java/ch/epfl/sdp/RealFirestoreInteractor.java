package ch.epfl.sdp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RealFirestoreInteractor {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addUser(Map<String, Object> user, Callback callback) {
        db.collection("Users")       //cast to FirebaseFirestore because FirestoreWrapper doesn't have same methods as real FirebaseFirestore
                    .add(user)
                    .addOnSuccessListener(userReference -> callback.onCallback(
                            "User snapshot successfully added to firestore."))
                    .addOnFailureListener(e -> callback.onCallback(
                            "Error adding user to firestore."));
    }

    public void modifyUserInfectionStatus(String userPath, Boolean infected) {
        Map<String, Object> data = new HashMap<>();
        data.put("Infected", infected);

        db.collection("Users").document(userPath)
                .set(data, SetOptions.merge());
    }
}
