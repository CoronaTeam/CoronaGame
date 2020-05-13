package ch.epfl.sdp.map;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.utilities.SingleFragmentActivity;

public class MapActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new MapFragment();
    }
}
