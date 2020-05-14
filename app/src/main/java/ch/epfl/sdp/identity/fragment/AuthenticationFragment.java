package ch.epfl.sdp.identity.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.R;
import ch.epfl.sdp.tabActivity.TabActivity;

public class AuthenticationFragment extends Fragment {

    public static final int RC_SIGN_IN = 0; //any number, but common for the app
    GoogleSignInClient googleSignInClient;
    View signIn;// error prone line if View is replaced by Button

    private static Class NEXT_ACTIVITY = TabActivity.class;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.activity_authentication, container, false);

        signIn = view.findViewById(R.id.sign_in_button);
        signIn.setOnClickListener(v -> {
            switch (v.getId()) {
                case R.id.sign_in_button:
                    signIn();
                    break;
                // ...
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
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
            signInComplete(getActivity());
        }
    }

    public static void signInComplete(Activity activity) {
        Intent intent = new Intent(activity, NEXT_ACTIVITY);// New activity
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //clears this activity's stack
        activity.startActivity(intent);
        activity.finish(); // Launches next Activity
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
                Toast.makeText(getActivity(), "Please check your internet connection", Toast.LENGTH_LONG).show();
            }

            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("authenticationActivity", "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }
}
