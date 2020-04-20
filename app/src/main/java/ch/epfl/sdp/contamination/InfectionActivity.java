package ch.epfl.sdp.contamination;

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

import ch.epfl.sdp.R;
import ch.epfl.sdp.fragment.AccountFragment;

public class InfectionActivity extends AppCompatActivity {

    private static GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor();
    private static InfectionAnalyst analyst = new ConcreteAnalysis(new Layman(Carrier.InfectionStatus.HEALTHY), new ConcreteDataReceiver(gridInteractor));
    private static DataReceiver receiver = new ConcreteDataReceiver(gridInteractor);
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
        receiver.getMyLastLocation(AccountFragment.getAccount(this)).thenApplyAsync(location -> {
            return analyst.updateInfectionPredictions(location, refreshTime).thenAccept(n ->{
                infectionStatus.setText(analyst.getCarrier().getInfectionStatus().toString());
                infectionProbability.setProgress(Math.round(analyst.getCarrier().getIllnessProbability() * 100));
                Log.e("PROB:", analyst.getCarrier().getIllnessProbability() + "");
            });
        });
    }
}
