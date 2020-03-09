package ch.epfl.sdp;


interface Firestore {
    void addFirestoreUser(final Callback<String> callback);
    void readFirestoreData(final Callback<String> callback);
}
