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

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * Class AccounteGettingactivity
 * @author lucas
 */
public class AccountGettingActivity extends AppCompatActivity {
    Button signOut;
    GoogleSignInClient mGoogleSignInClient;
    TextView name;
    TextView email;
    TextView lastName;
    ImageView img;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_getting);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        lastName = findViewById(R.id.lastName);
        img = findViewById(R.id.imageView);

        signOut = findViewById(R.id.button_sign_out);
        signOut.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.button_sign_out:
                        signOut();
                        break;
                }
            }
        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        getAndShowAccountInfo(GoogleSignIn.getLastSignedInAccount(this));

    }

    private void getAndShowAccountInfo(GoogleSignInAccount acct) {
        if (acct != null) {
            String personName = acct.getDisplayName();
            //  String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            //  String personId = acct.getId(); // Use this in order to uniquely identify people
            Uri personPhoto = acct.getPhotoUrl();
            name.setText(personName);
            lastName.setText(personFamilyName);
            email.setText(personEmail);
            Glide.with(this).load(String.valueOf(personPhoto)).into(img);
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
