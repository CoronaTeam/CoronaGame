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
    private Task<Void> documentSetReferenceTask;

    public ConcreteFirestoreWrapper(FirebaseFirestore realFirestore) {
        firebaseFirestore = realFirestore;
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
    public <A> FirestoreWrapper add(A obj) {
        this.documentReferenceTask = collectionReference.add(obj);
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
    public FirestoreWrapper get() {
        this.querySnapshotTask = collectionReference.get();
        return this;
    }

    @Override
    public FirestoreWrapper document(String documentPath) {
        this.documentReference = collectionReference.document(documentPath);
        return this;
    }

    @Override
    public <A, B> FirestoreWrapper set(Map<A, B> map) {
        this.documentSetReferenceTask = documentReference.set(map);
        return this;
    }

}