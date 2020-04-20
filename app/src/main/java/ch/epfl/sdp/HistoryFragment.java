package ch.epfl.sdp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import ch.epfl.sdp.firestore.FirestoreInteractor;
import ch.epfl.sdp.firestore.QueryHandler;

public class HistoryFragment extends Fragment {

    private HistoryFirestoreInteractor db;

    private Account account;

    private QueryHandler handler;

    private TextView connectionStatus;

    private View view;

    @VisibleForTesting
    void setHistoryFirestoreInteractor(HistoryFirestoreInteractor interactor) {
        db = interactor;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);

        account = AuthenticationManager.getAccount(getActivity());
        db = new HistoryFirestoreInteractor(account);
        connectionStatus = view.findViewById(R.id.conn_status);
        initQueryHandler();
        refreshHistory(null);

        // Callbacks have to be manually added instead of onClick in xml
        view.findViewById(R.id.refresh_history).setOnClickListener(this::refreshHistory);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void initQueryHandler() {
        ArrayAdapter historyAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1);
        ListView historyTracker = view.findViewById(R.id.history_tracker);
        historyTracker.setAdapter(historyAdapter);

        handler = new QueryHandler<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                historyAdapter.clear();
                connectionStatus.setText(R.string.query_ok);
                for (QueryDocumentSnapshot qs : snapshot) {
                    try {
                        Map<String, Object> positionRecord = qs.getData();
                        historyAdapter.insert(String.format("Found @ %s:%s on %s",
                                ((GeoPoint)(positionRecord.get("geoPoint"))).getLatitude(),
                                ((GeoPoint)(positionRecord.get("geoPoint"))).getLongitude(),
                                ((Timestamp)(positionRecord.get("timeStamp"))).toDate()), 0);
                    } catch (NullPointerException e) {
                        historyAdapter.insert("[...unreadable.:).]", 0);
                    }
                }
            }

            @Override
            public void onFailure() {
                connectionStatus.setText(R.string.connection_error);
            }
        };
    }

    private void refreshHistory(View view) {
        connectionStatus.setText(R.string.loading_with_dots);
        db.read(handler);
    }


}
