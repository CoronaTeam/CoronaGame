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

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class FirebaseActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseActivity";
    private ConcreteFirestoreInteractor fs;
    CountingIdlingResource countingResource;

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

    public void addUser(View view) {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);
        databaseOperation(R.id.FirebaseUploadConfirmation, R.string.uploading,
                R.string.Can_t_Upload_Offline, e -> fs.writeDocument("Players", user, e::setText));
    }

    public void readData(View view) {
        databaseOperation(R.id.FirebaseDownloadResult, R.string.downloading,
                R.string.Can_t_Download_Offline, e -> fs.readDocument("LastPositions", e::setText));
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
