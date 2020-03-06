package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.IdlingResource;

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

public class FirebaseActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseActivity";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
    }

    public void addUser(View view) {
        final TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
        if (isConnected()) {
            //Create a new user with a first and last name
            Map<String, Object> user = new HashMap<>();
            user.put("Name", "Ada Lovelace");
            int age = new Random().nextInt();
            user.put("Age", age);
            user.put("Infected", false);

            // Add a new document with a generated ID
            db.collection("Players")
                    .add(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            outputView.setText(R.string.DocumentSnapshot_successfully_added);
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    outputView.setText("Error adding the document");
                    Log.w(TAG, "Error adding document", e);
                }
            });
        } else {
            outputView.setText(R.string.Can_t_Upload_Offline);
        }

    }

    public void readData(View view) {
        final TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        if (isConnected()) {
            db.collection("LastPositions")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                    outputView.setText(String.format("%s => %s", document.getId(), document.getData()));
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                }
                            } else {
                                Log.w(TAG, "Error getting documents.", task.getException());
                            }
                        }
                    });
        } else {
            outputView.setText(R.string.Can_t_Download_Offline);
        }

    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connMgr != null;
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public boolean isConnected(){
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }
}
