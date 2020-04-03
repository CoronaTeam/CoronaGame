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

import ch.epfl.sdp.AccountGetting;
import ch.epfl.sdp.ConcreteFirestoreWrapper;
import ch.epfl.sdp.R;

public class InfectionActivity extends AppCompatActivity {

    private TextView infectionStatus;
    private ProgressBar infectionProbability;

    private static GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()));
    private static InfectionAnalyst analyst = new ConcreteAnalysis(new Layman(Carrier.InfectionStatus.HEALTHY), new ConcreteDataReceiver(gridInteractor));;
    private static DataReceiver receiver = new ConcreteDataReceiver(gridInteractor);

    private long lastUpdateTime;

    // TODO: The following 2 static methods must be deleted when GpsActivity is turned into a background service
    public static DataReceiver getReceiver() {
        return receiver;
    }

    public static InfectionAnalyst getAnalyst() {
        return analyst;
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

    @VisibleForTesting
    void setAnalyst(InfectionAnalyst analyst) {
        this.analyst = analyst;
    }

    @VisibleForTesting
    void setReceiver(DataReceiver receiver) {
        this.receiver = receiver;
    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();


        // TODO: Which location?
        receiver.getMyLastLocation(AccountGetting.getAccount(this), location -> {
            analyst.updateInfectionPredictions(location, refreshTime, n -> {
                InfectionActivity.this.runOnUiThread(() -> {
                    infectionStatus.setText(analyst.getCarrier().getInfectionStatus().toString());
                    infectionProbability.setProgress(Math.round(analyst.getCarrier().getIllnessProbability()*100));
                    Log.e("PROB:", analyst.getCarrier().getIllnessProbability() + "");
                });
            });
        });
    }
}
