package ch.epfl.sdp;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class TabActivityTest {


    @Rule
    public final ActivityTestRule<TabActivity> mActivityRule =
            new ActivityTestRule<>(TabActivity.class);

    @Test
    @Ignore("Travis won't run map")
    public void testTabsInterfaceCorrectly() {
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
    }

    @Test
    @Ignore("Travis won't run map")
    public void testTabsDisplayCorrectly() {
        onView(withId(R.id.mapView)).check(matches(isDisplayed()));
    }

    @Test
    @Ignore("Travis won't run map")
    public void testTabsMoveCorrectly() {
        onView(withId(R.id.history_tracker)).check(matches(not(hasFocus())));
        onView(withText(mActivityRule.getActivity().getString(R.string.tab_history))).perform(click());
        onView(withId(R.id.history_tracker)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
    }

}