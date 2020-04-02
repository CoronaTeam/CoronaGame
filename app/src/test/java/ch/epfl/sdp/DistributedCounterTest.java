package ch.epfl.sdp;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class DistributedCounterTest {

    FirebaseFirestore db;
    DistributedCounter dc;
    DocumentReference mockDocRef = Mockito.mock(DocumentReference.class);
    DocumentReference docRef;

    @Before
    public void setUp(){
        this.db = FirebaseFirestore.getInstance();
        this.dc = new DistributedCounter();
        this.docRef =db.document("Tests/DistributedCounter/FakeCounters/LG1_counter");
    }

    @Test
    public void createCounter() {
        dc.createCounter(docRef, 2);
        assertEquals(docRef,
                db.document("Tests/DistributedCounter/FakeCounters/LG1_counter"));
    }

    @Test
    public void incrementCounter() {
    }

    @Test
    public void decrementCounter() {
    }

    @Test
    public void getCount() {
    }
}