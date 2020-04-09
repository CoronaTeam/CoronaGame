package ch.epfl.sdp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.fragment.AccountFragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.reflect.Field;
import java.util.Map;

public class AuthenticationManager {

    /* TODO : REMOVE THIS METHOD AS SOON AS WE DO A GETCONTEXT OR THAT IT IS NOT USED ANYMORE
        This method was found on the internet for getting the current activity
     */
    public static Activity getActivity() {
        try {
            Class activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);

            Map<Object, Object> activities = (Map<Object, Object>) activitiesField.get(activityThread);
            if (activities == null)
                return null;

            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (Exception e) {
            return null;    //there should not be any exception, if so, try another way for getting the activity.
        }
        return null;
    }

    public static Account getAccount(Activity activity) {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(activity);
        return getNonNullAccount(acct);
    }
    public static String getUserId(Activity activity){
        return getAccount(activity).getId();
    }

    public static GoogleSignInClient getGoogleClient(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        return GoogleSignIn.getClient(activity, gso);
    }

    private static Account getNonNullAccount(GoogleSignInAccount acct) {
        if (acct == null) {
            User u = new User();//generic test user
            return new AccountFactory(u);
        } else {
            return new AccountFactory(acct);
        }
    }

    public static void signOut(Activity activity) {
        if (getAccount(activity).isGoogle()) {
            getGoogleClient(activity).signOut()
                    .addOnCompleteListener(activity, task -> {
                        Toast.makeText(activity, "Signed out successfully!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(activity, Authentication.class);// New activity
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //clears this activity's stack
                        activity.startActivity(intent);
                        activity.finish();
                    });
        } else {
            //no need to sign out from google, just go to the other activity
            // do not toast during test !
            Intent intent = new Intent(activity, Authentication.class);// New activity
            activity.startActivity(intent);
            activity.finish();
        }

    }

}