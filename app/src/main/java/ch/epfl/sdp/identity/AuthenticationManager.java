package ch.epfl.sdp.identity;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

public class AuthenticationManager {

    public static DefaultAuthenticationManager defaultManager = new DefaultAuthenticationManager() {
    };

    public static Activity getActivity() {
        return defaultManager.getActivity();
    }

    public static Account getAccount(Context context) {
        return defaultManager.getAccount(context);
    }

    public static String getUserId() {
        return defaultManager.getUserId();
    }

    public static GoogleSignInClient getGoogleClient(Activity activity) {
        return defaultManager.getGoogleClient(activity);
    }

    public static Account getNonNullAccount(GoogleSignInAccount account) {
        return defaultManager.getNonNullAccount(account);
    }

    public static void signOut(Activity activity) {
        defaultManager.signOut(activity);
    }
}