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

    private InfectionAnalyst analyst;
    private DataReceiver receiver;

    private long lastUpdateTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infection);

        infectionStatus = findViewById(R.id.my_infection_status);
        infectionProbability = findViewById(R.id.my_infection_probability);

        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()));
        analyst = new ConcreteAnalysis(new Layman(Carrier.InfectionStatus.HEALTHY), new ConcreteDataReceiver(gridInteractor));
        receiver = new ConcreteDataReceiver(gridInteractor);

        lastUpdateTime = System.currentTimeMillis();
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
