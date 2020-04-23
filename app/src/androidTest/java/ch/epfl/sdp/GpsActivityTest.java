package ch.epfl.sdp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.epfl.sdp.contamination.ConcreteAnalysis;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.location.ConcreteLocationBroker;
import ch.epfl.sdp.location.LocationBroker;
import ch.epfl.sdp.location.LocationService;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.TestTools.resetLocationServiceStatus;
import static ch.epfl.sdp.TestTools.sleep;
import static ch.epfl.sdp.TestUtils.buildLocation;
import static ch.epfl.sdp.location.LocationBroker.Provider.GPS;
import static ch.epfl.sdp.location.LocationBroker.Provider.NETWORK;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class GpsActivityTest {

    @Rule
    public final ActivityTestRule<GpsActivity> mActivityRule =
            new ActivityTestRule<>(GpsActivity.class, true, false);

    @Rule
    public GrantPermissionRule locationPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public ExpectedException illegalArgument = ExpectedException.none();

    private void startActivityWithBroker(LocationBroker br) throws Throwable {
        mActivityRule.launchActivity(new Intent());
        AtomicBoolean done = new AtomicBoolean(false);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                done.set(true);
                LocationService locationService = ((LocationService.LocationBinder)service).getService();
                resetLocationServiceStatus(locationService);
                // TODO: Refactor this to use only 1 reset function
                locationService.setAnalyst(new ConcreteAnalysis(
                        locationService.getAnalyst().getCarrier(),
                        locationService.getReceiver(),
                        locationService.getSender()));
                locationService.setBroker(br);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                done.set(false);
            }
        };
        mActivityRule.getActivity().bindService(new Intent(mActivityRule.getActivity(), LocationService.class), conn, Context.BIND_AUTO_CREATE);

        while (!done.get()) {}

        mActivityRule.runOnUiThread(() -> mActivityRule.getActivity().activatePosition());
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
        detectsTest(true, null, "SYNC OK");
    }

    @Test
    public void detectsFailedFirestoreUpdate() throws Throwable {
        detectsTest(false, null, "SYNC ERROR!");
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
        sleep();
        onView(withId(R.id.gpsLatitude)).check(matches(withText(startsWith(Double.toString(12)))));
        onView(withId(R.id.gpsLongitude)).check(matches(withText(startsWith(Double.toString(19)))));
    }

    @Test
    public void asksForPermissions() throws Throwable {
        MockBroker withoutPermissions = new MockBroker() {
            private boolean fakePermissions = false;

            @Override
            public boolean hasPermissions(Provider provider) {
                return fakePermissions;
            }

            @Override
            public void requestPermissions(Activity activity, int requestCode) {
                fakePermissions = true;
                activity.onRequestPermissionsResult(requestCode, new String[]{"GPS"}, new int[]{PackageManager.PERMISSION_GRANTED});
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
        LocationListener listener = null;

        Location fakeLocation;
        boolean fakeStatus = true;

        void setFakeLocation(Location location) throws Throwable {
            fakeLocation = location;
            mActivityRule.runOnUiThread(() -> listener.onLocationChanged(location));
        }

        void setProviderStatus(boolean status) throws Throwable {
            fakeStatus = status;
            if (listener != null) {
                if (fakeStatus) {
                    mActivityRule.runOnUiThread(() -> listener.onProviderEnabled(LocationManager.GPS_PROVIDER));
                } else {
                    mActivityRule.runOnUiThread(() -> listener.onProviderDisabled(LocationManager.GPS_PROVIDER));
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
                this.listener = listener;
            }
            return true;
        }

        @Override
        public void removeUpdates(LocationListener listener) {
            this.listener = null;
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
        public void requestPermissions(Activity activity, int requestCode) {
            // Trivial since always has permissions
        }
    }

    private class MockHistoryFirestoneInteractor extends HistoryFirestoreInteractor{
        private Boolean success;
        private Object onSuccess;

        MockHistoryFirestoneInteractor(Account user, Boolean success, Object onSuccess) {
            super(user);
            this.success = success;
            this.onSuccess = onSuccess;
        }

        @Override
        public CompletableFuture<Map<String, Object>> readDocument(DocumentReference documentReference) {

            return null;
        }

        @Override
        public CompletableFuture<Map<String, Map<String, Object>>> readCollection(CollectionReference collectionReference) {
            return null;
        }

        @Override
        public CompletableFuture<Void> writeDocumentWithID(DocumentReference documentReference, Object document) {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            if(success) completableFuture.complete(null);
            else completableFuture.completeExceptionally(new RuntimeException("Exception!"));
            return completableFuture;
        }

        @Override
        public CompletableFuture<DocumentReference> writeDocument(CollectionReference collectionReference, Object document) {
            CompletableFuture<DocumentReference> completableFuture = new CompletableFuture<>();
            if(success) completableFuture.complete(null);
            else completableFuture.completeExceptionally(new RuntimeException("Exception!"));
            return completableFuture;
        }

    }

    private FirestoreInteractor createWriteFirestoreInteractor(Boolean success, Object onSuccess) {
        return new FirestoreInteractor() {

            @Override
            public CompletableFuture<Map<String, Object>> readDocument(DocumentReference documentReference) {

                return null;
            }

            @Override
            public CompletableFuture<Map<String, Map<String, Object>>> readCollection(CollectionReference collectionReference) {
                return null;
            }

            @Override
            public CompletableFuture<Void> writeDocumentWithID(DocumentReference documentReference, Object document) {
                CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                if(success) completableFuture.complete(null);
                else completableFuture.completeExceptionally(new RuntimeException("Exception!"));
                return completableFuture;
            }

            @Override
            public CompletableFuture<DocumentReference> writeDocument(CollectionReference collectionReference, Object document) {
                CompletableFuture<DocumentReference> completableFuture = new CompletableFuture<>();
                if(success) completableFuture.complete(null);
                else completableFuture.completeExceptionally(new RuntimeException("Exception!"));
                return completableFuture;
            }
        };
    }

    private void detectsTest(Boolean isSuccess, Object onSuccess, String expectedText) throws Throwable {
        MockBroker mockBroker = new MockBroker();
        startActivityWithBroker(mockBroker);

        HistoryFirestoreInteractor interactor = new MockHistoryFirestoneInteractor(
                AuthenticationManager.getAccount(mActivityRule.getActivity()), isSuccess,
                onSuccess);

        mActivityRule.getActivity().setHistoryFirestoreInteractor(interactor);

        mockBroker.setProviderStatus(true);
        mockBroker.setFakeLocation(buildLocation(10, 20));

        onView(withId(R.id.history_upload_status)).check(matches(withText(expectedText)));
    }
}
