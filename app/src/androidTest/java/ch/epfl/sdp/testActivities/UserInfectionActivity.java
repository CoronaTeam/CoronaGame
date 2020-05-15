package ch.epfl.sdp.testActivities;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.contamination.fragment.UserInfectionFragment;

public class UserInfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new UserInfectionFragment();
    }
}
