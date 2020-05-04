package ch.epfl.sdp.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.BiometricPromptWrapper;
import ch.epfl.sdp.BiometricUtils;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.ConcreteBiometricPromptWrapper;
import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.Carrier;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.location.LocationService;

import static android.content.Context.BIND_AUTO_CREATE;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;
import static ch.epfl.sdp.contamination.CachingDataSender.privateRecoveryCounter;
import static ch.epfl.sdp.contamination.CachingDataSender.privateUserFolder;

public class UserInfectionFragment extends Fragment implements View.OnClickListener {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView onlineStatusView;
    private Button refreshButton;
    private Account account;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "User Infection Activity";
    private String userName;
    private View view;
    private LocationService service;

    private Executor executor;
    private BiometricPromptWrapper biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = inflater.inflate(R.layout.fragment_user_infection, container, false);

        infectionStatusView = view.findViewById(R.id.infectionStatusView);
        infectionStatusButton = view.findViewById(R.id.infectionStatusButton);
        infectionStatusButton.setOnClickListener(this);

        checkOnline();
        getLoggedInUser();

        this.executor = ContextCompat.getMainExecutor(getActivity());
        Intent intent = getActivity().getIntent();

        if (BiometricUtils.canAuthenticate(getActivity())) {
            if (intent.hasExtra("wrapper")) {
                this.biometricPrompt = (BiometricPromptWrapper) intent.getSerializableExtra("wrapper");
            } else {
                this.biometricPrompt = biometricPromptBuilder(this.executor);
            }
            this.promptInfo = promptInfoBuilder();
        }
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                UserInfectionFragment.this.service = ((LocationService.LocationBinder)service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        getActivity().bindService(new Intent(getActivity(), LocationService.class), conn, BIND_AUTO_CREATE);


        sharedPref = getActivity().getSharedPreferences("UserInfectionPrefFile", Context.MODE_PRIVATE);

        return view;
    }

    @Override
    public void onClick(View view) {
        Date currentTime = Calendar.getInstance().getTime();
        /* get 1 jan 1970 by default. It's definitely wrong but works as we want t check that
         * the status has not been updated less than a day ago.
         */
        Date lastStatusChange = new Date(sharedPref.getLong("lastStatusChange", 0));
        long difference = Math.abs(currentTime.getTime() - lastStatusChange.getTime());
        long differenceDays = difference / (24 * 60 * 60 * 1000);

        if(differenceDays > 1){
            switch (view.getId()) {
                case R.id.infectionStatusButton: {
                    onClickChangeStatus(view);
                } break;
                case R.id.refreshButton: {
                    onClickRefresh(view);
                } break;
            }
            sharedPref.edit().putLong("lastStatusChange", currentTime.getTime()).apply();
        }else {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Your health seems to be changing fast, Ignoring", Toast.LENGTH_LONG).show();
        }

    }

    public void onClickChangeStatus(View view) {
        if (checkOnline()) {
            if (BiometricUtils.canAuthenticate(getActivity())) {
                biometricPrompt.authenticate(promptInfo);
            } else {
                executeHealthStatusChange();
            }
        }
    }

    public void onClickRefresh(View view) {
        checkOnline();
    }

    private boolean checkOnline() {
        onlineStatusView = view.findViewById(R.id.onlineStatusView);
        refreshButton = view.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(this);
        checkNetworkStatus(getActivity());
        setOnlineOfflineVisibility(IS_ONLINE);
        return IS_ONLINE;
    }

    private void setOnlineOfflineVisibility(boolean isOnline) {
        int onlineVisibility = isOnline ? View.VISIBLE : View.INVISIBLE;
        int offlineVisibility = isOnline ? View.INVISIBLE : View.VISIBLE;
        onlineStatusView.setVisibility(offlineVisibility);
        refreshButton.setVisibility(offlineVisibility);
        infectionStatusButton.setVisibility(onlineVisibility);
        infectionStatusView.setVisibility(onlineVisibility);
    }

    private void getLoggedInUser() {
        account = AuthenticationManager.getAccount(getActivity());
        userName = account.getDisplayName();
        retrieveUserInfectionStatus(this::setInfectionColorAndMessage);
    }

