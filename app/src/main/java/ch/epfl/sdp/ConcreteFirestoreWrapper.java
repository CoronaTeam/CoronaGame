package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class ConcreteFirestoreWrapper implements FirestoreWrapper {

    private final FirebaseFirestore fbfs;


    public ConcreteFirestoreWrapper(FirebaseFirestore realFF) {
        fbfs = realFF;
    }

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
