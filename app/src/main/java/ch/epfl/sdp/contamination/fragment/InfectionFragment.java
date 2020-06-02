package ch.epfl.sdp.contamination.fragment;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.InfectionAnalyst;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * View displaying the probability of infection of the user
 */
public class InfectionFragment extends Fragment implements View.OnClickListener {

    private TextView infectionStatus;
    private ProgressBar infectionProbability;
    private long lastUpdateTime;

    private CompletableFuture<LocationService> service;

    private Handler uiHandler;

    public InfectionFragment(Handler uiHandler) {
        this.uiHandler = uiHandler;
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
        view.findViewById(R.id.my_infection_refresh).setOnClickListener(this);

        lastUpdateTime = System.currentTimeMillis();

        infectionStatus.setText(R.string.refresh_to_see_status);

        service = new CompletableFuture<>();

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service.complete(((LocationService.LocationBinder) binder).getService());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        requireActivity().bindService(new Intent(requireActivity(), LocationService.class), conn, BIND_AUTO_CREATE);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.my_infection_refresh) {
            onModelRefresh(view);
        }
    }

    public void onModelRefresh(View v) {

        Date refreshTime = new Date(lastUpdateTime);
        lastUpdateTime = System.currentTimeMillis();

        LocationService locationService = service.join();

        // TODO: Which location?
        locationService.getReceiver().getMyLastLocation(AccountFragment.getAccount(requireActivity()))
                .thenApply(location -> locationService.getAnalyst().updateInfectionPredictions(location, refreshTime, new Date())
                        .thenAccept(todayInfectionMeetings -> {
                            //TODO: should run on UI thread?
                            requireActivity().runOnUiThread(() -> {
                                infectionStatus.setText(R.string.infection_status_posted);
                                uiHandler.post(() -> {
                                    InfectionAnalyst analyst = locationService.getAnalyst();
                                    infectionStatus.setText(analyst.getCarrier().getInfectionStatus().toString());
                                    infectionProbability.setProgress(Math.round(analyst.getCarrier().getIllnessProbability() * 100));
                                    Log.e("PROB:", analyst.getCarrier().getIllnessProbability() + "");
                                    displayAlert(todayInfectionMeetings);
                                });
                            });
                        })
                        .join());
    }

    private void displayAlert(int todayInfectionMeetings) {
        if (todayInfectionMeetings < 0) {
            throw new IllegalArgumentException();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        CharSequence first;
        CharSequence second;
        CharSequence title = getText(R.string.infection_dialog_title);
        switch (todayInfectionMeetings) {
            case 0:
                first = getText(R.string.infection_dialog_cool_message);
                second = "";
                break;
            case 1:
                first = getText(R.string.infection_dialog_message1);
                second = getText(R.string.one_infection_dialog_message2);
                break;
            default:
                first = getText(R.string.infection_dialog_message1);
                second = getText(R.string.several_infection_dialog_message2);

        }
        builder.setMessage((String) first + (todayInfectionMeetings == 0 ? "" : todayInfectionMeetings) + (String) second)
                .setTitle(title);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @VisibleForTesting
    public CompletableFuture<LocationService> getLocationService() {
        return service;
    }

}
