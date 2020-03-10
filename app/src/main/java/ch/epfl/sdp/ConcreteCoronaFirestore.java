package ch.epfl.sdp;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class ConcreteCoronaFirestore implements CoronaFirestore {


    public ConcreteCoronaFirestore() {

    }

    @Override
    public <A, B> CoronaFirestore add(Map<A, B> map) {
        return null;
    }

    @Override
    public CoronaFirestore collection(String collectionPath) {
        return null;
    }

    @Override
    public CoronaFirestore addOnSuccessListener(OnSuccessListener<? super DocumentReference> onSuccessListener) {
        return null;
    }

    @Override
    public CoronaFirestore addOnFailureListener(OnFailureListener onFailureListener) {
        return null;
    }

    @Override
    public CoronaFirestore addOnCompleteListener(OnCompleteListener<QuerySnapshot> onCompleteListener) {
        return null;
    }

    @Override
    public CoronaFirestore get() {
        return null;
    }
}
