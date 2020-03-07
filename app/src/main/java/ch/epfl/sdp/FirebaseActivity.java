package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;

public class FirebaseActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseActivity";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirestoreInteractor fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        fs = new FirestoreInteractor(FirebaseFirestore.getInstance());
    }

    public void addUser1(View view) {
        final TextView outputView = findViewById(R.id.FirebaseUploadConfirmation);
        checkNetworkStatus();
        if (IS_ONLINE) {
            outputView.setText("Uploading ...");
            fs.addFirestoreUser(new Callback() {
                @Override
                public void onCallback(String value) {
                    outputView.setText(value);
                }
            });
        } else {
            outputView.setText(R.string.Can_t_Upload_Offline);
        }
    }

    public void readData1(View view) {
        final TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        checkNetworkStatus();
        if (IS_ONLINE) {
            outputView.setText("Downloading ...");
            fs.readFirestoreData(new Callback() {
                @Override
                public void onCallback(String value) {
                    outputView.setText(value);
                }
            });
        } else {
            outputView.setText(R.string.Can_t_Download_Offline);
        }
    }

    public void checkNetworkStatus() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        IS_ONLINE = (activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting());
    }
}

