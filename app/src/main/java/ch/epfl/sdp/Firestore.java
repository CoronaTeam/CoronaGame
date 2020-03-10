package ch.epfl.sdp;

public interface Firestore {
    void readDocument(Callback callback);
    void writeDocument(Callback callback);
}
