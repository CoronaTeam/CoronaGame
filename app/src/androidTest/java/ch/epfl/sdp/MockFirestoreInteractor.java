package ch.epfl.sdp;

import com.google.firebase.firestore.FirebaseFirestore;

public class MockFirestoreInteractor implements Firestore {

    private FirebaseFirestore db;

    public MockFirestoreInteractor(FirebaseFirestore firebaseFirestore) {
        db = firebaseFirestore;
    }

    @Override
    public void addFirestoreUser(Callback<String> callback) {

    }

    @Override
    public void readFirestoreData(Callback<String> callback) {

    }
}