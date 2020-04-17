package ch.epfl.sdp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static ch.epfl.sdp.MainActivity.checkNetworkStatus;

public class UserInfectionActivity extends AppCompatActivity {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView infectionUploadView;
    private TextView userNameView;
    private TextView onlineStatusView;
    private Button refreshButton;
    private Account account;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "User Infection Activity";
    private String userName;

    private Executor executor;
    private BiometricPromptWrapper biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        infectionStatusView = findViewById(R.id.infectionStatusView);
        infectionStatusButton = findViewById(R.id.infectionStatusButton);
        infectionUploadView = findViewById(R.id.infectionStatusUploadConfirmation);
        userNameView = findViewById(R.id.userName);

        checkOnline();
        getLoggedInUser();

        infectionStatusView.setSaveEnabled(true);
        infectionStatusButton.setSaveEnabled(true);

        this.executor = ContextCompat.getMainExecutor(this);
        Intent intent = getIntent();

        if (BiometricUtils.canAuthenticate(this)) {
            if (intent.hasExtra("wrapper")) {
                this.biometricPrompt = (BiometricPromptWrapper) intent.getSerializableExtra("wrapper");
            } else {
                this.biometricPrompt = biometricPromptBuilder(this.executor);
            }
            this.promptInfo = promptInfoBuilder();
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        outState.putCharSequence("INFECTION_STATUS_TEXT", infectionText.getText());
        outState.putCharSequence("INFECTION_STATUS_BUTTON", infectionButton.getText());
        infectionStatusButton.setOnClickListener(v -> statusButtonAction((Button) v));
    }

    public void onClickChangeStatus(View view) {
        if (checkOnline()) {
            if (BiometricUtils.canAuthenticate(this)) {
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
        onlineStatusView = findViewById(R.id.onlineStatusView);
        refreshButton = findViewById(R.id.refreshButton);
        checkNetworkStatus(this);
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
        infectionUploadView.setVisibility(onlineVisibility);
        userNameView.setVisibility(onlineVisibility);
    }

    private void getLoggedInUser() {
        account = AuthenticationManager.getAccount(this);
        userName = account.getDisplayName();
        userNameView.setText(userName);
        retrieveUserInfectionStatus(
                value -> setView(infectionStatusButton, infectionStatusView, value));
    }

    private void statusButtonAction(Button b) {
        checkOnline();
        CharSequence buttonText = b.getText();
        if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
            setView(infectionStatusButton, infectionStatusView, true);
            //upload to firebase
            modifyUserInfectionStatus(userName, true,
                    value -> infectionUploadView.setText(value));


        } else {
            setView(infectionStatusButton, infectionStatusView, false);
            //upload to firebase
            modifyUserInfectionStatus(userName, false,
                    value -> infectionUploadView.setText(value));
        }
    }

    private BiometricPromptWrapper biometricPromptBuilder(Executor executor) {
        return new ConcreteBiometricPromptWrapper(new BiometricPrompt(
                UserInfectionActivity.this,
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

    public void modifyUserInfectionStatus(String userPath, Boolean infected, Callback<String> callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("Infected", infected);
        db.collection("Users").document(userPath)
                .set(user, SetOptions.merge());

        DocumentReference userRef = db.collection("Users").document(userPath);

        userRef
                .update("Infected", infected)
                .addOnSuccessListener(documentReference ->
                        callback.onCallback("User infection status successfully updated!"))
                .addOnFailureListener(e ->
                        callback.onCallback("Error updating user infection status."));
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

    private void setInfectionColorAndMessage(int buttonTextID, int messageID, int colorID,
                                             boolean infected) {
        clickAction(infectionStatusButton, infectionStatusView, buttonTextID,
                messageID, colorID);
        modifyUserInfectionStatus(userName, infected,
                value -> infectionUploadView.setText(String.format("%s at %s", value,
                        Calendar.getInstance().getTime())));
    }

    private void displayAuthFailedToast() {
        Toast.makeText(getApplicationContext(), "Authentication failed",
                Toast.LENGTH_SHORT)
                .show();
    }

    private void displayNegativeButtonToast(int errorCode) {
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
            Toast.makeText(getApplicationContext(),
                    "Come back when sure about your health status!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void executeAndDisplayAuthSuccessToast() {
        Toast.makeText(getApplicationContext(),
                "Authentication succeeded!", Toast.LENGTH_SHORT).show();
        executeHealthStatusChange();
    }

    private void clickAction(Button button, TextView textView, int buttonText, int textViewText, int textColor) {
        button.setText(buttonText);
        textView.setTextColor(getResources().getColorStateList(textColor, this.getTheme()));
        textView.setText(textViewText);
    }

    private void setView(Button button, TextView textView, boolean infected) {
        int buttonText = infected ? R.string.i_am_cured : R.string.i_am_infected;
        int textViewText = infected ? R.string.your_user_status_is_set_to_infected : R.string.your_user_status_is_set_to_not_infected;
        int textColor = infected ? R.color.colorRedInfected : R.color.colorGreenCured;
        button.setText(buttonText);
        textView.setTextColor(getResources().getColorStateList(textColor, this.getTheme()));
        textView.setText(textViewText);
    }
}
