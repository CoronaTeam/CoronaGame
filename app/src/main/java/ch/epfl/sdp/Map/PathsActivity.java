package ch.epfl.sdp.Map;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.SingleFragmentActivity;
import ch.epfl.sdp.fragment.PathsFragment;

public class PathsActivity extends SingleFragmentActivity {
    public PathsFragment pathsFragment; // for testing

    @Override
    protected Fragment createFragment() {
        pathsFragment = new PathsFragment();
        return pathsFragment;
    }
}
