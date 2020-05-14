package ch.epfl.sdp.identity;

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
    GoogleSignInAccount getAccount();
    String getId();
}
