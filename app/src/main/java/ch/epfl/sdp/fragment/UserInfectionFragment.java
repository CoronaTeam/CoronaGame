package ch.epfl.sdp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.Account;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.BiometricPromptWrapper;
import ch.epfl.sdp.BiometricUtils;
import ch.epfl.sdp.ConcreteBiometricPromptWrapper;
import ch.epfl.sdp.R;
import ch.epfl.sdp.User;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class UserInfectionFragment extends Fragment implements View.OnClickListener {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView onlineStatusView;
    private Account account;
    private User user;
    private String userName;

    private Executor executor;
    private BiometricPromptWrapper biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private View view;

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

        infectionStatusView.setSaveEnabled(true);
        infectionStatusButton.setSaveEnabled(true);

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

        return view;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView infectionText = view.findViewById(R.id.infectionStatusView);
        Button infectionButton = view.findViewById(R.id.infectionStatusButton);
        outState.putCharSequence("INFECTION_STATUS_TEXT", infectionText.getText());
        outState.putCharSequence("INFECTION_STATUS_BUTTON", infectionButton.getText());
        infectionStatusButton.setOnClickListener(v -> statusButtonAction((Button) v));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.infectionStatusButton:
                onClickChangeStatus(v);
                break;
            // other buttons...
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
        checkNetworkStatus(getActivity());
        setOnlineOfflineVisibility(IS_ONLINE);
        return IS_ONLINE;
    }

    private void setOnlineOfflineVisibility(boolean isOnline) {
        int onlineVisibility = isOnline ? View.VISIBLE : View.INVISIBLE;
        int offlineVisibility = isOnline ? View.INVISIBLE : View.VISIBLE;
        onlineStatusView.setVisibility(offlineVisibility);
        infectionStatusButton.setVisibility(onlineVisibility);
        infectionStatusView.setVisibility(onlineVisibility);
    }

    private void getLoggedInUser() {
        account = AuthenticationManager.getAccount(getActivity());
        userName = account.getDisplayName();
        user = new User(userName, account.getFamilyName(), account.getEmail(),
                account.getPhotoUrl(), account.getPlayerId(getActivity()), account.getId(), User.DEFAULT_AGE, false);
        user.retrieveUserInfectionStatus(
                value -> setView(infectionStatusButton, infectionStatusView, value));
    }

    private void statusButtonAction(Button b) {
        checkOnline();
        CharSequence buttonText = b.getText();
        if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
            setView(infectionStatusButton, infectionStatusView, true);
            //upload to firebase
            user.modifyUserInfectionStatus(userName, true,
                    value -> {});


        } else {
            setView(infectionStatusButton, infectionStatusView, false);
            //upload to firebase
            user.modifyUserInfectionStatus(userName, false,
                    value -> {});
        }
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

    private void executeHealthStatusChange() {
        CharSequence buttonText = infectionStatusButton.getText();
        if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
            setInfectionColorAndMessage(R.string.i_am_cured,
                    R.string.your_user_status_is_set_to_infected, R.color.colorRedInfected, true);
        } else {
            setInfectionColorAndMessage(R.string.i_am_infected,
                    R.string.your_user_status_is_set_to_not_infected, R.color.colorGreenCured,
                    false);
        }
    }

    private void setInfectionColorAndMessage(int buttonTextID, int messageID, int colorID,
                                             boolean infected) {
        clickAction(infectionStatusButton, infectionStatusView, buttonTextID,
                messageID, colorID);
        user.modifyUserInfectionStatus(userName, infected,
                value -> {});
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

    private void clickAction(Button button, TextView textView, int buttonText, int textViewText, int buttonColor) {
        button.setText(buttonText);
        textView.setTextColor(getResources().getColorStateList(buttonColor));
        textView.setText(textViewText);
    }

    private void setView(Button button, TextView textView, boolean infected) {
        int buttonText = infected ? R.string.i_am_cured : R.string.i_am_infected;
        int textViewText = infected ? R.string.your_user_status_is_set_to_infected : R.string.your_user_status_is_set_to_not_infected;
        int buttonColor = infected ? R.color.colorRedInfected : R.color.colorGreenCured;
        button.setText(buttonText);
        textView.setTextColor(getResources().getColorStateList(buttonColor));
        textView.setText(textViewText);
    }
}
