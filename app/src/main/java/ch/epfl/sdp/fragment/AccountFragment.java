package ch.epfl.sdp.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.Account;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.R;

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
