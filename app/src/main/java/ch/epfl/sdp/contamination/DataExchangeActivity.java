package ch.epfl.sdp.contamination;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import ch.epfl.sdp.AuthenticationManager;
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
        sender = new ConcreteDataSender(new GridFirestoreInteractor(),
                AuthenticationManager.getAccount(this));
        //TODO .setOnSuccessListener(successListener).setOnFailureListener(failureListener);

        setContentView(R.layout.activity_dataexchange);
        exchangeStatus = findViewById(R.id.exchange_status);
        exchangeContent = findViewById(R.id.exchange_content);

        receiver = new ConcreteDataReceiver(
                new GridFirestoreInteractor());
    }
}
