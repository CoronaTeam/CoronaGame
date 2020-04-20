package ch.epfl.sdp.contamination;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;

import androidx.fragment.app.Fragment;
import ch.epfl.sdp.R;
import ch.epfl.sdp.SingleFragmentActivity;
import ch.epfl.sdp.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

public class InfectionActivity extends SingleFragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infection);

        infectionStatus = findViewById(R.id.my_infection_status);
        infectionProbability = findViewById(R.id.my_infection_probability);

        lastUpdateTime = System.currentTimeMillis();

        infectionStatus.setText("Refresh to see your status");

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InfectionActivity.this.service = ((LocationService.LocationBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        bindService(new Intent(this, LocationService.class), conn, BIND_AUTO_CREATE);

    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();

        // TODO: Which location?
        service.getReceiver().getMyLastLocation(AccountFragment.getAccount(this)).thenApplyAsync(location -> {
            return service.getAnalyst().updateInfectionPredictions(location, refreshTime).thenAccept(n -> {
                infectionStatus.setText(service.getAnalyst().getCarrier().getInfectionStatus().toString());
                infectionProbability.setProgress(Math.round(service.getAnalyst().getCarrier().getIllnessProbability() * 100));
                Log.e("PROB:", service.getAnalyst().getCarrier().getIllnessProbability() + "");
            });
        });
    }

    @VisibleForTesting
    LocationService getLocationService () {
        return service;
    protected Fragment createFragment() {
        return new InfectionFragment();
    }
}