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
    ConcreteFirestoreInteractor fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        FirestoreWrapper wrapper;
        if (intent.hasExtra("wrapper")){
            wrapper = (FirestoreWrapper) intent.getSerializableExtra("wrapper");
        }else{
            wrapper = new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance());
        }
        fs = new ConcreteFirestoreInteractor(wrapper);

    }

    public void addUser1(View view) {
        final TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText("Uploading ...");
            fs.writeDocument(outputView::setText);
        } else {
            outputView.setText(R.string.Can_t_Upload_Offline);
        }
    }

    public void readData1(View view) {
        final TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText("Downloading ...");
            fs.readDocument(outputView::setText);
        } else {
            outputView.setText(R.string.Can_t_Download_Offline);
        }
    }
}

