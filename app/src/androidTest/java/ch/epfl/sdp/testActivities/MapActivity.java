package ch.epfl.sdp.testActivities;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.map.fragment.MapFragment;

public class MapActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new MapFragment();
    }
}
