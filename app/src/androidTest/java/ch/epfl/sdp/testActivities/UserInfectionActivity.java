package ch.epfl.sdp.testActivities;

import androidx.fragment.app.Fragment;

public class UserInfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new UserInfectionFragment();
    }
}
