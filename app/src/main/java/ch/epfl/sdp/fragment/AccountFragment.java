package ch.epfl.sdp.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.Account;
import ch.epfl.sdp.AccountFactory;
import ch.epfl.sdp.AccountGetting;
import ch.epfl.sdp.Authentication;
import ch.epfl.sdp.R;
import ch.epfl.sdp.User;

public class AccountFragment extends Fragment {

    private GoogleSignInClient mGoogleSignInClient;
    private Account accountInUse;
    private TextView name;
    private TextView email;
    private TextView lastName;
    private TextView userIdView;
    //    TextView playerIdView;
    private ImageView img;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_account, container, false);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); //fixes a bug on travis about inflating ImageView
        name = view.findViewById(R.id.name);
        email = view.findViewById(R.id.email);
        lastName = view.findViewById(R.id.lastName);
        userIdView = view.findViewById(R.id.userIdView);
        img = view.findViewById(R.id.profileImage);
        mGoogleSignInClient = AccountGetting.getGoogleClient(getActivity());
        accountInUse = AccountGetting.getAccount(getActivity());
        getAndShowAccountInfo(accountInUse);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(getActivity(), "Signed out successfully!", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(getActivity(), Authentication.class);// New activity
//                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //clears this activity's stack
                            startActivity(intent);
                            getActivity().finish();
                        }
                    });
        } else {
            //no need to sign out from google, just go to the other activity
            // do not toast during test !
            Intent intent = new Intent(getActivity(), Authentication.class);// New activity
            startActivity(intent);
            getActivity().finish();
        }

    }

}
