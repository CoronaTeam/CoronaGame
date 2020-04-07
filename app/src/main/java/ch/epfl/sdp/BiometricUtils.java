package ch.epfl.sdp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class BiometricUtils {


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
