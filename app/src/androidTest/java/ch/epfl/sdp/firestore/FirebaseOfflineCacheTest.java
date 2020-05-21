package ch.epfl.sdp.firestore;

import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SnapshotMetadata;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sdp.TestTools;

/*
      Run tests in debug mode to see logs & values prints
 */
public class FirebaseOfflineCacheTest {

    private static final String TAG = "OFFLINE CACHE TEST";
    @Rule
    public final ActivityTestRule<FirebaseActivity> mActivityRule =
            new ActivityTestRule<>(FirebaseActivity.class);
    @Rule
    public GrantPermissionRule internetPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.INTERNET);
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String localChanges = "local changes 29 avril";

    @Before
    public void setUp() {
        String internetPermission = "android.permission.ACCESS_INTERNET";
        if (ContextCompat.checkSelfPermission(mActivityRule.getActivity().getBaseContext(),
                internetPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Aborting test -- not having internet permission!!!!");
        }
    }

    /*
    Modify the localChanges variable at each new run to convince yourself that it works
     */
    @Test
    public void synchronizeLocalChangeWithBackendWhenBackOnline() {
        db.disableNetwork()
                .addOnCompleteListener(task -> System.out.println("NETWORK ACCESS IS DISABLED"));
        // write operation to Firestore that is queued when offline
        db.collection("Tests")
                .document("LocalChangeSyncTest")
                .update("locallyChangedValue", localChanges);
        db.enableNetwork()
                .addOnCompleteListener(task -> System.out.println("NETWORK ACCESS IS ENABLED"));
        TestTools.sleep(5000);
        // check changes on Firestore
        db.collection("Tests")
                .document("LocalChangeSyncTest").get()
                .addOnSuccessListener(documentSnapshot ->
                        {
                            System.out.println("SYNCHRONISE LOCAL/BACKEND: locallyChangedValue = " +
                                    documentSnapshot.get("locallyChangedValue"));
                            SnapshotMetadata metadata = documentSnapshot.getMetadata();
                            String source = metadata.isFromCache() ? "local cache" : "server";
                            System.out.println("Data fetched from " + source);
                        }
                )
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error retrieving locallyChangedValue from Firestore while online.", e));
    }

    @Test
    public void readDataFromCacheWhenOffline() {
        db.disableNetwork()
                .addOnCompleteListener(task -> System.out.println("NETWORK ACCESS IS DISABLED"));
        db.collection("Tests")
                .document("LocalChangeSyncTest").get()
                .addOnSuccessListener(documentSnapshot ->
                {
                    SnapshotMetadata metadata = documentSnapshot.getMetadata();
                    String source = metadata.isFromCache() ? "local cache" : "server";
                    System.out.println("Data fetched from " + source);
                    System.out.println("OFFLINE DATA: locallyChangedValue = " +
                            documentSnapshot.get("locallyChangedValue"));
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error retrieving locallyChangedValue from cache Firestore while offline.", e));
        db.enableNetwork()
                .addOnCompleteListener(task -> System.out.println("NETWORK ACCESS IS ENABLED"));
    }
}
