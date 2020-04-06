package ch.epfl.sdp.contamination;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.Callback;

import ch.epfl.sdp.ConcreteFirestoreWrapper;
import ch.epfl.sdp.R;

public class DataExchangeActivity extends AppCompatActivity {

    // TODO: This activity will be converted into a Service

    private DataSender sender;
    private DataReceiver receiver;

    private TextView exchangeStatus;
    private TextView exchangeContent;

    private OnSuccessListener successListener = o -> {
        exchangeStatus.setText("EXCHANGE Succeeded");
    };

    private OnFailureListener failureListener = e -> {
        exchangeStatus.setText("EXCHANGE Failed");
    };


    @VisibleForTesting
    DataSender getSender() {
        return sender;
    }

    @VisibleForTesting
    DataReceiver getReceiver() {
        return receiver;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sender = new ConcreteDataSender(new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance())),
                AuthenticationManager.getAccount(this)).setOnSuccessListener(successListener).setOnFailureListener(failureListener);

        setContentView(R.layout.activity_dataexchange);
        exchangeStatus = findViewById(R.id.exchange_status);
        exchangeContent = findViewById(R.id.exchange_content);

        receiver = new ConcreteDataReceiver(
                new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance())));
    }
}
