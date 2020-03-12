package ch.epfl.sdp;

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

    private class MockFirestoreWrapper implements FirestoreWrapper {
        private FirebaseFirestore firebaseFirestore;
        private String collectionPath;
        private CollectionReference collectionReference;
        private Task<DocumentReference> documentReferenceTask;
        private Task<QuerySnapshot> querySnapshotTask;
        private DocumentReference documentReference;

        public MockFirestoreWrapper() {

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
            return this;
        }
    }

    private class MockFirestoneWrapperFailure extends MockFirestoreWrapper {
        @Override
        public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
            return this;
        }

        @Override
        public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener) {
            return super.addOnFailureListener(onFailureListener);
        }

        @Override
        public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {
            return super.addOnCompleteListener(onCompleteListener);
        }
    }


    private FirestoreInteractor mockFSI = new ConcreteFirestoreInteractor(new MockFirestoreWrapper());
    private FirestoreInteractor mockFSIFailure =
            new ConcreteFirestoreInteractor(new MockFirestoneWrapperFailure());
    private Map<String, Object> user = new HashMap<>();

    private void init() {
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);
    }

    @Test
    @Ignore("Not implemented")
    public void testHandleUploadDatabaseError() {
        mockFSIFailure.writeDocument(value -> assertEquals(
                "Error adding document to firestore.",
                value));
    }

    @Test
    @Ignore("Not implemented")
    public void testHandleDownloadDatabaseError() {
        mockFSIFailure.readDocument(value -> assertEquals(
                "Error getting firestone documents.", value));
    }

    @Test
    public void testDataDownloadIsReceived() {
        mockFSI.readDocument(value -> assertEquals(
                "User#000 => {Position=GeoPoint { latitude=0.0, longitude=0.0 }, " +
                        "Time=Timestamp(seconds=1583276400, nanoseconds=0)}",
                value));
    }

    @Test
    public void testDataUploadIsReceived() {
        mockFSI.writeDocument(value -> assertEquals("Document snapshot successfully added to " +
                "firestore.", value));
    }

    @Test
    public void testDetectNoInternetConnectionWhenUpload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        mockFSI.writeDocument(value -> assertEquals("Can't upload while offline", value));
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }

    @Test
    public void testDetectNoInternetConnectionWhenDownload() {
        IS_NETWORK_DEBUG = true;
        IS_ONLINE = false;
        mockFSI.readDocument(value -> assertEquals("Can't download while offline", value));
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }


    @After
    public void restoreOnline() {
        IS_ONLINE = true;
        IS_NETWORK_DEBUG = false;
    }


}