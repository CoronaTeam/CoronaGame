package ch.epfl.sdp.testActivities;

import android.os.Handler;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.contamination.fragment.InfectionFragment;

public class InfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        Handler uiHandler = new Handler();
        return new InfectionFragment(uiHandler);
    }
}