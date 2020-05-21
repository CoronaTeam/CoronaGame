package ch.epfl.sdp.testActivities;

import androidx.fragment.app.Fragment;

import ch.epfl.sdp.contamination.fragment.InfectionProbabilityChartFragment;
import ch.epfl.sdp.contamination.fragment.UserInfectionFragment;
import ch.epfl.sdp.utilities.SingleFragmentActivity;

public class InfectionProbabilityChartFragmentActivity  extends SingleFragmentActivity {
    protected Fragment createFragment() {
        return new InfectionProbabilityChartFragment();
    }
}