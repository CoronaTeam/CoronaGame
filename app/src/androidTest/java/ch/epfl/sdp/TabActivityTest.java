package ch.epfl.sdp;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class TabActivityTest {


    @Rule
    public final ActivityTestRule<TabActivity> mActivityRule =
            new ActivityTestRule<>(TabActivity.class);

    @Test
    public void testTabsInterfaceCorrectly() {
        onView(withId(R.id.tabs)).check(matches(isDisplayed()));
    }

    @Test
    public void testTabsDisplayCorrectly() {
        onView(withId(R.id.map_fragment)).check(matches(isDisplayed()));
        onView(withId(R.id.history_fragment)).check(matches(isDisplayed()));
    }

}