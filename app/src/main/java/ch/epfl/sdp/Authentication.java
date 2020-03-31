package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;


/**
 * AuthenticationActivity : handling the signIn process via google play. This class will check if a user has been already logged in.
 * If not, it displays the sign In button and if this latter is pressed, a window built by google is shown.
 * Then, it launches and displays the main app UI.
 *
 * @author lucas
 */
public class Authentication extends AppCompatActivity {
    public static final int RC_SIGN_IN = 0; //any number, but common for the app
    GoogleSignInClient googleSignInClient;
    View signIn;// error prone line if View is replaced by Button

    public static String APP_PREFERENCES = "APP_PREFERENCES";
    public static String OPENED_BEFORE_PREFERENCE = "OPENED_BEFORE";

    private static Class NEXT_ACTIVITY = TabActivity.class;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkFirstTimeOpen();

        setContentView(R.layout.activity_authentication);
        signIn = findViewById(R.id.sign_in_button);


        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.sign_in_button:
                        signIn();
                        break;
                    // ...
                }
            }

        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
// the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(account);
    }

    private void updateUI(GoogleSignInAccount account) {
        if (account == null) {
            //display the Google Sign-in button -> not yet registered
            signIn.setVisibility(View.VISIBLE);
        } else {
            // hide the sign-in button, launch your main activity -> already registered
            signIn.setVisibility(View.INVISIBLE);
//            startActivity(new Intent(this, AccountGettingActivity.class));
            Intent intent = new Intent(Authentication.this, NEXT_ACTIVITY);// New activity
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //clears this activity's stack
            startActivity(intent);
            finish(); // Launches next Activity
        }
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...) ;
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        } else {
            throw new IllegalStateException("Request code is not = RC_SIGN_IN");
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e) {
            if (e.getStatusCode() == 7) {
                Toast.makeText(Authentication.this, "Please check your internet connection", Toast.LENGTH_LONG).show();
            }

            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("authenticationActivity", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    /** Launch IntroActivity if it is the first time the user opens the app */
    private void checkFirstTimeOpen() {
        SharedPreferences sp = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        if (!sp.getBoolean(OPENED_BEFORE_PREFERENCE, false)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(OPENED_BEFORE_PREFERENCE, true);
            editor.apply();
            setIntroView();
        }
    }

    /** Called when the user opens the app for the first time */
    private void setIntroView() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }
}