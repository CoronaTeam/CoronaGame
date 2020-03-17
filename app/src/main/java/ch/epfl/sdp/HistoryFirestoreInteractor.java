package ch.epfl.sdp;

public class HistoryFirestoreInteractor extends FirestoreInteractor {

    private FirestoreWrapper wrapper;

    HistoryFirestoreInteractor(FirestoreWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    void read(QueryHandler handler) {
        wrapper.collection("LastPositions")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handler.onSuccess(task.getResult());
                    } else {
                        handler.onFailure();
                    }
                });
    }
}
