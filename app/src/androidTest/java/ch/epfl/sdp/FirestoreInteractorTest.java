package ch.epfl.sdp;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class FirestoreInteractorTest {

    @Test
    public void addFirestoreUser() {
    }

    @Test
    public void readFirestoreData() {
    }

    @Test
    public void testHandleUploadDatabaseError() {
        Map<String, Object> user = new HashMap<>();
        user.put("Name", "Bob Bobby");
        user.put("Age", 24);
        user.put("Infected", false);

        /*FirebaseFirestore mockFF = Mockito.mock(FirebaseFirestore.class);
        Mockito.when(mockFF.collection("Players").add(user)).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                throw new Exception();
            }
        });
        FirestoreInteractor fs = new FirestoreInteractor(mockFF);*/

        MockFirestoreInteractor mockFirestoreInteractor = new MockFirestoreInteractor(FirebaseFirestore.getInstance());
        mockFirestoreInteractor.readFirestoreData(new Callback<String>() {
            @Override
            public void onCallback(String value) {
                assertEquals("Error while adding document to firestore.", value);
            }
        });
    }
}