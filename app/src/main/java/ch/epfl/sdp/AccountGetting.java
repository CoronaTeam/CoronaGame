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

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Class AccounteGettingactivity : once logged in google, this class will be able to retrieve given user information.
 *
 * @author lucas
 */
public class AccountGetting extends AppCompatActivity {
    GoogleSignInClient mGoogleSignInClient;
    Account accountInUse;
    TextView name;
    TextView email;
    TextView lastName;
    TextView userIdView;
    //    TextView playerIdView;
    ImageView img;

    public static Account getAccount(Activity activity) {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(activity);
        return getNonNullAccount(acct);
    }

    private static GoogleSignInClient getGoogleClient(Activity activity) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_getting);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); //fixes a bug on travis about inflating ImageView
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        lastName = findViewById(R.id.lastName);
        userIdView = findViewById(R.id.userIdView);
        img = findViewById(R.id.profileImage);
        mGoogleSignInClient = getGoogleClient(this);
        Account acct = getAccount(this);
        getAndShowAccountInfo(acct);
    }

    private void getAndShowAccountInfo(Account acct) {
        if (acct != null) {
            String personName = acct.getDisplayName();
            //  String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            String personId = acct.getId(); // Use this in order to uniquely identify people
            Uri personPhoto = acct.getPhotoUrl();
            //PlayersClient pc = Games.getPlayersClient(this, acct.getAccount());
            //String playerId = String.valueOf(pc.getCurrentPlayerId());
            //String playerId = acct.getPlayerId(this);

            name.setText(personName);
            lastName.setText(personFamilyName);
            email.setText(personEmail);
            userIdView.setText(personId);
//            playerIdView.setText(playerId);
            Glide.with(this).load(String.valueOf(personPhoto)).into(img);

        }
    }

    public void signOut(View v) {
        if (accountInUse.isGoogle()) {
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(AccountGetting.this, "Signed out successfully!", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(AccountGetting.this, Authentication.class);// New activity
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //clears this activity's stack
                            startActivity(intent);
                            finish();
                        }
                    });
        } else {
            //no need to sign out from google, just go to the other activity
            // do not toast during test !
            Intent intent = new Intent(AccountGetting.this, Authentication.class);// New activity
            startActivity(intent);
            finish();
        }

    }
}