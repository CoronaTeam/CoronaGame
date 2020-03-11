package ch.epfl.sdp;

import android.net.Uri;

public interface Account {
    public String getDisplayName();
    public String getFamilyName();
    public String getEmail();
    public Uri getPhotoUrl();
}
