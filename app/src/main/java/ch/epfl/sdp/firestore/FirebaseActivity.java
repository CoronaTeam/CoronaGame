package ch.epfl.sdp.firestore;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.R;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class FirebaseActivity extends AppCompatActivity {
    private FirestoreInteractor fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);
        fs = new ConcreteFirestoreInteractor();
    }

    public void addUser1(View view) {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Ed Edward");
        user.put("Age", 104);
        user.put("Infected", false);

        TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            CollectionReference collectionReference = fs.collectionReference("Players");
            fs.writeDocument(collectionReference, user)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            outputView.setText("Unexpected error" + throwable);
                        } else {
                            outputView.setText(R.string.docSnap_success_upload);
                        }
                    });
        } else {
            outputView.setText(R.string.can_t_Upload_Offline);
        }
    }

    public void addUser2(View view) {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Aly Alice");
        user.put("Age", 42);
        user.put("Infected", true);

        TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            DocumentReference documentReference = fs.documentReference("Players",
                    String.valueOf(new Random().nextInt()));
            fs.writeDocumentWithID(documentReference, user)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            outputView.setText("Unexpected error" + throwable);
                        } else {
                            outputView.setText(R.string.docSnap_success_upload);
                        }
                    });
        } else {
            outputView.setText(R.string.can_t_Upload_Offline);
        }

    }

    public void readData2(View view) throws ExecutionException, InterruptedException, TimeoutException {
        DocumentReference documentReference = fs.documentReference("Tests/FirebaseActivity" +
                "/Download", "DownloadTest");
        TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText(R.string.downloading);
            fs.readDocument(documentReference).thenAccept(s -> outputView.setText(s.toString()));
        } else {
            outputView.setText(R.string.can_t_Download_Offline);
        }
    }

    public void readData1(View view) {
        CollectionReference collectionReference = fs.collectionReference("Tests/FirebaseActivity" +
                "/Download");
        TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText(R.string.downloading);
            fs.readCollection(collectionReference).thenAccept(s -> outputView.setText(s.toString()));
        } else {
            outputView.setText(R.string.can_t_Download_Offline);
        }
    }

    private void databaseOperation(int outputViewID, int duringOperation,
                                   int offlineMessage, Callback<TextView> callback) {
        final TextView outputView = findViewById(outputViewID);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText(duringOperation);
            callback.onCallback(outputView);
        } else {
            outputView.setText(offlineMessage);
        }
    }
}