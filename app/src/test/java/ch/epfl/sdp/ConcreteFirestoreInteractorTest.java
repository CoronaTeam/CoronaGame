package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class ConcreteFirestoreInteractorTest {


    private class MockFirestoreWrapper implements FirestoreWrapper{

        @Override
        public <A, B> FirestoreWrapper add(Map<A, B> map) {
            return null;
        }

        @Override
        public FirestoreWrapper collection(String collectionPath) {
            return null;
        }

        @Override
        public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
            return null;
        }

        @Override
        public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener) {
            return null;
        }

        @Override
        public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {
            return null;
        }

        @Override
        public FirestoreWrapper get() {
            return null;
        }
    }


    private FirestoreInteractor mockFSI = new ConcreteFirestoreInteractor(new MockFirestoreWrapper());
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