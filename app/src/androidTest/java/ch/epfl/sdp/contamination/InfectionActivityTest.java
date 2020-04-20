package ch.epfl.sdp.contamination;

import androidx.test.rule.ActivityTestRule;
import ch.epfl.sdp.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static ch.epfl.sdp.TestTools.getActivity;
import static junit.framework.TestCase.assertNotNull;

public class InfectionActivityTest {

    private InfectionFragment fragment;

    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);

    @Before
    public void setup() {
        fragment = ((InfectionFragment)((InfectionActivity)(getActivity())).getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
    }

    @Test
    public void receiverIsInstantiated(){
        assertNotNull(fragment.getLocationService().getReceiver());
    }
    @Test
    public void analystIsInstantiated(){
        assertNotNull(fragment.getLocationService().getAnalyst());
    }
}
