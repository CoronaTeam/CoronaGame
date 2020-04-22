package ch.epfl.sdp.contamination;

import android.os.Handler;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.SingleFragmentActivity;

public class InfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        Handler uiHandler = new Handler();
        return new InfectionFragment(uiHandler);
    }
}