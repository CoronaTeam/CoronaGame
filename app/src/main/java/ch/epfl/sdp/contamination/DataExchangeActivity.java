package ch.epfl.sdp.contamination;

import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ch.epfl.sdp.Account;
import ch.epfl.sdp.AuthenticationManager;
import ch.epfl.sdp.Callback;
import ch.epfl.sdp.ConcreteFirestoreWrapper;
import ch.epfl.sdp.QueryHandler;
import ch.epfl.sdp.R;

public class DataExchangeActivity extends AppCompatActivity {

    // TODO: This activity will be converted into a Service

    private DataSender sender;
    private DataReceiver receiver;

    private TextView exchangeStatus;
    private TextView exchangeContent;

    class ConcreteDataSender implements DataSender {

        private Account account;

        private GridFirestoreInteractor interactor;

        ConcreteDataSender(GridFirestoreInteractor interactor, Account account) {
            this.interactor = interactor;
            this.account = account;
        }

        @VisibleForTesting
        void setInteractor(GridFirestoreInteractor interactor) {
            this.interactor = interactor;
        }

        public OnSuccessListener successListener = o -> {
            exchangeStatus.setText("EXCHANGE Succeeded");
        };

        public OnFailureListener failureListener = e -> {
            exchangeStatus.setText("EXCHANGE Failed");
        };

        @Override
        public void registerLocation(Carrier carrier, Location location, Date time) {

            interactor.write(location, String.valueOf(time.getTime()), carrier, successListener, failureListener);
        }
    }

    class ConcreteDataReceiver implements DataReceiver {

        private Account account;
        private GridFirestoreInteractor interactor;

        ConcreteDataReceiver(GridFirestoreInteractor interactor, Account account) {
            this.interactor = interactor;
            this.account = account;
        }

        @VisibleForTesting
        void setInteractor(GridFirestoreInteractor interactor) {
            this.interactor = interactor;
        }

        @Override
        public void getUserNearby(Location location, Date date, Callback<Set<? extends Carrier>> callback) {

            QueryHandler nearbyHandler = new QueryHandler() {

                @Override
                public void onSuccess(QuerySnapshot snapshot) {

                    Set<Carrier> carriers = new HashSet<>();

                    for (QueryDocumentSnapshot q : snapshot) {
                        carriers.add(new Layman(Enum.valueOf(Carrier.InfectionStatus.class,(String) q.get("infectionStatus")), ((float)((double)q.get("illnessProbability")))));
                    }

                    callback.onCallback(carriers);
                }

                @Override
                public void onFailure() {

                    callback.onCallback(Collections.EMPTY_SET);
                }
            };

            interactor.read(location, date.getTime(), nearbyHandler);
        }

        private Set<Long> filterValidTimes(long startDate, long endDate, QuerySnapshot snapshot) {
            Set<Long> validTimes = new HashSet<>();

            for (QueryDocumentSnapshot q : snapshot) {
                long time = Long.decode((String)q.get("Time"));
                if (startDate <= time && time <= endDate) {
                    validTimes.add(time);
                }
            }

            return validTimes;
        }

        private class SliceQueryHandle implements QueryHandler {

            private Map<Carrier, Integer> metDuringInterval;
            private AtomicInteger done;
            private Set<Long> validTimes;
            private Callback<Map<? extends Carrier, Integer>> callback;

            SliceQueryHandle(Set<Long> validTimes, Map<Carrier, Integer> metDuringInterval, AtomicInteger done, Callback<Map<? extends Carrier, Integer>> callback){
                this.metDuringInterval = metDuringInterval;
                this.done = done;
                this.validTimes = validTimes;
                this.callback = callback;
            }

            private void launchCallback() {
                int size = validTimes.size();
                boolean elected = true;

                done.incrementAndGet();
                if (done.get() == size) {
                    while (!done.compareAndSet(size, 0)) {
                        elected = (done.get() != 0);
                    }
                    if (elected) {
                        callback.onCallback(metDuringInterval);
                    }
                }
            }

            @Override
            public void onSuccess(QuerySnapshot snapshot) {
                for (QueryDocumentSnapshot q : snapshot) {

                    Carrier c = new Layman(
                            Enum.valueOf(Carrier.InfectionStatus.class,(String) q.get("infectionStatus")),
                            ((float)((double)q.get("illnessProbability"))));

                    int numberOfMeetings = 1;
                    if (metDuringInterval.containsKey(c)) {
                        numberOfMeetings += metDuringInterval.get(c);
                    }
                    metDuringInterval.put(c, numberOfMeetings);
                }

                launchCallback();
            }

            @Override
            public void onFailure() {
                // Do nothing
            }
        }

        @Override
        public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {

            Set<Carrier> carriers = new HashSet<>();

            interactor.getTimes(location, new QueryHandler() {

                @Override
                public void onSuccess(QuerySnapshot snapshot) {

                    Set<Long> validTimes = filterValidTimes(startDate.getTime(), endDate.getTime(), snapshot);

                    Map<Carrier, Integer> metDuringInterval = new ConcurrentHashMap<>();

                    AtomicInteger done = new AtomicInteger();

                    QueryHandler updateFromTimeSlice = new SliceQueryHandle(validTimes, metDuringInterval, done, callback);

                    for (long t : validTimes) {
                        interactor.read(location, t, updateFromTimeSlice);
                    }
                }

                @Override
                public void onFailure() {
                    callback.onCallback(Collections.EMPTY_MAP);
                }
            });

        }

        @Override
        public Location getMyLocationAtTime(Date date) {
            return null;
        }

    }


    @VisibleForTesting
    DataSender getSender() {
        return sender;
    }

    @VisibleForTesting
    DataReceiver getReceiver() {
        return receiver;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sender = new ConcreteDataSender(
                new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance())),
                AuthenticationManager.getAccount(this));

        setContentView(R.layout.activity_dataexchange);
        exchangeStatus = findViewById(R.id.exchange_status);
        exchangeContent = findViewById(R.id.exchange_content);

        receiver = new ConcreteDataReceiver(
                new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance())),
                AuthenticationManager.getAccount(this));
    }
}
