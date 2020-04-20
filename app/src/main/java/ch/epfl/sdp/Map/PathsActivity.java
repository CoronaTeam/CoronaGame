package ch.epfl.sdp.Map;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.SingleFragmentActivity;
import ch.epfl.sdp.fragment.PathsFragment;

public class PathsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new PathsFragment();
    }
}
