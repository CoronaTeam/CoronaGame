package ch.epfl.sdp;

import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;


public class UserInfectionTest {

    @Rule
    public final ActivityTestRule<UserInfectionActivity> activityRule =
            new ActivityTestRule<>(UserInfectionActivity.class);


    @Test
    public void testDataUpload() {
        TestTools.clickAndCheck(R.id.infectionStatusButton,
                R.id.infectionStatusUploadConfirmation);
    }
}
