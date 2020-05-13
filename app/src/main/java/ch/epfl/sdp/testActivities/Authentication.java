package ch.epfl.sdp.testActivities;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.utilities.SingleFragmentActivity;
import ch.epfl.sdp.identity.fragment.AuthenticationFragment;

/**
 * AuthenticationActivity : handling the signIn process via google play. This class will check if a user has been already logged in.
 * If not, it displays the sign In button and if this latter is pressed, a window built by google is shown.
 * Then, it launches and displays the main app UI.
 *
 * @author lucas
 */
public class Authentication extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new AuthenticationFragment();
    }
}