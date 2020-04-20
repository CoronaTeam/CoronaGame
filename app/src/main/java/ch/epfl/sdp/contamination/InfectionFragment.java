package ch.epfl.sdp.contamination;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
import ch.epfl.sdp.location.LocationService;

import static android.content.Context.BIND_AUTO_CREATE;

public class InfectionFragment extends Fragment {

    private TextView infectionStatus;
    private ProgressBar infectionProbability;
    private long lastUpdateTime;

    private LocationService service;

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

        lastUpdateTime = System.currentTimeMillis();

        infectionStatus.setText("Refresh to see your status");

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InfectionFragment.this.service = ((LocationService.LocationBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        getActivity().bindService(new Intent(getActivity(), LocationService.class), conn, BIND_AUTO_CREATE);

        return view;
    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();


        // TODO: Which location?
        service.getReceiver().getMyLastLocation(AccountFragment.getAccount(getActivity()), location -> {
            service.getAnalyst().updateInfectionPredictions(location, refreshTime, n -> {
                getActivity().runOnUiThread(() -> {
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
