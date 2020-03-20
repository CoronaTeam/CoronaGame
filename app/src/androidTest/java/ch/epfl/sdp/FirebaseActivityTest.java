package ch.epfl.sdp;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;


public class FirebaseActivityTest {

    private ConnectivityManager cm;


    @Rule
    public GrantPermissionRule internetPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.INTERNET);

    @Rule
    public final ActivityTestRule<FirebaseActivity> mActivityRule =
            new ActivityTestRule<>(FirebaseActivity.class);

    @Before
    public void setup() {
        String internetPermission = "android.permission.ACCESS_INTERNET";
        if (ContextCompat.checkSelfPermission(mActivityRule.getActivity().getBaseContext(),
                internetPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ABORT", "Aborting test -- not having internet permission!!!!");
        }
        //EspressoIdling res: https://developer.android.com/reference/androidx/test/espresso/idling/CountingIdlingResource
    }

    @Test @Ignore
    public void testDataDownloadIsDisplayed() {
        clickWaitAndCheckText(R.id.FirebaseDownloadButton,
                R.id.FirebaseDownloadResult,
                "User#000 => {Position=GeoPoint { latitude=0.0, longitude=0.0 }, Time=Timestamp(seconds=1583276400, nanoseconds=0)}",
                20000);
    }

    @Test
    public void testDataUploadIsDisplayed() {
        clickWaitAndCheckText(R.id.FirebaseUploadButton,
                R.id.FirebaseUploadConfirmation,
                "Document snapshot successfully added to firestore.",
                5000);
    }

    @Test
    public void testDetectNoInternetConnectionWhenUpload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        clickWaitAndCheckText(R.id.FirebaseUploadButton,
                R.id.FirebaseUploadConfirmation,
                "Can't upload while offline",
                0);
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }

    @Test
    public void testDetectNoInternetConnectionWhenDownload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        clickWaitAndCheckText(R.id.FirebaseDownloadButton,
                R.id.FirebaseDownloadResult,
                "Can't download while offline",
                0);
        IS_NETWORK_DEBUG = false;
    }

    @After
    public void restoreOnline() {
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }

    private void clickWaitAndCheckText(int buttonID, int textID, String expectedText, int waitingTime) {
        onView(withId(buttonID)).perform(click());
        waitingForTravis(waitingTime);
        onView(withId(textID)).check(matches(withText(expectedText)));
    }

    private void waitingForTravis(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class MockFirestoreWrapper implements FirestoreWrapper {

        private DocumentReference documentReference;
        private Task<Void> documentSetReferenceTask;

        @Override
        public <A, B> FirestoreWrapper add(Map<A, B> map) {
            return this;
        }

        @Override
        public FirestoreWrapper collection(String collectionPath) {
            return this;
        }

        @Override
        public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
            return this;
        }

        @Override
        public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener) {
            return this;
        }

        @Override
        public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {

            return this;
        }

        @Override
        public FirestoreWrapper get() {
            return null;
        }

        @Override
        public FirestoreWrapper addOnSetSuccessListener(OnSuccessListener<Void> onSuccessListener) {
            documentSetReferenceTask.addOnSuccessListener(onSuccessListener);
            return this;
        }

        @Override
        public FirestoreWrapper addOnSetFailureListener(OnFailureListener onFailureListener) {
            documentSetReferenceTask.addOnFailureListener(onFailureListener);
            return this;
        }

        @Override
        public FirestoreWrapper document(String documentPath) {
            this.documentReference = null;
            return this;
        }

        @Override
        public <A, B> FirestoreWrapper set(Map<A, B> map) {
            this.documentSetReferenceTask = documentReference.set(map);
            return this;
        }

    }
}
