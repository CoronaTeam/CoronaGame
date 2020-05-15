package ch.epfl.sdp;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.Serializable;

public interface Account extends Serializable {
    String getDisplayName();
    String getFamilyName();
    String getEmail();
    Uri getPhotoUrl();
    Boolean isGoogle();
    String getPlayerId(Activity activity);
    GoogleSignInAccount getAccount();
    String getId();
}