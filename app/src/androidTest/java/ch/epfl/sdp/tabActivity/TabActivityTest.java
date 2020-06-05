package ch.epfl.sdp.tabActivity;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

public class TabActivityTest {

    @Rule
    public final ActivityTestRule<TabActivity> activityRule = new ActivityTestRule<>(TabActivity.class);
    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() {
    }

    @After
    public void clean() {
    }

    @Test
    public void testTabsInterfaceCorrectly() {
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabsMoveCorrectly() {
        onView(withId(R.id.infectionStatusView)).check(matches(not(hasFocus())));
        onView(withText(activityRule.getActivity().getString(R.string.tab_status))).perform(click());
        onView(withId(R.id.infectionStatusView)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
        onView(withText(activityRule.getActivity().getString(R.string.tab_account))).perform(click());
        onView(withId(R.id.accountDetails)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

}