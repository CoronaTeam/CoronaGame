package ch.epfl.sdp.identity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.junit.Before;
import org.junit.Test;

import ch.epfl.sdp.identity.fragment.AccountFragment;

import static org.junit.Assert.assertEquals;

public class AccountAdapterTest {
    private AccountAdapter userAdapter;

    //    AccountFactory googleFactory;  NOT TESTABLE (sorry)
    @Before
    public void setup() {
        userAdapter = new AccountAdapter(new User());
        AccountFragment.IN_TEST = true;
        //  googleFactory = new AccountFactory() NOT TESTABLE
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfUserIsNullForCreation() {
        User nullUser = null;
        new AccountAdapter(nullUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfGoogleAccountIsNullForCreation() {
        GoogleSignInAccount nullUser = null;
        new AccountAdapter(nullUser);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals(User.DEFAULT_DISPLAY_NAME, userAdapter.getDisplayName());
    }

    @Test
    public void testGetFamilyName() {
        assertEquals(User.DEFAULT_FAMILY_NAME, userAdapter.getFamilyName());

    }

    @Test
    public void testGetEmail() {
        assertEquals(User.DEFAULT_EMAIL, userAdapter.getEmail());

    }

    @Test
    public void testGetPhotoUrl() {
        assertEquals(User.DEFAULT_URI, userAdapter.getPhotoUrl());

    }

    @Test
    public void testIsGoogle() {
        assertEquals(false, userAdapter.isGoogle());

    }


    @Test
    public void testGetId() {
        assertEquals(User.DEFAULT_USERID, userAdapter.getId());
    }
}
