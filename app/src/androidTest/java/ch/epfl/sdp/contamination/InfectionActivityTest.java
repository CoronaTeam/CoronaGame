package ch.epfl.sdp.contamination;

import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;

public class InfectionActivityTest {

    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);

    @Test
    public void receiverIsInstantiated(){
        assertNotNull(InfectionActivity.getReceiver());
    }
    @Test
    public void analystIsInstantiated(){
        assertNotNull(InfectionActivity.getAnalyst());
    }
}
