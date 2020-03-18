package ch.epfl.sdp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class UserInfectionTest {
    @Rule
    public ActivityScenarioRule<UserInfectionActivity> rule = new ActivityScenarioRule<>(UserInfectionActivity.class);

    private ActivityScenario<UserInfectionActivity> scenario;

    @Before
    public void setUp() {
        scenario = rule.getScenario();
    }

    @Test
    public void changeViewContentWhenClick() {
        // click for the first time changes view from default to infected status
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am cured", "Your user status is set to infected.", 5000);
        // click again changes view from infected status to cured status
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am infected", "Your user status is set to not infected.", 5000);
    }

    @Test
    public void keepLastInfectionStatusWhenRestartingApp() {
        ActivityScenario<UserInfectionActivity> launchedActivity = scenario.launch(UserInfectionActivity.class);
        clickWaitAndCheckTexts(R.id.infectionStatusButton, R.id.infectionStatusView, "I am cured", "Your user status is set to infected.", 5000);
        launchedActivity.recreate();
        onView(withId(R.id.infectionStatusView)).check(matches(withText("Your user status is set to infected.")));
        onView(withId(R.id.infectionStatusButton)).check(matches(withText("I am cured")));

    }


    private void clickWaitAndCheckTexts(int buttonID, int textID, String expectedButtonText, String expectedText, int waitingTime) {
        onView(withId(buttonID)).perform(click());
        waitingForTravis(waitingTime);
        onView(withId(textID)).check(matches(withText(expectedText)));
        onView(withId(buttonID)).check(matches(withText(expectedButtonText)));
    }

    private void waitingForTravis(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
