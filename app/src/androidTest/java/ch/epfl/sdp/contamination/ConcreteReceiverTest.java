package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.test.rule.ActivityTestRule;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ch.epfl.sdp.AccountGetting;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.TestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ConcreteReceiverTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    // TODO: Doesn't work yet
    @Test @Ignore
    public final void mockUserGetsHisLastPosition() {
        DataReceiver receiver = mActivityRule.getActivity().getReceiver();

        AtomicReference<Location> myLatestLocation = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);

        Callback<Location> callback = value -> {
            myLatestLocation.set(value);
            done.set(true);
        };

        receiver.getMyLocationAtTime(AccountGetting.getAccount(mActivityRule.getActivity()), new Date(1585761337000L), callback);

        while (!done.get()) {}

        assertThat(myLatestLocation, equalTo(TestUtils.buildLocation(12, 19)));
    }
}
