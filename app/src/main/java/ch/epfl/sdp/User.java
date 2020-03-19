package ch.epfl.sdp;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class User implements Account {
    public static String DEFAULT_DISPLAY_NAME = "MyDisplayName";
    public static String DEFAULT_FAMILY_NAME = "MyFamilyName";
    public static String DEFAULT_EMAIL = "MyEmal@epfl.ch";
    public static String DEFAULT_PLAYERID = "MyPlayerId";
    public static String DEFAULT_USERID = "USER_ID:X42";
   // public static String url_string = "https://pbs.twimg.com/profile_images/1173987553885556736/WuLwZF3C_400x400.jpg";
    public static Uri DEFAULT_URI = Uri.parse("https://upload.wikimedia.org/wikipedia/commons/9/9a/Gull_portrait_ca_usa.jpg");
           //Uri.parse("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png");
           //Uri.parse("https://i.pinimg.com/236x/51/bf/9c/51bf9c7fdf0d4303140c4949afd1d7b8--baby-kitty-little-kitty.jpg");
        //  Uri.parse("https://pbs.twimg.com/profile_images/1173987553885556736/WuLwZF3C_400x400.jpg");
    String displayName;
    String familyName;
    String email;
    Uri photoUrl;
    String playerId;
    String userID;
    public User(String dName, String fName, String email, Uri photoUrl, String playerId,String userID){
        this.displayName = dName;
        this.email = email;
        this.familyName = fName;
        this.photoUrl = photoUrl;
        this.playerId = playerId;
        this.userID = userID;
    }
    public User(){
        this(DEFAULT_DISPLAY_NAME, DEFAULT_FAMILY_NAME, DEFAULT_EMAIL, DEFAULT_URI,DEFAULT_PLAYERID,DEFAULT_USERID);
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
    public String getPlayerId(Activity activity) {
        return this.playerId;
    } // activity argument not used: not clean :(

    @Override
    public GoogleSignInAccount getAccount(){return null;}

    @Override
    public String getId() {
        return this.userID;
    }
import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class User {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Map<String, Object> user = new HashMap<>();
    private static final String TAG = "UserInfectionActivity";

    public User(String userName, int userAge, boolean infected) {
        user.put("Name", userName);
        user.put("Age", userAge);
        user.put("Infected", infected);
        db.collection("Users")
                    .add(user)
                    .addOnSuccessListener(documentReference -> Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId()))
                    .addOnFailureListener(e -> Log.w(TAG, "Error adding document", e));
    }

    public void modifyUserInfectionStatus(String userPath, Boolean infected, Callback callback) {
        user.put("Infected", infected);
        db.collection("Users").document(userPath)
                .set(user, SetOptions.merge());

        DocumentReference userRef = db.collection("Users").document(userPath);

        userRef
                .update("Infected", infected)
                .addOnSuccessListener(documentReference -> callback.onCallback("User infection status successfully updated!"))
                .addOnFailureListener(e -> callback.onCallback("Error updating user infection status."));
    }


}
