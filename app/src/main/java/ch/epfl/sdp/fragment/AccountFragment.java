package ch.epfl.sdp.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.Account;
import ch.epfl.sdp.AccountFactory;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.R;
import ch.epfl.sdp.User;

public class AccountFragment extends Fragment implements View.OnClickListener {

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
        Button signOutButton = view.findViewById(R.id.button_sign_out);
        signOutButton.setOnClickListener(this);
        getAndShowAccountInfo(AuthenticationManager.getAccount(getActivity()));

        return view;
    }

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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sign_out:
                signOut(v);
                break;
            // other buttons...
        }
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
        AuthenticationManager.signOut(getActivity());
    }

}
