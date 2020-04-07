package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

public class ConcreteDataSender implements DataSender {
    private Account account;
    private GridFirestoreInteractor gridInteractor;
    private FirestoreInteractor normalInteractor; //for alerts

    // Default success listener
    private OnSuccessListener successListener = o -> { };

    // Default Failure listener
    private OnFailureListener failureListener = e -> { };

    public ConcreteDataSender(FirestoreInteractor interactor, Account account) {
        this.gridInteractor = new GridFirestoreInteractor(interactor);
        this.normalInteractor = interactor;
        this.account = account;
    }

    public ConcreteDataSender setOnSuccessListener(OnSuccessListener successListener) {
        this.successListener = successListener;
        return this;
    }

    public ConcreteDataSender setOnFailureListener(OnFailureListener failureListener) {
        this.failureListener = failureListener;
        return this;
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.gridInteractor = interactor;
    }

    @Override
    public void registerLocation(Carrier carrier, Location location, Date time) {

        gridInteractor.write(location, String.valueOf(time.getTime()), carrier, successListener, failureListener);
    }

    @Override
    public void sendAlert(String userId) {
//        String path = "publicPlayers/" + userId;// + "/lastMetPerson";
//        normalInteractor.readDocument(path, /*new QueryHandler<DocumentReference> */ h ->{

    }
}
