package ch.epfl.sdp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class HistoryActivity extends AppCompatActivity {

    private FirestoreInteractor db;

    private QueryHandler handler;

    private TextView connectionStatus;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new HistoryFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance()));

        ListView historyTracker = findViewById(R.id.history_tracker);

        ArrayAdapter historyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        historyTracker.setAdapter(historyAdapter);

        connectionStatus = findViewById(R.id.conn_status);

        handler = new QueryHandler() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                historyAdapter.clear();
                connectionStatus.setText("QUERY OK");
                for (QueryDocumentSnapshot qs : snapshot) {
                    try {
                        historyAdapter.insert(String.format("Found @ %s:%s on %s",
                                ((GeoPoint)(qs.get("Position"))).getLatitude(),
                                ((GeoPoint)(qs.get("Position"))).getLongitude(),
                                ((Timestamp)(qs.get("Time"))).toDate()), 0);
                    } catch (NullPointerException e) {
                        historyAdapter.insert("[...unreadable.:).]", 0);
                    }
                }
            }

            @Override
            public void onFailure() {
                connectionStatus.setText("CONNECTION ERROR");
            }
        };

        refreshHistory(null);
    }

    @VisibleForTesting
    void setFirestoreInteractor(FirestoreInteractor interactor) {
        db = interactor;
    }

    public void refreshHistory(View view) {
        connectionStatus.setText("Loading...");
        db.read(handler);
    }


}
