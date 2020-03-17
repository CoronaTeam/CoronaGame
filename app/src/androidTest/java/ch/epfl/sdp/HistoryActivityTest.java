package ch.epfl.sdp;

import androidx.test.rule.ActivityTestRule;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.anything;
import static org.mockito.Mockito.when;

public class HistoryActivityTest {

    @Rule
    public final ActivityTestRule<HistoryActivity> mActivityRule = new ActivityTestRule<>(HistoryActivity.class);

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private QueryDocumentSnapshot queryDocumentSnapshot;

    @Mock
    private QuerySnapshot unreadableSnapshot;

    @Mock
    private QueryDocumentSnapshot unreadableDocumentSnapshot;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setupMockito() {
        when(querySnapshot.iterator()).thenReturn(Collections.singletonList(queryDocumentSnapshot).iterator());
        when(queryDocumentSnapshot.get("Time")).thenReturn(new Timestamp(new Date(2020, 03, 17, 22, 11, 00)));
        when(queryDocumentSnapshot.get("Position")).thenReturn(new GeoPoint(19, 98));

        when(unreadableSnapshot.iterator()).thenReturn(Collections.singletonList(unreadableDocumentSnapshot).iterator());
        when(unreadableDocumentSnapshot.get("Time")).thenReturn(null);
        when(unreadableDocumentSnapshot.get("Position")).thenReturn(new GeoPoint(19, 98));
    }

    @Test
    public void historyIsUpdated() {
        FirestoreInteractor successInteractor = new FirestoreInteractor() {
            @Override
            void read(QueryHandler handler) {
                handler.onSuccess(querySnapshot);
            }
        };

        mActivityRule.getActivity().setFirestoreInteractor(successInteractor);

        onView(withId(R.id.refresh_history)).perform(click());
        onView(withId(R.id.conn_status)).check(matches(withText("QUERY OK")));
        onData(anything())
                .inAdapterView(withId(R.id.history_tracker))
                .atPosition(0)
                .check(matches(withText("Found @ 19.0:98.0 on Sat Apr 17 22:11:00 GMT+01:00 3920")));
    }

    @Test
    public void failureIsNotified() {
        FirestoreInteractor failureInteractor = new FirestoreInteractor() {
            @Override
            void read(QueryHandler handler) {
                handler.onFailure();
            }
        };

        mActivityRule.getActivity().setFirestoreInteractor(failureInteractor);

        onView(withId(R.id.refresh_history)).perform(click());
        onView(withId(R.id.conn_status)).check(matches(withText("CONNECTION ERROR")));
    }

    @Test
    public void unreadableContentIsPurged() {
        FirestoreInteractor unreadableInteractor = new FirestoreInteractor() {
            @Override
            void read(QueryHandler handler) {
                handler.onSuccess(unreadableSnapshot);
            }
        };

        mActivityRule.getActivity().setFirestoreInteractor(unreadableInteractor);

        onView(withId(R.id.refresh_history)).perform(click());
        onData(anything())
                .inAdapterView(withId(R.id.history_tracker))
                .atPosition(0)
                .check(matches(withText("[...unreadable.:).]")));
    }
}
