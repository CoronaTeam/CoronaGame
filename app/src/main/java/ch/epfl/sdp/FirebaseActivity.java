package ch.epfl.sdp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FirebaseActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseActivity";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
    }

    public void addUser(View view){
        //Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Ada Lovelace");
        user.put("Age", 19);
        user.put("Infected", false);

        // Add a new document with a generated ID
        db.collection("Players")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
                        outputView.setText(R.string.DocumentSnapshot_successfully_added);
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
                outputView.setText("Error adding the document");
                Log.w(TAG, "Error adding document", e);
            }
        });
    }

    public void readData(View view){
        db.collection("Players")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                TextView outputView = findViewById(R.id.FirebaseDownloadResult);
                                outputView.setText(String.format("%s => %s", document.getId(), document.getData()));
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }







}
