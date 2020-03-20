package ch.epfl.sdp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class UserInfectionActivity extends AppCompatActivity {

    private Button infectionStatusButton;
    private TextView infectionStatusView;
    private TextView infectionUploadView;
    private User user = new User("Test", User.DEFAULT_FAMILY_NAME, User.DEFAULT_EMAIL,
            User.DEFAULT_URI, User.DEFAULT_PLAYERID, User.DEFAULT_USERID, 25, false);
    private String userName = "Test"; // TODO: this is temporary: we need to get the real current user

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_infection);

        infectionStatusView = findViewById(R.id.infectionStatusView);
        infectionStatusButton = findViewById(R.id.infectionStatusButton);
        infectionUploadView = findViewById(R.id.infectionStatusUploadConfirmation);

        infectionStatusView.setSaveEnabled(true);
        infectionStatusButton.setSaveEnabled(true);

        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(UserInfectionActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(),
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build();

        infectionStatusButton.setOnClickListener(v -> {
            CharSequence buttonText = ((Button)v).getText();
            if (buttonText.equals(getResources().getString(R.string.i_am_infected))) {
                biometricPrompt.authenticate(promptInfo);
                clickAction(infectionStatusButton, infectionStatusView, R.string.i_am_cured,
                        R.string.your_user_status_is_set_to_infected, R.color.colorGreenCured);
                //upload to firebase
                user.modifyUserInfectionStatus(userName, true,
                        value -> infectionUploadView.setText(value));
            }
            else {
                biometricPrompt.authenticate(promptInfo);
                clickAction(infectionStatusButton, infectionStatusView, R.string.i_am_infected,
                        R.string.your_user_status_is_set_to_not_infected, R.color.colorRedInfected);
                //upload to firebase
                user.modifyUserInfectionStatus(userName, false,
                        value -> infectionUploadView.setText(value));
            }
        });
    }



    private void clickAction(Button button, TextView textView, int buttonText, int textViewText, int buttonColor) {
        button.setText(buttonText);
        button.setBackgroundTintList(
                getResources().getColorStateList(buttonColor));
        textView.setText(textViewText);
    }


    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        outState.putCharSequence("INFECTION_STATUS_TEXT", infectionText.getText());
        outState.putCharSequence("INFECTION_STATUS_BUTTON", infectionButton.getText());
    }

    @Override
    public void onRestoreInstanceState(@NotNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        TextView infectionText = findViewById(R.id.infectionStatusView);
        Button infectionButton = findViewById(R.id.infectionStatusButton);
        CharSequence DEFAULT_INFECTION_TEXT = getResources().getString(R.string.your_user_status_is_set_to_not_infected);
        CharSequence DEFAULT_INFECTION_BUTTON = getResources().getString(R.string.i_am_infected);
        infectionText.setText(savedInstanceState
                .getCharSequence("INFECTION_STATUS_TEXT", DEFAULT_INFECTION_TEXT));
        infectionButton.setText(savedInstanceState
                .getCharSequence("INFECTION_STATUS_BUTTON", DEFAULT_INFECTION_BUTTON));
    }
}
