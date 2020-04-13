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

import ch.epfl.sdp.R;
import ch.epfl.sdp.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

public class InfectionActivity extends AppCompatActivity {

    private TextView infectionStatus;
    private ProgressBar infectionProbability;
    private long lastUpdateTime;

    private LocationService service;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infection);

        infectionStatus = findViewById(R.id.my_infection_status);
        infectionProbability = findViewById(R.id.my_infection_probability);

        /*
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()));
        analyst = new ConcreteAnalysis(new Layman(Carrier.InfectionStatus.HEALTHY), new ConcreteDataReceiver(gridInteractor));
        receiver = new ConcreteDataReceiver(gridInteractor);
         */

        lastUpdateTime = System.currentTimeMillis();

        infectionStatus.setText("Refresh to see your status");

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InfectionActivity.this.service = ((LocationService.LocationBinder)service).getService();
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
        service.getReceiver().getMyLastLocation(AccountFragment.getAccount(this), location -> {
            service.getAnalyst().updateInfectionPredictions(location, refreshTime, n -> {
                InfectionActivity.this.runOnUiThread(() -> {
                    InfectionAnalyst analyst = service.getAnalyst();
                    infectionStatus.setText(analyst.getCarrier().getInfectionStatus().toString());
                    infectionProbability.setProgress(Math.round(analyst.getCarrier().getIllnessProbability() * 100));
                    Log.e("PROB:", analyst.getCarrier().getIllnessProbability() + "");
                });
            });
        });
    }

    @VisibleForTesting
    LocationService getLocationService() {
        return service;
    }
}
