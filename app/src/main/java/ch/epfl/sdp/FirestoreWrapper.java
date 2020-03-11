package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.Map;

public interface FirestoreWrapper extends Serializable {

    public <A, B> FirestoreWrapper add(Map<A, B> map);

    public FirestoreWrapper collection(String collectionPath);

    public FirestoreWrapper addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener);

    public FirestoreWrapper addOnFailureListener(OnFailureListener onFailureListener);

    public FirestoreWrapper addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener);

    public FirestoreWrapper get();

}
