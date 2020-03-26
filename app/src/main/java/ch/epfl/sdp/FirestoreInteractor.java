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

    void writeDocument(String path, Map<String, Object> document, Callback callback){
        throw new NotImplementedError();
    }

    void writeDocument(String path, Map<String, Object> document,
                                OnSuccessListener onSuccess, OnFailureListener onFailure){
        throw new NotImplementedError();
    }

    void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                                      Callback callback){
        throw new NotImplementedError();
    }

    void writeDocumentWithID(String path, String documentID, Map<String, Object> document,
                             OnSuccessListener onSuccess, OnFailureListener onFailure){
        throw new NotImplementedError();
    }

    void readDocument(String path, Callback callback){
        throw new NotImplementedError();
    }

    void readDocument(String path, QueryHandler handler){
        throw new NotImplementedError();
    }

    void readDocumentWithID(String path, String documentID, Callback callback){
        throw new NotImplementedError();
    }

    void readDocumentWithID(String path, String documentID, QueryHandler handler){
        throw new NotImplementedError();
    }










}