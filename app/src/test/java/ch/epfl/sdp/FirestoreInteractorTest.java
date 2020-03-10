package ch.epfl.sdp;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class FirestoreInteractorTest {

    private class MockFirestoreInteractor implements Firestore {

        @Override
        public void readDocument(Callback callback) {

        }

        @Override
        public void writeDocument(Callback callback) {

        }
    }

    private Firestore mockFSI = new MockFirestoreInteractor();
    private Map<String, Object> user = new HashMap<>();

    private void init() {
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);
    }

    @Test
    public void testHandleUploadDatabaseError() {

    }

    @Test
    @Ignore("Not implemented")
    public void testHandleDownloadDatabaseError() {
    }


}