package ch.epfl.sdp.contamination;

import android.location.Location;
import android.os.Handler;
import android.widget.TextView;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import ch.epfl.sdp.R;
import ch.epfl.sdp.TestTools;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.TestUtils.buildLocation;
import static org.hamcrest.CoreMatchers.is;

public class RealGridSenderTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    Consumer<Void> writeSuccessToUi;
    Function<Throwable, Void> writeFailureToUi;

    @Before
    public void setupTests() {
        TextView exchangeStatus = mActivityRule.getActivity().exchangeStatus;

        // Get reference to UI handler
        Handler uiHandler = mActivityRule.getActivity().uiHandler;

        writeSuccessToUi = (a) -> uiHandler.post(() -> exchangeStatus.setText("EXCHANGE Succeeded"));
        writeFailureToUi = (a) -> {
            uiHandler.post(() -> exchangeStatus.setText("EXCHANGE Failed"));
            return null;
        };
    }


    @Test
    public void complexQueriesComeAndGoFromServer() throws Throwable {
        // The following test uses the actual Firestore

        TestTools.resetLocationServiceStatus(mActivityRule.getActivity().getService());

        Carrier aFakeCarrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.2734f);
        Carrier trulyHealthy = new Layman(Carrier.InfectionStatus.HEALTHY, 0f);

        Location somewhereInTheWorld = buildLocation(12, 73);

        Date rightNow = new Date(System.currentTimeMillis());
        Date aLittleLater = new Date(rightNow.getTime() + 10);

        mActivityRule.getActivity().getService().getSender().registerLocation(
                aFakeCarrier, somewhereInTheWorld, rightNow)
                .thenAccept(writeSuccessToUi)
                .exceptionally(writeFailureToUi);
        mActivityRule.getActivity().getService().getSender().registerLocation(
                trulyHealthy,
                somewhereInTheWorld,
                aLittleLater);

        TestTools.sleep();

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));

        getBackRangeData(somewhereInTheWorld, rightNow, aLittleLater).thenAccept(result -> {
            assertThat(result.size(), is(2));
            assertThat(result.containsKey(aFakeCarrier), is(true));
            assertThat(result.containsKey(trulyHealthy), is(true));
            assertThat(result.get(aFakeCarrier), is(1));
            assertThat(result.get(trulyHealthy), is(1));
        });



    }

    private Map<Carrier, Boolean> getBackSliceData(Location somewhere, Date rightNow) throws Throwable {
        Map<Carrier, Boolean> result = new ConcurrentHashMap<>();

        AtomicBoolean done = new AtomicBoolean();
        done.set(false);

        mActivityRule.runOnUiThread(() -> mActivityRule.getActivity().getService().getReceiver()
                .getUserNearby(somewhere, rightNow).thenAccept(people -> {
                    for (Carrier c : people) {
                        result.put(c, false);
                    }
                    done.set(true);
                }));

        while (!done.get()) {
        } // Busy wait

        return result;
    }

    @Test
    public void dataReallyComeAndGoFromServer() throws Throwable {
        // The following test uses the actual Firestore
        TestTools.resetLocationServiceStatus(mActivityRule.getActivity().getService());

        Carrier aFakeCarrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.2734f);
        Date rightNow = new Date(System.currentTimeMillis());
        mActivityRule.getActivity().getService().getSender().registerLocation(
                aFakeCarrier, buildLocation(12, 73), rightNow)
                .thenAccept(writeSuccessToUi)
                .exceptionally(writeFailureToUi)
                .thenRun(() -> {
                    Map<Carrier, Boolean> result = null;
                    try {
                        result = getBackSliceData(buildLocation(12, 73), rightNow);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                    assertThat(result.size(), is(1));
                    assertThat(result.containsKey(aFakeCarrier), is(true));
                });
    }

    private CompletableFuture<Map<Carrier, Integer>> getBackRangeData(Location somewhere, Date rangeStart, Date rangeEnd) throws Throwable {
        return mActivityRule.getActivity().getService().getReceiver()
                .getUserNearbyDuring(somewhere, rangeStart, rangeEnd);
    }

    @Test
    public void repetitionsOfSameCarrierAreDetected() throws Throwable {
        // The following test uses the actual Firestore

        Carrier aFakeCarrier = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.2734f);

        Location somewhereInTheWorld = buildLocation(12, 73);

        Date rightNow = new Date(System.currentTimeMillis());
        Date aLittleLater = new Date(rightNow.getTime() + 1000);

        mActivityRule.runOnUiThread(() -> {
            mActivityRule.getActivity().getService().getSender().registerLocation(
                    aFakeCarrier,
                    somewhereInTheWorld,
                    rightNow)
                    .thenAccept(writeSuccessToUi)
                    .exceptionally(writeFailureToUi);
            mActivityRule.getActivity().getService().getSender().registerLocation(
                    aFakeCarrier,
                    somewhereInTheWorld,
                    aLittleLater);
        });

        TestTools.sleep();

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));

        getBackRangeData(somewhereInTheWorld, rightNow, aLittleLater).thenAccept(result -> {
            assertThat(result.size(), is(1));
            assertThat(result.containsKey(aFakeCarrier), is(true));
            assertThat(result.get(aFakeCarrier), is(2));
        });
    }
}
