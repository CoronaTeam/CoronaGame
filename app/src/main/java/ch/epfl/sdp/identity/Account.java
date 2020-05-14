package ch.epfl.sdp.identity;

import android.net.Uri;

import java.io.Serializable;

public interface Account extends Serializable {
    String getDisplayName();
    String getFamilyName();
    String getEmail();
    Uri getPhotoUrl();
    Boolean isGoogle();
    String getId();
}
