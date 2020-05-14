package ch.epfl.sdp.identity;

//import Account;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class AccountAdapter implements Account {
    private GoogleSignInAccount googleSignInAccount = null;
    private User user = null;
    public AccountAdapter(GoogleSignInAccount googleAccount){
        if(googleAccount != null){
            this.googleSignInAccount = googleAccount;
        }else{
            throw new IllegalArgumentException("ERR: googleAccount or User should not be null");
        }
    }
    public AccountAdapter(User u){
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
    public Boolean isGoogle() {
        return googleSignInAccount !=null;
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
