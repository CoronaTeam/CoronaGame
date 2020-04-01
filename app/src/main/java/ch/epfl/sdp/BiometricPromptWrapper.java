package ch.epfl.sdp;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

public interface BiometricPromptWrapper {

    void authenticate(@NonNull BiometricPrompt.PromptInfo info);

    void cancelAuthentication();
}
