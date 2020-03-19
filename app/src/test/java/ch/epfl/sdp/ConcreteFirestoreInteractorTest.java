package ch.epfl.sdp;

import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static ch.epfl.sdp.MainActivity.IS_NETWORK_DEBUG;
import static ch.epfl.sdp.MainActivity.IS_ONLINE;
import static org.junit.Assert.assertEquals;


public class ConcreteFirestoreInteractorTest {

    private final MockFirestoreWrapper mockWrapper = new MockFirestoreWrapper();
    private final FirestoreInteractor mockFSI = new ConcreteFirestoreInteractor(mockWrapper,
            new CountingIdlingResource("FooServerCalls"));
    private final FirestoreInteractor mockFSIFailure =
            new ConcreteFirestoreInteractor(new MockFirestoneWrapperFailure(),
                    new CountingIdlingResource("FooServerFailureCalls"));
    private final Map<String, Object> user = new HashMap<>();

    private void init() {
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);
    }

    @Test
    public void testHandleUploadDatabaseError() {
        mockWrapper.setMask("failure");
        mockFSIFailure.writeDocument(value -> assertEquals(
                "Error adding document to firestore.",
                value));
    }

    @Test
    public void testHandleDownloadDatabaseError() {
        mockWrapper.setMask("complete");
        mockFSIFailure.readDocument(value -> assertEquals(
                "Error getting firestone documents.", value));
    }

    @Test
    @Ignore("Still have to figure out the best way to fully mock FB")
    public void testDataDownloadIsReceived() {
        mockWrapper.setMask("complete");
        mockFSI.readDocument(value -> assertEquals(
                "User#000 => {Position=GeoPoint { latitude=0.0, longitude=0.0 }, " +
                        "Time=Timestamp(seconds=1583276400, nanoseconds=0)}",
                value));
    }

    @Test
    public void testDataUploadIsReceived() {
        mockWrapper.setMask("success");
        mockFSI.writeDocument(value -> assertEquals("Document snapshot successfully added to " +
                "firestore.", value));
    }

    @After
    public void restoreOnline() {
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }

    private class MockFirestoreWrapper implements FirestoreWrapper {
        String mask;
        private FirebaseFirestore firebaseFirestore;
        private String collectionPath;
        private CollectionReference collectionReference;
        private Task<DocumentReference> documentReferenceTask;
        private Task<QuerySnapshot> querySnapshotTask;
        private DocumentReference documentReference;

        MockFirestoreWrapper() {

        }

        void setMask(String mask) {
            this.mask = mask;
        }

        @Override
        public <A, B> FirestoreWrapper add(Map<A, B> map) {
            return this;
        }

        @Override
        public FirestoreWrapper collection(String newCollectionPath) {
            return this;
        }

        @Override
        public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
            if (mask.equals("success")) {
                onSuccessListener.onSuccess(null);
            }
            return this;
        }

        @Override
        public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener) {
            if (mask.equals("failure")) {
                onFailureListener.onFailure(null);
            }
            return this;
        }

        @Override
        public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {
            if (mask.equals("complete")) {
                onCompleteListener.onComplete(null);
            }
            return this;
        }

        @Override
        public FirestoreWrapper addOnSetSuccessListener(OnSuccessListener<Void> onSuccessListener) {
            return null;
        }

        @Override
        public FirestoreWrapper addOnSetFailureListener(OnFailureListener onFailureListener) {
            return null;
        }

        @Override
        public FirestoreWrapper get() {
            return this;
        }

        @Override
        public FirestoreWrapper document(String documentPath) {
            return null;
        }

        @Override
        public <A, B> FirestoreWrapper set(Map<A, B> map) {
            return null;
        }
    }

    private class MockFirestoneWrapperFailure extends MockFirestoreWrapper {
        @Override
        public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
            return this;
        }

        @Override
        public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener) {
            onFailureListener.onFailure(null);
            return this;
        }

        @Override
        public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {
            return this;
        }
    }


}