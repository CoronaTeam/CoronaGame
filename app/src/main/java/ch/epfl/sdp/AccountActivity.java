package ch.epfl.sdp;

import androidx.fragment.app.Fragment;
import ch.epfl.sdp.fragment.AccountFragment;

/**
 * Class AccounteGettingactivity : once logged in google, this class will be able to retrieve given user information.
 *
 * @author lucas
 */
public class AccountActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new AccountFragment();
    }

}