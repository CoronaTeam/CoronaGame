package ch.epfl.sdp.firestore;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public interface QueryHandler<T> {

    void onSuccess(T snapshot);

    void onFailure();
}
