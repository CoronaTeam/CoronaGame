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

public class AuthenticationManager {

    public static Account getAccount(Activity activity) {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(activity);
        return getNonNullAccount(acct);
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