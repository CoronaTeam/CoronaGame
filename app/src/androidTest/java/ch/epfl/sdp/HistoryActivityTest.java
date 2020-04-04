package ch.epfl.sdp;

import androidx.test.rule.ActivityTestRule;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestTools.sleep;
import static org.hamcrest.CoreMatchers.anything;
import static org.mockito.Mockito.when;

public class HistoryActivityTest {

    @Rule
    public final ActivityTestRule<HistoryActivity> mActivityRule = new ActivityTestRule<>(HistoryActivity.class);
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private QuerySnapshot querySnapshot;
    @Mock
    private QueryDocumentSnapshot queryDocumentSnapshot;
    @Mock
    private QuerySnapshot unreadableSnapshot;
    @Mock
    private QueryDocumentSnapshot unreadableDocumentSnapshot;
    private HistoryFragment fragment;

    @Before
    public void setupMockito() {
        when(querySnapshot.iterator()).thenReturn(Collections.singletonList(queryDocumentSnapshot).iterator());
        Date date = new GregorianCalendar(2020, Calendar.MARCH, 17).getTime();
        Map<String, Object> lastPos = new HashMap<>();
        lastPos.put("geoPoint", new GeoPoint(19, 98));
        lastPos.put("timeStamp", new Timestamp(date));
        when(queryDocumentSnapshot.getData()).thenReturn(lastPos);

        when(unreadableSnapshot.iterator()).thenReturn(Collections.singletonList(unreadableDocumentSnapshot).iterator());
        when(unreadableDocumentSnapshot.get("Time")).thenReturn(null);
        when(unreadableDocumentSnapshot.get("Position")).thenReturn(new GeoPoint(19, 98));

        fragment = (HistoryFragment) mActivityRule.getActivity().getSupportFragmentManager().findFragmentById(R.id.history_fragment);
        when(unreadableDocumentSnapshot.getData()).thenReturn(null);
    }

    @Test
    @Ignore
    public void historyIsUpdated() {
        FirestoreInteractor successInteractor = new FirestoreInteractor() {

            @Override
            public void writeDocument(CollectionReference collectionReference, Map<String, Object> document,
                                      OnSuccessListener successListener, OnFailureListener failureListener) {

            }

            @Override
            public void writeDocumentWithID(DocumentReference documentReference, Map<String, Object> document,
                                            OnSuccessListener successListener, OnFailureListener failureListener) {

            }

            @Override
            public void readDocument(CollectionReference collectionReference, QueryHandler handler) {
                handler.onSuccess(querySnapshot);
            }

            @Override
            public void readDocumentWithID(DocumentReference documentReference, Callback callback) {
                callback.onCallback(querySnapshot);
            }
        };

        fragment.setFirestoreInteractor(successInteractor);

        onView(withId(R.id.refresh_history)).perform(click());
        sleep(500);
        onView(withId(R.id.conn_status)).check(matches(withText("QUERY OK")));
        sleep(500);
        onData(anything())
                .inAdapterView(withId(R.id.history_tracker))
                .atPosition(0)
                .check(matches(withText(CoreMatchers.startsWith("Found @ 19.0:98.0 on "))));
        onData(anything())
                .inAdapterView(withId(R.id.history_tracker))
                .atPosition(0)
                .check(matches(withText(CoreMatchers.containsString("2020"))));
        onData(anything())
                .inAdapterView(withId(R.id.history_tracker))
                .atPosition(0)
                .check(matches(withText(CoreMatchers.containsString("17"))));
    }

    @Test
    @Ignore
    public void failureIsNotified() {
        FirestoreInteractor failureInteractor = new FirestoreInteractor() {

            @Override
            public void writeDocument(CollectionReference collectionReference, Map<String, Object> document,
                                      OnSuccessListener successListener, OnFailureListener failureListener) {

            }

            @Override
            public void writeDocumentWithID(DocumentReference documentReference, Map<String,
                    Object> document, OnSuccessListener successListener, OnFailureListener failureListener) {

            }

            @Override
            public void readDocument(CollectionReference collectionReference, QueryHandler handler) {
                handler.onFailure();
            }

            @Override
            public void readDocumentWithID(DocumentReference documentReference, Callback callback) {
                callback.onCallback("Error");
            }
        };

        fragment.setFirestoreInteractor(failureInteractor);
        onView(withId(R.id.refresh_history)).perform(click());
    }

    @Test
    @Ignore
    public void unreadableContentIsPurged() {
        FirestoreInteractor unreadableInteractor = new FirestoreInteractor() {

            @Override
            public void writeDocument(CollectionReference collectionReference, Map<String, Object> document,
                                      OnSuccessListener successListener, OnFailureListener failureListener) {

            }

            @Override
            public void writeDocumentWithID(DocumentReference documentReference, Map<String, Object> document,
                                            OnSuccessListener successListener, OnFailureListener failureListener) {

            }

            @Override
            public void readDocument(CollectionReference collectionReference, QueryHandler handler) {
                handler.onSuccess(unreadableSnapshot);
            }

            @Override
            public void readDocumentWithID(DocumentReference documentReference, Callback callback) {
                callback.onCallback(unreadableSnapshot);
            }
        };

        fragment.setFirestoreInteractor(unreadableInteractor);

        onView(withId(R.id.refresh_history)).perform(click());
        sleep(500);
        onData(anything())
                .inAdapterView(withId(R.id.history_tracker))
                .atPosition(0)
                .check(matches(withText("[...unreadable.:).]")));
    }
}
