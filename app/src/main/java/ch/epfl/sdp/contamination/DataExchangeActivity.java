package ch.epfl.sdp.contamination;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import ch.epfl.sdp.R;
import ch.epfl.sdp.location.LocationService;


public class DataExchangeActivity extends AppCompatActivity {

    // TODO: This activity will be converted into a Service


    private CachingDataSender sender;
    private DataReceiver receiver;

    private LocationService service;


    private TextView exchangeStatus;

    OnSuccessListener successListener = o -> {
        exchangeStatus.setText("EXCHANGE Succeeded");
    };

    OnFailureListener failureListener = e -> {
        exchangeStatus.setText("EXCHANGE Failed");
    };



    @VisibleForTesting
    CachingDataSender getSender() {
        return sender;
    }

    @VisibleForTesting
    public LocationService getService() {
        return service;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        sender =                new ConcreteCachingDataSender(new GridFirestoreInteractor())
//                .setOnSuccessListener(successListener).setOnFailureListener(failureListener);

        setContentView(R.layout.activity_dataexchange);
        exchangeStatus = findViewById(R.id.exchange_status);

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationService.LocationBinder binder = (LocationService.LocationBinder) service;
                DataExchangeActivity.this.service = binder.getService();
                DataExchangeActivity.this.sender = DataExchangeActivity.this.service.getSender();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                DataExchangeActivity.this.service = null;
                DataExchangeActivity.this.sender = null;
            }
        };
        bindService(new Intent(this, LocationService.class), conn, BIND_AUTO_CREATE);
    }
}
