package ch.epfl.sdp;

import com.google.firebase.firestore.QuerySnapshot;

public interface QueryHandler {

    void onSuccess(QuerySnapshot snapshot);

    void onFailure();
}
