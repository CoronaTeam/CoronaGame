package ch.epfl.sdp.contamination;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.R;
import ch.epfl.sdp.fragment.AccountFragment;

public class InfectionFragment extends Fragment {

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
        InfectionFragment.receiver = receiver;
    }

    public static InfectionAnalyst getAnalyst() {
        return analyst;
    }

    @VisibleForTesting
    void setAnalyst(InfectionAnalyst analyst) {
        InfectionFragment.analyst = analyst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_infection, container, false);

        infectionStatus = view.findViewById(R.id.my_infection_status);
        infectionProbability = view.findViewById(R.id.my_infection_probability);

        /*
        GridFirestoreInteractor gridInteractor = new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()));
        analyst = new ConcreteAnalysis(new Layman(Carrier.InfectionStatus.HEALTHY), new ConcreteDataReceiver(gridInteractor));
        receiver = new ConcreteDataReceiver(gridInteractor);
         */

        lastUpdateTime = System.currentTimeMillis();

        infectionStatus.setText("Refresh to see your status");

        return view;
    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();


        // TODO: Which location?
        receiver.getMyLastLocation(AccountFragment.getAccount(getActivity()), location -> {
            analyst.updateInfectionPredictions(location, refreshTime, n -> {
                getActivity().runOnUiThread(() -> {
                    infectionStatus.setText(analyst.getCarrier().getInfectionStatus().toString());
                    infectionProbability.setProgress(Math.round(analyst.getCarrier().getIllnessProbability() * 100));
                    Log.e("PROB:", analyst.getCarrier().getIllnessProbability() + "");
                });
            });
        });
    }
}
