package ch.epfl.sdp.contamination;

import android.annotation.TargetApi;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ch.epfl.sdp.Callback;
import ch.epfl.sdp.QueryHandler;
import ch.epfl.sdp.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

public class GridSenderTest {

    @Rule
    public final ActivityTestRule<DataExchangeActivity> mActivityRule = new ActivityTestRule<>(DataExchangeActivity.class);

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private QueryDocumentSnapshot queryDocumentSnapshot;

    @Mock
    private QuerySnapshot firstPeriodSnapshot;

    @Mock
    private QuerySnapshot secondPeriodSnapshot;

    @Mock
    private QueryDocumentSnapshot firstPeriodDocumentSnapshot;

    @Mock
    private QueryDocumentSnapshot secondPeriodDocumentSnapshot;

    @Mock
    private QuerySnapshot timesListSnapshot;

    @Mock
    private QueryDocumentSnapshot range1DocumentSnapshot;

    @Mock
    private QueryDocumentSnapshot range2DocumentSnapshot;

    @Mock
    private QueryDocumentSnapshot afterRangeDocumentSnapshot;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    final long rangeStart = 1585223373883L;
    final long rangeEnd = 1585223373963L;
    final long outsideRange = 1585223373983L;

    @Before
    public void setupMockito() {
        when(querySnapshot.iterator()).thenReturn(Collections.singletonList(queryDocumentSnapshot).iterator());
        when(queryDocumentSnapshot.get("infectionStatus")).thenReturn(Carrier.InfectionStatus.HEALTHY.toString());
        when(queryDocumentSnapshot.get("illnessProbability")).thenReturn(0.5d);

        when(firstPeriodSnapshot.iterator()).thenReturn(Collections.singletonList(firstPeriodDocumentSnapshot).iterator());
        when(secondPeriodSnapshot.iterator()).thenReturn(Collections.singletonList(secondPeriodDocumentSnapshot).iterator());
        when(firstPeriodDocumentSnapshot.get("infectionStatus")).thenReturn(Carrier.InfectionStatus.IMMUNE.toString());
        when(firstPeriodDocumentSnapshot.get("illnessProbability")).thenReturn(0.0d);
        when(secondPeriodDocumentSnapshot.get("infectionStatus")).thenReturn(Carrier.InfectionStatus.UNKNOWN.toString());
        when(secondPeriodDocumentSnapshot.get("illnessProbability")).thenReturn(0.75d);

        when(timesListSnapshot.iterator()).thenReturn(Arrays.asList(range1DocumentSnapshot, range2DocumentSnapshot, afterRangeDocumentSnapshot).iterator());
        when(range1DocumentSnapshot.get("Time")).thenReturn(String.valueOf(rangeStart));
        when(range2DocumentSnapshot.get("Time")).thenReturn(String.valueOf(rangeEnd));
        when(afterRangeDocumentSnapshot.get("Time")).thenReturn(String.valueOf(outsideRange));
    }

    @TargetApi(17)
    private Location buildLocation(double latitude, double longitude) {
        Location l = new Location(LocationManager.GPS_PROVIDER);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        l.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            // Also need to set the et field
            l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        l.setAltitude(400);
        l.setAccuracy(1);
        return l;
    }

    class MockGridInteractor extends GridFirestoreInteractor {

        // TODO: GridFirestoreInteractor should become an interface too
        MockGridInteractor() {
            super(null);
        }
    }

    @Test
    public void dataSenderUploadsInformation() {
        ((DataExchangeActivity.ConcreteDataSender) mActivityRule.getActivity().getSender()).setInteractor(new MockGridInteractor() {
            @Override
            public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
                success.onSuccess(null);
            }
        });

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().getSender().registerLocation(
                new Layman(Carrier.InfectionStatus.HEALTHY),
                buildLocation(10, 20),
                new Date(System.currentTimeMillis())));

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Succeeded")));
    }

    @Test
    public void dataSenderFailsWithError() {
        ((DataExchangeActivity.ConcreteDataSender) mActivityRule.getActivity().getSender()).setInteractor(new MockGridInteractor() {
            @Override
            public void write(Location location, String time, Carrier carrier, OnSuccessListener success, OnFailureListener failure) {
                failure.onFailure(null);
            }
        });

        mActivityRule.getActivity().runOnUiThread(() -> mActivityRule.getActivity().getSender().registerLocation(
                new Layman(Carrier.InfectionStatus.HEALTHY),
                buildLocation(10, 20),
                new Date(System.currentTimeMillis())));

        onView(withId(R.id.exchange_status)).check(matches(withText("EXCHANGE Failed")));
    }

    @Test
    public void dataReceiverFindsContacts() {
        ((DataExchangeActivity.ConcreteDataReceiver) mActivityRule.getActivity().getReceiver()).setInteractor(new MockGridInteractor() {
            @Override
            public void read(Location location, long time, QueryHandler handler) {
                handler.onSuccess(querySnapshot);
            }
        });

        Callback<Set<? extends Carrier>> successCallback = value -> {
            assertThat(value.size(), is(1));
            assertThat(value.iterator().hasNext(), is(true));
            assertThat(value.iterator().next().getIllnessProbability(), is(0.0f));
        };

        mActivityRule.getActivity().getReceiver().getUserNearby(
                buildLocation(10, 20),
                new Date(1585223373883L),
                successCallback);
    }

    @Test
    public void dataReceiverFindsContactsDuring() {

        final Location testLocation = buildLocation(70.5, 71.25);

        ((DataExchangeActivity.ConcreteDataReceiver) mActivityRule.getActivity().getReceiver()).setInteractor(new MockGridInteractor() {

            @Override
            public void getTimes(Location location, QueryHandler handler) {
                if (location != testLocation) {
                    handler.onFailure();
                } else {
                    handler.onSuccess(timesListSnapshot);
                }
            }

            @Override
            public void read(Location location, long time, QueryHandler handler) {
                if (location != testLocation) {
                    handler.onFailure();
                } else {
                    if (time == rangeStart) {
                        handler.onSuccess(firstPeriodSnapshot);
                    } else if (time == rangeEnd) {
                        handler.onSuccess(secondPeriodSnapshot);
                    }
                }
            }
        });

        Callback<Map<? extends Carrier, Integer>> callback = value -> {
            assertThat(value.size(), is(2));
            Log.e("CALLBK", value.keySet().toString());
            assertThat(value.keySet().contains(new Layman(Carrier.InfectionStatus.IMMUNE, 0f)), is(true));
            assertThat(value.containsKey(new Layman(Carrier.InfectionStatus.UNKNOWN)), is(false));
            Carrier unknownLayman = new Layman(Carrier.InfectionStatus.UNKNOWN, 0.75f);
            assertThat(value.get(unknownLayman), is(1));
        };

        mActivityRule.getActivity().getReceiver().getUserNearbyDuring(
                testLocation,
                new Date(rangeStart),
                new Date(rangeEnd),
                callback);
    }

    @Test
    public void laymanEqualityTest() {
        Carrier c1 = new Layman(Carrier.InfectionStatus.INFECTED);
        Carrier c2 = new Layman(Carrier.InfectionStatus.INFECTED, 1f);

        assertThat(c1, is(c2));

        Map<Carrier, Integer> aMap = new HashMap<>();
        aMap.put(c1, 2);

        assertThat(aMap.containsKey(c2), is(true));
    }

}
