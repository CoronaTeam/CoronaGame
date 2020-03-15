package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class ConcreteFirestoreWrapper implements FirestoreWrapper {

    private final FirebaseFirestore firebaseFirestore;
    private CollectionReference collectionReference;
    private Task<DocumentReference> documentReferenceTask;
    private Task<QuerySnapshot> querySnapshotTask;
    private DocumentReference documentReference;


    public ConcreteFirestoreWrapper(FirebaseFirestore realFirestone) {
        firebaseFirestore = realFirestone;
    }

    @Override
    public FirestoreWrapper collection(String newCollectionPath) {
        this.collectionReference = firebaseFirestore.collection(newCollectionPath);
        return this;
    }

    @Override
    public <A, B> FirestoreWrapper add(Map<A, B> map) {
        this.documentReferenceTask = collectionReference.add(map);
        return this;
    }

    @Override
    public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
        documentReferenceTask.addOnSuccessListener(onSuccessListener);
        return this;
    }

    @Override
    public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener) {
        documentReferenceTask.addOnFailureListener(onFailureListener);
        return this;
    }

    @Override
    public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {
        querySnapshotTask.addOnCompleteListener(onCompleteListener);
        return this;
    }


    @Override
    public FirestoreWrapper get() {
        this.querySnapshotTask = collectionReference.get();
        return this;
    }

}