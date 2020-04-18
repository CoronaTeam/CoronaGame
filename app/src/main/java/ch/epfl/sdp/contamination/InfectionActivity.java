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

import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.R;
import ch.epfl.sdp.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

import static ch.epfl.sdp.AuthenticationManager.getActivity;

public class InfectionActivity extends AppCompatActivity {


    private static GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();

    //TODO : remove those static attributes when GPS becomes a service
    private static ConcreteCachingDataSender sender = new ConcreteCachingDataSender(gridInteractor);
    private static DataReceiver receiver = new ConcreteDataReceiver(gridInteractor);
    private static InfectionAnalyst analyst = new ConcreteAnalysis(new Layman(Carrier.InfectionStatus.HEALTHY,0, AuthenticationManager.getUserId(getActivity())), receiver,sender);
    private static ConcretePositionAggregator aggregator = new ConcretePositionAggregator(sender,analyst);

    private TextView infectionStatus;
    private ProgressBar infectionProbability;
    private long lastUpdateTime;


    // TODO: The following 2 static methods must be deleted when GpsActivity is turned into a background service
    public static DataReceiver getReceiver() {
        return receiver;
    }
    public static CachingDataSender getSender(){
        return sender;
    }

    @VisibleForTesting
    void setReceiver(DataReceiver receiver) {
        InfectionActivity.receiver = receiver;
    }

    public static InfectionAnalyst getAnalyst() {
        return analyst;
    }
    public static PositionAggregator getAggregator(){
        return aggregator;
    }

    @VisibleForTesting
    void setAnalyst(InfectionAnalyst analyst) {
        InfectionActivity.analyst = analyst;
        aggregator.setAnalyst(analyst);
    }

    private LocationService service;


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
