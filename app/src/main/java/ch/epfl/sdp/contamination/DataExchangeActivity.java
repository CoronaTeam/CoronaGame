package ch.epfl.sdp.contamination;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import ch.epfl.sdp.R;
import ch.epfl.sdp.location.LocationService;


public class DataExchangeActivity extends AppCompatActivity {

    // TODO: This activity will be converted into a Service

    private LocationService service;

    @VisibleForTesting
    TextView exchangeStatus;

    @VisibleForTesting
    Handler uiHandler;

    @VisibleForTesting
    public LocationService getService() {
        return service;
    }

    @VisibleForTesting
    public ServiceConnection serviceConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dataexchange);
        exchangeStatus = findViewById(R.id.exchange_status);

        uiHandler = new Handler();

        bindLocationService();
    }

    @VisibleForTesting
    public void bindLocationService() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationService.LocationBinder binder = (LocationService.LocationBinder) service;
                DataExchangeActivity.this.service = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                DataExchangeActivity.this.service = null;
            }
        };

        bindService(new Intent(this, LocationService.class), serviceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }
}