    private void executeHealthStatusChange() {
        CharSequence buttonText = infectionStatusButton.getText();
        boolean infected = buttonText.equals(getResources().getString(R.string.i_am_infected));
        if (infected) {
            //Tell the analyst we are now sick !
            service.getAnalyst().updateStatus(Carrier.InfectionStatus.INFECTED);
            setInfectionColorAndMessage(true);
            modifyUserInfectionStatus(userName, true,
                    value -> {
                        //infectionUploadView.setText(String.format("%s at %s", value, Calendar.getInstance().getTime()));
                    });
        } else {
            //Tell analyst we are now healthy !
            service.getAnalyst().updateStatus(Carrier.InfectionStatus.HEALTHY);
            sendRecoveryToFirebase();
            setInfectionColorAndMessage(false);
            modifyUserInfectionStatus(userName, false,
                    value -> {
                        //infectionUploadView.setText(String.format("%s at %s", value, Calendar.getInstance().getTime()))
                    });
        }
    }

    private void sendRecoveryToFirebase() {
        DocumentReference ref = FirestoreInteractor.documentReference(privateUserFolder,account.getId());
        ref.update(privateRecoveryCounter, FieldValue.increment(1));
    }

    public void modifyUserInfectionStatus(String userPath, Boolean infected, Callback<String> callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("Infected", infected);
        db.collection("Users").document(userPath)
                .set(user, SetOptions.merge());

        DocumentReference userRef = db.collection("Users").document(userPath);

        userRef
                .update("Infected", infected)
                .addOnSuccessListener(documentReference ->
                        callback.onCallback(getString(R.string.user_status_update)))
                .addOnFailureListener(e ->
                        callback.onCallback(getString(R.string.error_status_update)));
    }

    public void retrieveUserInfectionStatus(Callback<Boolean> callbackBoolean) {
        db.collection("Users").document(userName).get().addOnSuccessListener(documentSnapshot ->
        {
            Log.d(TAG, "Infected status successfully loaded.");
            Object infected = documentSnapshot.get("Infected");
            if (infected == null) {
                callbackBoolean.onCallback(false);
            } else {
                callbackBoolean.onCallback((boolean) infected);
            }
        })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error retrieving infection status from Firestore.", e));
    }

    private void setInfectionColorAndMessage(boolean infected) {
        int buttonTextID = infected ? R.string.i_am_cured : R.string.i_am_infected;
        int messageID = infected ? R.string.your_user_status_is_set_to_infected :
                R.string.your_user_status_is_set_to_not_infected;
        int colorID = infected ? R.color.colorRedInfected : R.color.colorGreenCured;
        clickAction(infectionStatusButton, infectionStatusView, buttonTextID,
                messageID, colorID);
    }

    private void clickAction(Button button, TextView textView, int buttonText, int textViewText, int textColor) {
        button.setText(buttonText);
        textView.setTextColor(getResources().getColorStateList(textColor, getActivity().getTheme()));
        textView.setText(textViewText);
    }

    private BiometricPromptWrapper biometricPromptBuilder(Executor executor) {
        return new ConcreteBiometricPromptWrapper(new BiometricPrompt(
                UserInfectionFragment.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                displayNegativeButtonToast(errorCode);
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                executeAndDisplayAuthSuccessToast();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                displayAuthFailedToast();
            }
        }));
    }

    private BiometricPrompt.PromptInfo promptInfoBuilder() {
        return new BiometricPrompt.PromptInfo.Builder()
                .setConfirmationRequired(true)
                .setTitle("Biometric authentication")
                .setSubtitle("Confirm your health status change")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void displayAuthFailedToast() {
        Toast.makeText(getActivity().getApplicationContext(), "Authentication failed",
                Toast.LENGTH_SHORT)
                .show();
    }

    private void displayNegativeButtonToast(int errorCode) {
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Come back when sure about your health status!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void executeAndDisplayAuthSuccessToast() {
        Toast.makeText(getActivity().getApplicationContext(),
                "Authentication succeeded!", Toast.LENGTH_SHORT).show();
        executeHealthStatusChange();
    }
    public LocationService getLocationService(){
        return service;
    }
    @VisibleForTesting
    public boolean isImmediatelyNowIll(){
        CharSequence buttonText = infectionStatusButton.getText();
        boolean healthy = buttonText.equals(getResources().getString(R.string.i_am_infected));
        return !healthy;
    }

}