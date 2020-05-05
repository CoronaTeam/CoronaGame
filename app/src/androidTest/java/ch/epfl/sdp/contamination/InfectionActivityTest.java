package ch.epfl.sdp.contamination;

import android.content.Intent;
import android.location.Location;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.ActivityTestRule;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.R;
import ch.epfl.sdp.location.LocationService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.CoronaGame.getContext;
import static ch.epfl.sdp.TestTools.clickBack;
import static ch.epfl.sdp.TestTools.getActivity;
import static ch.epfl.sdp.TestTools.initSafeTest;
import static ch.epfl.sdp.TestTools.sleep;
import static junit.framework.TestCase.assertNotNull;
//@Ignore("Lucas please fix: UserInfectionFragment not attached to a context")
public class InfectionActivityTest {

    private InfectionFragment fragment;

    @Rule
    public final ActivityTestRule<InfectionActivity> mActivityRule = new ActivityTestRule<>(InfectionActivity.class);

    @Before
    public void setup() {
        fragment = ((InfectionFragment)((InfectionActivity)(getActivity())).getSupportFragmentManager().findFragmentById(R.id.fragmentContainer));
        initSafeTest(mActivityRule,true);
    }
    @After
    public void release(){
        Intents.release();
//        fragment.getActivity().stopService(new Intent(getContext(), LocationService.class));

    }

    @Test
    public void receiverIsInstantiated(){
        assertNotNull(fragment.getLocationService().getReceiver());
    }
    @Test
    public void analystIsInstantiated(){
        assertNotNull(fragment.getLocationService().getAnalyst());
    }
    private void displayHelper(){
        InfectionAnalyst analyst = new FakeAnalyst();
        fragment.getLocationService().setAnalyst(analyst);
        fragment.getLocationService().setSender(new FakeCachingDataSender());
        fragment.getLocationService().setReceiver(new DataReceiver() {
            @Override
            public void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback) {
                callback.onCallback(null);
            }

            @Override
            public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {
                callback.onCallback(null);
            }

            @Override
            public void getMyLastLocation(Account account, Callback<Location> callback) {
                callback.onCallback(null);
            }

            @Override
            public void getNumberOfSickNeighbors(String userId, Callback<Map<String, Float>> callback) {
                callback.onCallback(null);
            }

            @Override
            public void getRecoveryCounter(String userId, Callback<Map<String, Integer>> callback) {
                callback.onCallback(null);
            }
        });
        int now = (int)System.currentTimeMillis();
        fragment.onModelRefresh(null);
        sleep(10);
        onView(withId(R.id.my_infection_refresh)).perform(click());
        clickBack();

    }
    @Test
    public void displaysWhenNoMeeting(){
        FakeAnalyst.infectMeets = 0 ;
        displayHelper();
        //Why we check it has HEALTHY : the press-back would have quit the app if the display would have not be shown
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
    }
    @Test
    public void displaysDialogWhen1Meeting(){
        FakeAnalyst.infectMeets = 1;
        displayHelper();
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
    }
    @Test
    public void displayDialogWhenSeveralMeetings(){
        FakeAnalyst.infectMeets=42;
        displayHelper();
        onView(withId(R.id.my_infection_status)).check(matches(withText("HEALTHY")));
    }
}
