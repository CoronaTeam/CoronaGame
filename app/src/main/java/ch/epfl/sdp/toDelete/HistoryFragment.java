package ch.epfl.sdp.toDelete;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import ch.epfl.sdp.utilities.Account;
import ch.epfl.sdp.utilities.AuthenticationManager;
import ch.epfl.sdp.R;

public class HistoryFragment extends Fragment {

    private HistoryFirestoreInteractor db;

    private Account account;

    private TextView connectionStatus;

    private View view;

    @VisibleForTesting
    public void setHistoryFirestoreInteractor(HistoryFirestoreInteractor interactor) {
        db = interactor;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);

        account = AuthenticationManager.getAccount(getActivity());
        db = new HistoryFirestoreInteractor(account);
        connectionStatus = view.findViewById(R.id.conn_status);
        refreshHistory(null);

        // Callbacks have to be manually added instead of onClick in xml
        view.findViewById(R.id.refresh_history).setOnClickListener(this::refreshHistory);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private CompletableFuture<Void> refreshHistory(View view) {
        connectionStatus.setText(R.string.loading_with_dots);
        return db.readHistory().thenAccept(res -> {
            ArrayAdapter historyAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_1);
            ListView historyTracker = view.findViewById(R.id.history_tracker);
            historyTracker.setAdapter(historyAdapter);
            historyAdapter.clear();

            connectionStatus.setText(R.string.query_ok);

            for (Map.Entry<String, Map<String, Object>> mapEntry : res.entrySet()) {
                try {
                    Map<String, Object> positionRecord = mapEntry.getValue();
                    //TODO resource strings with interpolation
                    historyAdapter.insert(String.format("Found @ %s:%s on %s",
                            ((GeoPoint)(positionRecord.get("geoPoint"))).getLatitude(),
                            ((GeoPoint)(positionRecord.get("geoPoint"))).getLongitude(),
                            ((Timestamp)(positionRecord.get("timeStamp"))).toDate()), 0);
                } catch (NullPointerException e) {
                    historyAdapter.insert("[...unreadable.:).]", 0);
                }
            }
        }).exceptionally(e -> {
            connectionStatus.setText(R.string.connection_error);
            return null;
        });
    }


}
