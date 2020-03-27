package ch.epfl.sdp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class FirebaseActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseActivity";
    CountingIdlingResource countingResource;
    private ConcreteFirestoreInteractor fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        // Get the Intent that started this activity and extract the string
        FirestoreWrapper wrapper;
        Intent intent = getIntent();
        if (intent.hasExtra("wrapper")) {
            wrapper = (FirestoreWrapper) intent.getSerializableExtra("wrapper");
        } else {
            wrapper = new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance());
        }
        this.countingResource = new CountingIdlingResource("FirestoreServerCalls");
        fs = new ConcreteFirestoreInteractor(wrapper, countingResource);

    }

    public void addUser1(View view) {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Bob Bobby");
        user.put("Age", (Object) 24);
        user.put("Infected", (Object) false);
        databaseOperation(R.id.FirebaseUploadConfirmation, R.string.uploading,
                R.string.Can_t_Upload_Offline, e -> fs.writeDocument("Players", user,
                        e::setText));
    }

    public void addUser2(View view){
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Aly Alice");
        user.put("Age", (Object)  42);
        user.put("Infected", (Object)  true);
        databaseOperation(R.id.FirebaseUploadConfirmation, R.string.uploading,
                R.string.Can_t_Upload_Offline, e -> fs.writeDocumentWithID("Players",
                        String.valueOf(new Random().nextInt()), user,
                        e::setText));
    }

    public void readData2(View view) {
        databaseOperation(R.id.FirebaseDownloadResult, R.string.downloading,
                R.string.Can_t_Download_Offline, e -> fs.readDocumentWithID("Tests", "DownloadTest",
                        e::setText));
    }

    public void readData1(View view){
        databaseOperation(R.id.FirebaseDownloadResult, R.string.downloading,
                R.string.Can_t_Download_Offline, e -> fs.readDocument("Tests",
                        e::setText));
    }

    private void databaseOperation(int outputViewID, int duringOperation,
                                   int offlineMessage, Operation op) {
        final TextView outputView = findViewById(outputViewID);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText(duringOperation);
            op.apply(outputView);
        } else {
            outputView.setText(offlineMessage);
        }
    }

    interface Operation {
        void apply(TextView output);
    }
}