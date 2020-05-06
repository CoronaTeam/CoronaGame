package ch.epfl.sdp.Map;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.SingleFragmentActivity;

public class MapActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new MapFragment();
    }
}
