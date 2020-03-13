package ch.epfl.sdp;

//import Account;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.PlayersClient;

public class AccountFactory implements Account {
    private GoogleSignInAccount googleSignInAccount = null;
    private User user = null;
    public AccountFactory(GoogleSignInAccount googleAccount){
        if(googleAccount != null){
            this.googleSignInAccount = googleAccount;
        }else{
            throw new IllegalArgumentException("ERR: googleAccount or User should not be null");
        }
    }
    public AccountFactory(User u){
        if(u != null){
            this.user = u;
        }else {
            throw new IllegalArgumentException("ERR: googleAccount or User should not be null");
        }
    }

    @Override
    public String getDisplayName() {
        if(user != null){
            return user.getDisplayName();
        }else {
            return googleSignInAccount.getDisplayName();
        }
    }

    @Override
    public String getFamilyName() {
        if(user != null){
            return user.getFamilyName();
        }else{
            return googleSignInAccount.getFamilyName();
        }
    }

    @Override
    public String getEmail() {
        if(user != null){
            return user.getEmail();
        }else{
            return googleSignInAccount.getEmail();
        }
    }

    @Override
    public Uri getPhotoUrl() {
        if(user != null){
            return user.getPhotoUrl();
        }else{
            return googleSignInAccount.getPhotoUrl();
        }
    }

    @Override
    public String getPlayerId(Activity activity) {
        if(user != null){
            return user.getPlayerId(activity);
        }else{
            PlayersClient pc = Games.getPlayersClient(activity, googleSignInAccount);
            return String.valueOf(pc.getCurrentPlayerId());
        }
    }

    @Override
    public GoogleSignInAccount getAccount() {
        return googleSignInAccount;
    }

    @Override
    public String getId() {
        if(googleSignInAccount!=null){
            return googleSignInAccount.getId();
        }else{
            return user.getId();
        }
    }
}
