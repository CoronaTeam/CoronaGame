package ch.epfl.sdp.identity;

import android.net.Uri;


/**
 * This class is there for testing purposes and for representing a user in UserInfectionActivity
 */
public class User implements Account {
    public static final String DEFAULT_DISPLAY_NAME = "MyDisplayName";
    public static final String DEFAULT_FAMILY_NAME = "MyFamilyName";
    public static final String DEFAULT_EMAIL = "MyEmal@epfl.ch";
    public static final Uri DEFAULT_URI = Uri.parse("https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg");
    public static String DEFAULT_USERID = "USER_ID_X42";
    private final String displayName;
    private final String familyName;
    private final String email;
    private final Uri photoUrl;
    private final String userID;


    public User(String dName, String fName, String email, Uri photoUrl, String userID) {
        this.displayName = dName;
        this.email = email;
        this.familyName = fName;
        this.photoUrl = photoUrl;
        this.userID = userID;
    }

    public User() {
        this(DEFAULT_DISPLAY_NAME, DEFAULT_FAMILY_NAME, DEFAULT_EMAIL, DEFAULT_URI, DEFAULT_USERID);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Uri getPhotoUrl() {
        return photoUrl;
    }

    @Override
    public Boolean isGoogle() {
        return false;
    }

    @Override
    public String getId() {
        return this.userID;
    }

}