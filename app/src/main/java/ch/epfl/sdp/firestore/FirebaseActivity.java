package ch.epfl.sdp.firestore;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;

import ch.epfl.sdp.R;

import static ch.epfl.sdp.Tools.IS_ONLINE;
import static ch.epfl.sdp.Tools.checkNetworkStatus;
import static ch.epfl.sdp.firestore.FirestoreInteractor.collectionReference;
import static ch.epfl.sdp.firestore.FirestoreInteractor.documentReference;

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
            CollectionReference collectionReference = collectionReference("Players");
            fs.writeDocument(collectionReference, user)
                    .whenComplete(getObjectThrowableBiConsumer(outputView));
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
            DocumentReference documentReference = documentReference("Players",
                    String.valueOf(new Random().nextInt()));
            fs.writeDocumentWithID(documentReference, user)
                    .whenComplete(getObjectThrowableBiConsumer(outputView));
        } else {
            outputView.setText(R.string.can_t_Upload_Offline);
        }
    }

    public void readData1(View view) {
        CollectionReference collectionReference = collectionReference("Tests/FirebaseActivity" +
                "/Download");
        TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText(R.string.downloading);
            fs.readCollection(collectionReference).thenAccept(result -> outputView.setText(result.toString()));
        } else {
            outputView.setText(R.string.can_t_Download_Offline);
        }
    }

    public void readData2(View view) {
        DocumentReference documentReference = documentReference("Tests/FirebaseActivity" +
                "/Download", "DownloadTest");
        TextView outputView = findViewById(R.id.FirebaseDownloadResult);
        checkNetworkStatus(this);
        if (IS_ONLINE) {
            outputView.setText(R.string.downloading);
            fs.readDocument(documentReference).thenAccept(result -> outputView.setText(result.toString()));
        } else {
            outputView.setText(R.string.can_t_Download_Offline);
        }
    }

    @NotNull
    private BiConsumer<Object, Throwable> getObjectThrowableBiConsumer(TextView outputView) {
        return (result, throwable) -> {
            if (throwable != null) {
                outputView.setText(String.format("%s%s", getString(R.string.unexpected_error),
                        throwable));
            } else {
                outputView.setText(R.string.docSnap_success_upload);
            }
        };
    }
}