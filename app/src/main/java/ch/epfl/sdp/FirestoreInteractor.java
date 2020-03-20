package ch.epfl.sdp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Map;

import kotlin.NotImplementedError;

// TODO: This should become an interface again (is abstract class for legacy compatibility)
abstract class FirestoreInteractor {

    /*
    Generic read/write operations on String results
     */
    void readDocument(Callback callback) {
        throw new NotImplementedError();
    }
    void writeDocument(Callback callback) {
        throw new NotImplementedError();
    }

    /*
    Specific read operation (for QuerySnapshot), by default it uses readDocument
     */
    void read(QueryHandler handler) {
        // TODO: Convert this class to interface
        throw new NotImplementedError();
    }

    void write(Map<String, PositionRecord> content, OnSuccessListener success, OnFailureListener failure) {
        throw new NotImplementedError();
    }

}