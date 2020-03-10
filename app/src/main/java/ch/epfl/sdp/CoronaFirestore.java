package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public interface CoronaFirestore {

    public <A,B> CoronaFirestore add(Map<A, B> map);

    public CoronaFirestore collection(String collectionPath);

    public CoronaFirestore addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener);

    public CoronaFirestore addOnFailureListener(OnFailureListener onFailureListener);

    public CoronaFirestore addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener);

    public CoronaFirestore get();

}
