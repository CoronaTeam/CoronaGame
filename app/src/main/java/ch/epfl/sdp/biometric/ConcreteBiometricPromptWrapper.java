package ch.epfl.sdp.biometric;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

/**
 * This class create an instance of a BiometricPromptWrapper
 */
public class ConcreteBiometricPromptWrapper implements BiometricPromptWrapper {
    private BiometricPrompt biometricPrompt;

    /**
     * Constructor for a concrete instance of BiometricPromptWrapper
     * @param realBiometricPrompt a real biometricPrompt
     */
    public ConcreteBiometricPromptWrapper(BiometricPrompt realBiometricPrompt) {
        this.biometricPrompt = realBiometricPrompt;
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
