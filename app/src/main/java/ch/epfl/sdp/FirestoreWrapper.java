package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.Map;

public interface FirestoreWrapper extends Serializable {

    <A, B> FirestoreWrapper add(Map<A, B> map);

    FirestoreWrapper collection(String collectionPath);

    FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener);

    FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener);

    FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener);

    FirestoreWrapper addOnSetSuccessListener(OnSuccessListener<Void> onSuccessListener);

    FirestoreWrapper addOnSetFailureListener(OnFailureListener onFailureListener);

    FirestoreWrapper get();

    FirestoreWrapper document(String documentPath);

    <A, B> FirestoreWrapper set(Map<A, B> map);

}