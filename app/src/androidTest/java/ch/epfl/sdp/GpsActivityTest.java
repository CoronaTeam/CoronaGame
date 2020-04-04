package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.LocationBroker.Provider.GPS;
import static ch.epfl.sdp.LocationBroker.Provider.NETWORK;
import static ch.epfl.sdp.TestUtils.buildLocation;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class GpsActivityTest {

    @Rule
    public final ActivityTestRule<GpsActivity> mActivityRule =
            new ActivityTestRule<>(GpsActivity.class, true, false);

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public ExpectedException illegalArgument = ExpectedException.none();

    private void startActivityWithBroker(LocationBroker br) {
        mActivityRule.launchActivity(new Intent());
        mActivityRule.getActivity().setLocationBroker(br);
    }

    @Test
    @Ignore
    public void locationIsUpdated() throws Throwable {
        MockBroker mockBroker = new MockBroker();
        startActivityWithBroker(mockBroker);

        mockBroker.setProviderStatus(true);

        double currLatitude, currLongitude;
        currLatitude = 46.5188;
        currLongitude = 6.5625;

        for (int i = 0; i < 10; i++) {
            double variation = Math.random() * .1;
            if (Math.random() < .5) {
                currLatitude += variation;
                currLatitude = Math.floor(currLatitude * 100) / 100;
            } else {
                currLongitude += variation;
                currLongitude = Math.floor(currLongitude * 100) / 100;
            }
            mockBroker.setFakeLocation(buildLocation(currLatitude, currLongitude));
            Thread.sleep(1000);
            onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith(Double.toString(currLatitude)))));
            onView(withId(R.id.gpsLongitude)).check(matches(withText(startsWith(Double.toString(currLongitude)))));
        }
    }

    @Test
    public void detectsSuccessfulFirestoreUpdate() throws Throwable {
        MockBroker mockBroker = new MockBroker();
        startActivityWithBroker(mockBroker);

        FirestoreInteractor successfulInteractor = new FirestoreInteractor() {
            @Override
            public void writeDocument(CollectionReference collectionReference, Object document,
                                      OnSuccessListener successListener, OnFailureListener failureListener) {
                successListener.onSuccess(null);
            }

            @Override
            public void writeDocumentWithID(DocumentReference documentReference,
                                            Object document,
                                            OnSuccessListener successListener, OnFailureListener failureListener) {
                successListener.onSuccess(null);
            }

            @Override
            public void readCollection(CollectionReference collectionReference, QueryHandler handler) {

            }

            @Override
            public void readDocument(DocumentReference documentReference,
                                     QueryHandler handler) {

            }
        };
        mActivityRule.getActivity().setFirestoreInteractor(successfulInteractor);

        mockBroker.setProviderStatus(true);
        mockBroker.setFakeLocation(buildLocation(10, 20));

        onView(withId(R.id.history_upload_status)).check(matches(withText("SYNC OK")));
    }

    @Test
    public void detectsFailedFirestoreUpdate() throws Throwable {
        MockBroker mockBroker = new MockBroker();
        startActivityWithBroker(mockBroker);

        FirestoreInteractor failingInteractor = new FirestoreInteractor() {
            @Override
            public void writeDocument(CollectionReference collectionReference, Object document,
                                      OnSuccessListener successListener, OnFailureListener failureListener) {
                failureListener.onFailure(new Exception());
            }

            @Override
            public void writeDocumentWithID(DocumentReference documentReference, Object document,
                                            OnSuccessListener successListener, OnFailureListener failureListener) {
                failureListener.onFailure(new Exception());
            }

            @Override
            public void readCollection(CollectionReference collectionReference, QueryHandler handler) {

            }

            @Override
            public void readDocument(DocumentReference documentReference, QueryHandler handler) {

            }
        };

        mActivityRule.getActivity().setFirestoreInteractor(failingInteractor);

        mockBroker.setProviderStatus(true);
        mockBroker.setFakeLocation(buildLocation(10, 20));

        onView(withId(R.id.history_upload_status)).check(matches(withText("SYNC ERROR!")));
    }

    @Test
    public void detectsLackOfSignal() throws Throwable {
        MockBroker withoutSignal = new MockBroker() {

            @Override
            public void setProviderStatus(boolean status) throws Throwable {
                super.setProviderStatus(false);
            }

            @Override
            public boolean isProviderEnabled(Provider provider) {
                return false;
            }
        };
        startActivityWithBroker(withoutSignal);

        withoutSignal.setProviderStatus(false);

        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith("Missing GPS signal"))));
    }

    @Test
    public void UiReactsWhenSwitchingGps() throws Throwable {
        MockBroker mockBroker = new MockBroker();
        startActivityWithBroker(mockBroker);

        mockBroker.setProviderStatus(false);
        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith("Missing GPS signal"))));

        mockBroker.setProviderStatus(true);
        mockBroker.setFakeLocation(buildLocation(12, 19));
        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith(Double.toString(12)))));
        onView(withId(R.id.gpsLongitude)).check(matches(withText(startsWith(Double.toString(19)))));
    }

    @Test
    public void asksForPermissions() throws Throwable {
        Boolean asked = false;
        MockBroker withoutPermissions = new MockBroker() {
            private boolean fakePermissions = false;

            @Override
            public boolean hasPermissions(Provider provider) {
                return fakePermissions;
            }

            @Override
            public void requestPermissions(int requestCode) {
                fakePermissions = true;
                ((AppCompatActivity) listeners).onRequestPermissionsResult(requestCode, new String[]{"GPS"}, new int[]{PackageManager.PERMISSION_GRANTED});
            }
        };
        startActivityWithBroker(withoutPermissions);

        withoutPermissions.setProviderStatus(true);
        withoutPermissions.setFakeLocation(buildLocation(2, 3));

        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith(Double.toString(2)))));
        onView(withId(R.id.gpsLongitude)).check(matches(withText(startsWith(Double.toString(3)))));
    }

    @Test
    public void concreteBrokerFailsPermissionsTest() {
        mActivityRule.launchActivity(new Intent());

        ConcreteLocationBroker concreteBroker = new ConcreteLocationBroker(
                (LocationManager) mActivityRule.getActivity().getSystemService(Context.LOCATION_SERVICE), mActivityRule.getActivity());

        illegalArgument.expect(IllegalArgumentException.class);
        concreteBroker.hasPermissions(NETWORK);
    }

    private class MockBroker implements LocationBroker {
        LocationListener listeners = null;

        Location fakeLocation;
        boolean fakeStatus = true;

        void setFakeLocation(Location location) throws Throwable {
            fakeLocation = location;
            mActivityRule.runOnUiThread(() -> listeners.onLocationChanged(location));
        }

        void setProviderStatus(boolean status) throws Throwable {
            fakeStatus = status;
            if (listeners != null) {
                if (fakeStatus) {
                    mActivityRule.runOnUiThread(() -> listeners.onProviderEnabled(LocationManager.GPS_PROVIDER));
                } else {
                    mActivityRule.runOnUiThread(() -> listeners.onProviderDisabled(LocationManager.GPS_PROVIDER));
                }
            }
        }

        @Override
        public boolean isProviderEnabled(Provider provider) {
            return fakeStatus;
        }

        @Override
        public boolean requestLocationUpdates(Provider provider, long minTimeDelay, float minSpaceDist, LocationListener listener) {
            if (provider == GPS) {
                listeners = listener;
            }
            return true;
        }

        @Override
        public void removeUpdates(LocationListener listener) {
            listeners = null;
        }

        @Override
        public Location getLastKnownLocation(Provider provider) {
            return fakeLocation;
        }

        @Override
        public boolean hasPermissions(Provider provider) {
            return true;
        }

        @Override
        public void requestPermissions(int requestCode) {
            // Trivial since always has permissions
        }
    }
}
