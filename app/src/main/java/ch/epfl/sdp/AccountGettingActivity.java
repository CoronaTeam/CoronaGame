package ch.epfl.sdp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Class AccounteGettingactivity : once logged in google, this class will be able to retrieve given user information.
 * @author lucas
 */
public class AccountGettingActivity extends AppCompatActivity {
    Button signOut;
    GoogleSignInClient mGoogleSignInClient;
    TextView name;
    TextView email;
    TextView lastName;
    TextView userIdView;
    //TextView playerIdView;
//    ImageView img;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_getting);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); //fixes a bug on travis about inflating ImageView
//        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        lastName = findViewById(R.id.lastName);
        userIdView = findViewById(R.id.userIdView);
//        img = findViewById(R.id.profileImage);
        //playerIdView = findViewById(R.id.playerIdView);

//        signOut = findViewById(R.id.button_sign_out);
//        signOut.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                switch (v.getId()) {
//                    case R.id.button_sign_out:
//                        signOut();
//                        break;
//                }
//            }
//        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        showAccountOrCreateAGenericOneForTesting(acct);
    }

    /*
    Thank's to this method, the method onCreate is less than 25 lines long ! Thank you CodeClimate :)
    PS: this method will only be accessed with a null account if we are in test mode.
    @requires : test mode OR account non-null.
     */
    private void showAccountOrCreateAGenericOneForTesting(GoogleSignInAccount acct) {
        if(acct == null){
            User u = new User();//generic test user
            getAndShowAccountInfo(new AccountFactory(u));
        }else{
            getAndShowAccountInfo(new AccountFactory(acct));
        }
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

//            name.setText(personName);
            lastName.setText(personFamilyName);
            email.setText(personEmail);
            userIdView.setText(personId);
            //playerIdView.setText(playerId);
 //           Glide.with(this).load(String.valueOf(personPhoto)).into(img);

        }
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(AccountGettingActivity.this,"Signed out successfully!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(AccountGettingActivity.this, AuthenticationActivity.class);// New activity
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //clears this activity's stack
                        startActivity(intent);
                        finish();
                    }
                });

    }
}