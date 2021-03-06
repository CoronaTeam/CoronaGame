package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.databaseIO.DataReceiver;
import ch.epfl.sdp.contamination.fragment.InfectionFragment;
import ch.epfl.sdp.location.LocationService;
import ch.epfl.sdp.testActivities.InfectionActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestTools.clickBack;
import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.sleep;
import static junit.framework.TestCase.assertNotNull;

public class InfectionActivityTest {

    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);
    private InfectionFragment fragment;

    @Before
    public void setup() {
        fragment = ((InfectionFragment) ((InfectionActivity) (getActivity())).getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        initSafeTest(mActivityRule, true);
    }

    @After
    public void release() {
//        Intents.release();
    }

    @Test
    public void receiverIsInstantiated() {
        assertNotNull(fragment.getLocationService().join().getReceiver());
    }

    @Test
    public void analystIsInstantiated() {
        assertNotNull(fragment.getLocationService().join().getAnalyst());
    }

    private void displayHelper() {
        InfectionAnalyst analyst = new FakeAnalyst();

        LocationService service = fragment.getLocationService().join();

        service.setAnalyst(analyst);
        service.setSender(new FakeCachingDataSender());
        service.setReceiver(new DataReceiver() {
            @Override
            public CompletableFuture<Set<Carrier>> getUserNearby(Location location, Date date) {
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Map<Carrier, Integer>> getUserNearbyDuring(Location location, Date startDate, Date endDate) {
                return CompletableFuture.completedFuture(null);

            }

            @Override
            public CompletableFuture<Location> getMyLastLocation(String accountId) {
                return CompletableFuture.completedFuture(null);

            }

            @Override
            public CompletableFuture<Map<String, Object>> getNumberOfSickNeighbors(String userId) {
                return CompletableFuture.completedFuture(null);

            }

            @Override
            public CompletableFuture<Map<String, Object>> getRecoveryCounter(String userId) {
                return CompletableFuture.completedFuture(null);

            }
        });

        fragment.onModelRefresh(null);
        sleep(10);
        onView(withId(R.id.my_infection_refresh)).perform(click());
        clickBack();

    }

    @Test
    public void displaysWhenNoMeeting() {
        FakeAnalyst.infectMeets = 0;
        displayHelper();
        //Why we check it has HEALTHY : the press-back would have quit the app if the display would have not be shown
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
    }

    @Test
    public void displaysDialogWhen1Meeting() {
        FakeAnalyst.infectMeets = 1;
        displayHelper();
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
    }

    @Test
    public void displayDialogWhenSeveralMeetings() {
        FakeAnalyst.infectMeets = 42;
        displayHelper();
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
    }
}
