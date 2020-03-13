package ch.epfl.sdp;

interface FirestoreInteractor {
    void readDocument(Callback callback);

    void writeDocument(Callback callback);
}
