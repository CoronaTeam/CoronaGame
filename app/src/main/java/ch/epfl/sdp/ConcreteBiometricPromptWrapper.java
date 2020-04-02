package ch.epfl.sdp;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class ConcreteBiometricPromptWrapper implements BiometricPromptWrapper {
    BiometricPrompt biometricPrompt;


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
