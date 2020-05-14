package ch.epfl.sdp.identity;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;

import ch.epfl.sdp.identity.User;
import ch.epfl.sdp.identity.fragment.AccountFragment;

import static org.junit.Assert.assertEquals;

public class UserTest {
    User u;

    @Before
    public void setup() {
        u = new User("INFINITE", "LOOP", "TEST@USELESS",
                Uri.parse("https://www.meme-arsenal.com/memes/783e18d7c7722bedc71b80ec5986b648.jpg"),
                 "9827545", User.DEFAULT_AGE, false);
    }

    @Test
    public void mailIsOk() {
        assertEquals("TEST@USELESS", u.getEmail());
    }

    @Test
    public void displayNameIsOk() {
        assertEquals("INFINITE", u.getDisplayName());
    }

    @Test
    public void familyNameIsOk() {
        assertEquals("LOOP", u.getFamilyName());
    }

    @Test
    public void uriIsOk() {
        assertEquals(Uri.parse("https://www.meme-arsenal.com/memes/783e18d7c7722bedc71b80ec5986b648.jpg"), u.getPhotoUrl());
    }

    @Test
    public void isGoogleReturnsFalse() {
        assertEquals(false, u.isGoogle());
    }

    @Test
    public void userIdIsOk() {
        assertEquals("9827545", u.getId());
    }

}
