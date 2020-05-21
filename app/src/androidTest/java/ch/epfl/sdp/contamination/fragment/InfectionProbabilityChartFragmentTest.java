package ch.epfl.sdp.contamination.fragment;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.R;
import ch.epfl.sdp.testActivities.InfectionProbabilityChartFragmentActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.sleep;

public class InfectionProbabilityChartFragmentTest {

    @Rule
    public final ActivityTestRule<InfectionProbabilityChartFragmentActivity> activityRule =
            new ActivityTestRule<>(InfectionProbabilityChartFragmentActivity.class);
    private InfectionProbabilityChartFragment fragment;

    @Before
    public void setUp() {
        initSafeTest(activityRule, true);
        sleep(1001);
        fragment =
                ((InfectionProbabilityChartFragment) activityRule.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        sleep(1000);
    }

    @Test
    public void infectionChartIsDisplayed() {
        onView(withId(R.id.infectionProbabilityChart)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }
}