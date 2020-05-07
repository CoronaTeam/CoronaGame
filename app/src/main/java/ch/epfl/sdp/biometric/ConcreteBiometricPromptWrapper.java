package ch.epfl.sdp.biometric;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import ch.epfl.sdp.R;

/**
 * This class create an instance of a BiometricPromptWrapper
 */
public class ConcreteBiometricPromptWrapper implements BiometricPromptWrapper {
    private BiometricPrompt biometricPrompt;

    public ConcreteBiometricPromptWrapper(Fragment fragment, Executor executor,
                                          Activity activity,
                                          CompletableFuture<Void> run) {
        this.biometricPrompt = new BiometricPrompt(
                fragment, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        displayNegativeButtonToast(errorCode, activity);
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        executeAndDisplayAuthSuccessToast(activity).thenCompose(v -> run);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        displayAuthFailedToast(activity);
                    }
                });
    }

    private static void displayAuthFailedToast(Activity activity) {
        Toast.makeText(activity.getApplicationContext(), R.string.authentication_failed,
                Toast.LENGTH_SHORT)
                .show();
    }

    private static void displayNegativeButtonToast(int errorCode, Activity activity) {
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
            Toast.makeText(activity.getApplicationContext(),
                    R.string.bio_auth_negative_button_toast, Toast.LENGTH_LONG)
                    .show();
        }
    }

    private static CompletableFuture<Void> executeAndDisplayAuthSuccessToast(Activity activity) {
        return CompletableFuture.completedFuture(null).thenRun(() ->
                Toast.makeText(activity.getApplicationContext(),
                        R.string.bio_auth_success_toast, Toast.LENGTH_SHORT).show());
    }

    public static BiometricPrompt.PromptInfo promptInfoBuilder(Boolean confirmationRequired,
                                                               String title,
                                                               String subtitle,
                                                               String negativeButtonText) {
        return new BiometricPrompt.PromptInfo.Builder()
                .setConfirmationRequired(confirmationRequired)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build();
    }

    @Override
    public void authenticate(@NonNull BiometricPrompt.PromptInfo info) {
        this.biometricPrompt.authenticate(info);
    }

    @Override
    public void cancelAuthentication() {
        this.biometricPrompt.cancelAuthentication();
    }
}
