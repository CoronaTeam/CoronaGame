package ch.epfl.sdp.utilities;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.junit.Before;
import org.junit.Test;

import ch.epfl.sdp.utilities.AccountFactory;
import ch.epfl.sdp.utilities.User;

import static org.junit.Assert.assertEquals;

public class AccountFactoryTest {
    AccountFactory userFactory;

    //    AccountFactory googleFactory;  NOT TESTABLE (sorry)
    @Before
    public void setup() {
        userFactory = new AccountFactory(new User());
        //  googleFactory = new AccountFactory() NOT TESTABLE
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfUserIsNullForCreation() {
        User nullUser = null;
        new AccountFactory(nullUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfgoogleAccountIsNullForCreation() {
        GoogleSignInAccount nullUser = null;
        new AccountFactory(nullUser);
    }

    @Test
    public void testGetDisplayName() {
        assertEquals(User.DEFAULT_DISPLAY_NAME, userFactory.getDisplayName());
    }

    @Test
    public void testGetFamilyName() {
        assertEquals(User.DEFAULT_FAMILY_NAME, userFactory.getFamilyName());

    }

    @Test
    public void testGetEmail() {
        assertEquals(User.DEFAULT_EMAIL, userFactory.getEmail());

    }

    @Test
    public void testGetPhotoUrl() {
        assertEquals(User.DEFAULT_URI, userFactory.getPhotoUrl());

    }

    @Test
    public void testIsGoogle() {
        assertEquals(false, userFactory.isGoogle());

    }

    @Test
    public void testGetPlayerId() {
        assertEquals(User.DEFAULT_PLAYERID, userFactory.getPlayerId(null));

    }

    @Test
    public void testGetAccount() {
        assertEquals(null, userFactory.getAccount());

    }

    @Test
    public void testGetId() {
        assertEquals(User.DEFAULT_USERID, userFactory.getId());
    }
}
