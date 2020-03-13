package ch.epfl.sdp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class FirebaseActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseActivity";
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
        fs = new ConcreteFirestoreInteractor(wrapper);

    }

    public void addUser(View view) {
        databaseOperation(R.id.FirebaseUploadConfirmation, R.string.uploading,
                R.string.Can_t_Upload_Offline, e -> fs.writeDocument(e::setText));
    }

    public void readData(View view) {
        databaseOperation(R.id.FirebaseDownloadResult, R.string.downloading,
                R.string.Can_t_Download_Offline, e -> fs.readDocument(e::setText));
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

