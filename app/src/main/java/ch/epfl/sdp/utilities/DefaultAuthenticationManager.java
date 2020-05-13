package ch.epfl.sdp.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.lang.reflect.Field;
import java.util.Map;

import ch.epfl.sdp.CoronaGame;
import ch.epfl.sdp.IntroActivity;
import ch.epfl.sdp.testActivities.Authentication;

public interface DefaultAuthenticationManager {

    /* TODO : REMOVE THIS METHOD AS SOON AS WE DO A GETCONTEXT OR THAT IT IS NOT USED ANYMORE
    This method was found on the internet for getting the current activity
    */
    default Activity getActivity() {
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

    default Account getAccount(Context context) {
        if (context == null){     //for tests
            return getNonNullAccount(null);
        }
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(context);
        return getNonNullAccount(acct);
    }

    default String getUserId() { return getAccount(CoronaGame.getContext()).getId(); }

    default GoogleSignInClient getGoogleClient(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        return GoogleSignIn.getClient(activity, gso);
    }

    default Account getNonNullAccount(GoogleSignInAccount acct) {
        if (acct == null) {
            User u = new User();//generic test user
            return new AccountFactory(u);
        } else {
            return new AccountFactory(acct);
        }
    }

    default void signOut(Activity activity) {
        if (getAccount(activity).isGoogle()) {
            getGoogleClient(activity).signOut()
                    .addOnCompleteListener(activity, task -> {
                        Toast.makeText(activity, "Signed out successfully!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(activity, IntroActivity.class);// New activity
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
