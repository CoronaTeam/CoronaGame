package ch.epfl.sdp.biometric;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.core.app.ActivityCompat;

/**
 * This class includes some utilities used when dealing with biometric authentication
 */
public class BiometricUtils {

    /**
     * Check if biometric authentication is available and can be used by the app
     *
     * @param context The Context in which the method is executed
     * @return a boolean which correspond to the possibility or not to use biometric authentication
     */
    public static boolean canAuthenticate(Context context) {
        if (!isPermissionGranted(context))
            return false;

        BiometricManager biometricManager = BiometricManager.from(context);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("MY_APP_TAG", "No biometric features available on this device.");
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e("MY_APP_TAG", "The user hasn't associated " +
                        "any biometric credentials with their account.");
        }
        return false;
    }

    private static boolean isPermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context,
                Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED;
    }
}
