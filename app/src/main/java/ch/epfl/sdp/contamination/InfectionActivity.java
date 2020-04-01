package ch.epfl.sdp.contamination;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import ch.epfl.sdp.ConcreteFirestoreWrapper;
import ch.epfl.sdp.R;

public class InfectionActivity extends AppCompatActivity {

    private TextView infectionStatus;
    private ProgressBar infectionProbability;

    private InfectionAnalyst analyst;

    private long lastUpdateTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_infection);

        infectionStatus = findViewById(R.id.my_infection_status);
        infectionProbability = findViewById(R.id.my_infection_probability);

        // Create a Carrier, representing the app user
        Carrier me = new Layman(Carrier.InfectionStatus.HEALTHY);
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()));
        analyst = new ConcreteAnalysis(me, new ConcreteDataReceiver(gridInteractor));

        lastUpdateTime = System.currentTimeMillis();
    }

    public void onModelRefresh(View v) {

        //analyst.updateInfectionPredictions();
    }
}
