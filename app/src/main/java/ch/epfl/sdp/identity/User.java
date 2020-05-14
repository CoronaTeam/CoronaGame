package ch.epfl.sdp.identity;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * This class is there for testing purposes and for representing a user in UserInfectionActivity
 */
public class User implements Account {
    public static String DEFAULT_DISPLAY_NAME = "MyDisplayName";
    public static String DEFAULT_FAMILY_NAME = "MyFamilyName";
    public static String DEFAULT_EMAIL = "MyEmal@epfl.ch";
    public static int DEFAULT_AGE = 25;
    public static String DEFAULT_USERID = "USER_ID_X42";
    // public static String url_string = "https://pbs.twimg.com/profile_images/1173987553885556736/WuLwZF3C_400x400.jpg";
    public static Uri DEFAULT_URI = Uri.parse("https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg");
    private String displayName;
    private String familyName;
    private String email;
    private Uri photoUrl;
    private String playerId;
    private String userID;
    private int age;
    private boolean  infected;


    public User(String dName, String fName, String email, Uri photoUrl,  String userID, int age, boolean infected) {
        this.displayName = dName;
        this.email = email;
        this.familyName = fName;
        this.photoUrl = photoUrl;
        this.userID = userID;
        this.age = age;
        this.infected = infected;
    }

    public User() {
        this(DEFAULT_DISPLAY_NAME, DEFAULT_FAMILY_NAME, DEFAULT_EMAIL, DEFAULT_URI, DEFAULT_USERID, DEFAULT_AGE, false);
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
    public GoogleSignInAccount getAccount() {
        return null;
    }

    @Override
    public String getId() {
        return this.userID;
    }

    public int getAge() {
        return age;
    }

}