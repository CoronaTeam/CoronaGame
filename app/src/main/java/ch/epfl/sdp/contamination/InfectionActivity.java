package ch.epfl.sdp.contamination;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.SingleFragmentActivity;

public class InfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new InfectionFragment();
    }
}