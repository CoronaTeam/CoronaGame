package ch.epfl.sdp.contamination;

import android.location.Location;

import androidx.annotation.VisibleForTesting;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

import ch.epfl.sdp.Account;

public class ConcreteDataSender implements DataSender {
    private Account account;
    private GridFirestoreInteractor interactor;

    public ConcreteDataSender(GridFirestoreInteractor interactor, Account account) {
        this.interactor = interactor;
        this.account = account;
    }

    @VisibleForTesting
    void setInteractor(GridFirestoreInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public CompletableFuture<Void> registerLocation(Carrier carrier, Location location, Date time) {
        return interactor.gridWrite(location, String.valueOf(time.getTime()), carrier);
    }
}
