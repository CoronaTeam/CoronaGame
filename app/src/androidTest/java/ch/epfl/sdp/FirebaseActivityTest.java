package ch.epfl.sdp;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class FirebaseActivityTest {


    @Rule
    public GrantPermissionRule internetPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.INTERNET);

    @Rule
    public final ActivityTestRule<FirebaseActivity> mActivityRule =
            new ActivityTestRule<>(FirebaseActivity.class);

    @Test
    public void testDetectNoInternetConnection() {
    }

    @Test
    public void testDataDownloadIsDisplayed() {

    }

    @Test
    public void testHandleDataDownloadWithNoInternetConnection() {

    }

    @Test
    public void testDataIsCorrectlyUploaded() {

    }

    @Test
    public void testDataUploadIsDisplayed() {
        onView(withId(R.id.FirebaseUploadButton)).perform(click());
        onView(withId(R.id.FirebaseUploadConfirmation)).check(matches(withText("DocumentSnapshot successfully added")));
    }
}
