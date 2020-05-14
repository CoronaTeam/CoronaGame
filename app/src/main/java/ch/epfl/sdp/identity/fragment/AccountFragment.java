package ch.epfl.sdp.identity.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import ch.epfl.sdp.identity.Account;
import ch.epfl.sdp.identity.AccountAdapter;
import ch.epfl.sdp.identity.AuthenticationManager;
import ch.epfl.sdp.R;
import ch.epfl.sdp.identity.User;

public class AccountFragment extends Fragment implements View.OnClickListener, MenuItem.OnMenuItemClickListener {
    public static boolean IN_TEST = false;
    private TextView name;
    private TextView email;
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
        userIdView = view.findViewById(R.id.userIdView);
        img = view.findViewById(R.id.profileImage);
        img.setImageResource(R.drawable.ic_person);
        getAndShowAccountInfo(AuthenticationManager.getAccount(getActivity()));

        view.findViewById(R.id.moreButton).setOnClickListener(this);

        return view;
    }

    public static Account getAccount(Activity activity) {
        if( ! IN_TEST){
            return new AccountAdapter(GoogleSignIn.getLastSignedInAccount(activity));
        }else{
            return new AccountAdapter(new User());
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

            name.setText(personName);
            email.setText(personEmail);
            userIdView.setText(getString(R.string.user_id, personId));
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
        }
        return false;
    }
}
