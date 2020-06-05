package ch.epfl.sdp.identity.fragment;

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

import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.identity.Account;
import ch.epfl.sdp.identity.AuthenticationManager;
import ch.epfl.sdp.location.LocationService;

public class AccountFragment extends Fragment implements View.OnClickListener, MenuItem.OnMenuItemClickListener {
    public static boolean IN_TEST = false;
    private TextView name;
    private TextView email;
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
        img = view.findViewById(R.id.profileImage);
        img.setImageResource(R.drawable.ic_person);
        getAndShowAccountInfo(AuthenticationManager.getAccount(requireActivity()));

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

        requireActivity().bindService(new Intent(requireActivity(), LocationService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);

        return view;
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
        PopupMenu popup = new PopupMenu(requireActivity(), anchor);
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
            String personEmail = acct.getEmail();
            Uri personPhoto = acct.getPhotoUrl();

            name.setText(personName);
            email.setText(personEmail);
            if (personPhoto != null) {
                Glide.with(this).load(String.valueOf(personPhoto)).into(img);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.button_sign_out: {
                AuthenticationManager.signOut(requireActivity());
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
            requireActivity().unbindService(serviceConnection);
            serviceConnection = null;
        }
    }
}
