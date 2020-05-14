package ch.epfl.sdp.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.AccountFactory;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.R;
import ch.epfl.sdp.User;
import ch.epfl.sdp.location.LocationService;

public class AccountFragment extends Fragment implements View.OnClickListener, MenuItem.OnMenuItemClickListener {

    private TextView name;
    private TextView email;
    private TextView userIdView;
    //    TextView playerIdView;
    private ImageView img;

    private ServiceConnection serviceConnection;
    private CompletableFuture<LocationService> locationService = new CompletableFuture<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_account, container, false);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); //fixes a bug on travis about inflating ImageView
        name = view.findViewById(R.id.name);
        email = view.findViewById(R.id.email);
        userIdView = view.findViewById(R.id.userIdView);
        img = view.findViewById(R.id.profileImage);
        img.setImageResource(R.drawable.ic_person);
        getAndShowAccountInfo(AuthenticationManager.getAccount(getActivity()));

        view.findViewById(R.id.moreButton).setOnClickListener(this);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                locationService.complete(((LocationService.LocationBinder) service).getService());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                locationService = null;
            }
        };

        getActivity().bindService(new Intent(getActivity(), LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        return view;
    }

    public static Account getAccount(Activity activity) {
        GoogleSignInAccount acct;
        try{
            acct = GoogleSignIn.getLastSignedInAccount(activity);
        }catch (NullPointerException e){
            acct = null;
        }

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
            case R.id.moreButton:
                showMoreMenu(v);
                break;
            // other buttons...
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(getActivity(), anchor);
        popup.getMenuInflater().inflate(R.menu.more_menu, popup.getMenu());
        popup.show();
        popup.getMenu().findItem(R.id.button_sign_out).setOnMenuItemClickListener(this);
        popup.getMenu().findItem(R.id.button_delete_local_history).setOnMenuItemClickListener(this);
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
            email.setText(personEmail);
            userIdView.setText(getString(R.string.user_id, personId));
//            playerIdView.setText(playerId);

            if (personPhoto != null) {
                Glide.with(this).load(String.valueOf(personPhoto)).into(img);
            }

        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.button_sign_out: {
                AuthenticationManager.signOut(getActivity());
                return true;
            }
            case R.id.button_delete_local_history: {
                locationService.join().getAnalyst().getCarrier().deleteLocalProbabilityHistory();
            }
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (serviceConnection != null) {
            getActivity().unbindService(serviceConnection);
            serviceConnection = null;
        }
    }
}
