package ch.epfl.sdp;

public interface FirestoreInteractor {
    void readDocument(Callback callback);
    void writeDocument(Callback callback);
}
