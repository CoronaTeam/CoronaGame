package ch.epfl.sdp.firestore;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.R;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class FirebaseActivity extends AppCompatActivity {
    private ConcreteFirestoreInteractor fs;

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
        databaseOperation(R.id.FirebaseUploadConfirmation, R.string.uploading,
                R.string.can_t_Upload_Offline, textView -> fs.writeDocument("Players", user,
                        stringMapMap -> textView.setText(R.string.docSnap_success_upload)));
    }

    public void addUser2(View view){
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Aly Alice");
        user.put("Age", 42);
        user.put("Infected", true);
        databaseOperation(R.id.FirebaseUploadConfirmation, R.string.uploading,
                R.string.can_t_Upload_Offline, textView -> fs.writeDocumentWithID("Players",
                        String.valueOf(new Random().nextInt()), user,
                        stringMapMap -> textView.setText(R.string.docSnap_success_upload)));
    }

    public void readData2(View view) {
        databaseOperation(R.id.FirebaseDownloadResult, R.string.downloading,
                R.string.can_t_Download_Offline, textView -> fs.readDocument("Tests/FirebaseActivity/Download", "DownloadTest",
                        stringMapMap -> textView.setText(stringMapMap.toString())));
    }

    public void readData1(View view){
        databaseOperation(R.id.FirebaseDownloadResult, R.string.downloading,
                R.string.can_t_Download_Offline, textView -> fs.readCollection("Tests/FirebaseActivity/Download",
                        stringMapMap -> {
                            textView.setText(stringMapMap.toString());
                        }));
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