package ch.epfl.sdp;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public interface Account {
    public String getDisplayName();
    public String getFamilyName();
    public String getEmail();
    public Uri getPhotoUrl();
    public Boolean isGoogle();
    public String getPlayerId(Activity activity);
    public GoogleSignInAccount getAccount();
    public String getId();
}
