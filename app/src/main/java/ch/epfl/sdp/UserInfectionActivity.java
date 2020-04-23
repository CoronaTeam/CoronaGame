package ch.epfl.sdp;

import androidx.fragment.app.Fragment;
import ch.epfl.sdp.fragment.UserInfectionFragment;

public class UserInfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new UserInfectionFragment();
    }
}
