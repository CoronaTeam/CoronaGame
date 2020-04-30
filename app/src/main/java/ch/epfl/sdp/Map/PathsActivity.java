package ch.epfl.sdp.Map;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.SingleFragmentActivity;

// to be deleted
public class PathsActivity extends SingleFragmentActivity {
    public PathsHandler pathsHandler; // for testing

    @Override
    protected Fragment createFragment() {
        //pathsHandler = new PathsHandler();
        return pathsHandler;
    }
}
