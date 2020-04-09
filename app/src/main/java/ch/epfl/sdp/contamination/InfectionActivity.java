package ch.epfl.sdp.contamination;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.SortedMap;

import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.R;
import ch.epfl.sdp.fragment.AccountFragment;

import static ch.epfl.sdp.AuthenticationManager.getActivity;

public class InfectionActivity extends AppCompatActivity {

    private static GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();

    //TODO : remove those static attributes when GPS becomes a service
    private static ConcreteDataSender sender = new ConcreteDataSender(gridInteractor);
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
    }

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
    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();


        // TODO: Which location?
        receiver.getMyLastLocation(AccountFragment.getAccount(this), location -> {
            analyst.updateInfectionPredictions(location, refreshTime, n -> {
                InfectionActivity.this.runOnUiThread(() -> {
                    infectionStatus.setText(analyst.getCarrier().getInfectionStatus().toString());
                    infectionProbability.setProgress(Math.round(analyst.getCarrier().getIllnessProbability() * 100));
                    Log.e("PROB:", analyst.getCarrier().getIllnessProbability() + "");
                });
            });
        });
    }
}
