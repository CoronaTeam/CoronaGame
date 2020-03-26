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
import ch.epfl.sdp.AccountGetting;
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

        @Override
        public void getUserNearbyDuring(Location location, Date startDate, Date endDate, Callback<Map<? extends Carrier, Integer>> callback) {

            Set<Carrier> carriers = new HashSet<>();

            interactor.getTimes(location, new QueryHandler() {

                @Override
                public void onSuccess(QuerySnapshot snapshot) {

                    Set<Long> validTimes = new HashSet<>();

                    for (QueryDocumentSnapshot q : snapshot) {
                        long time = Long.decode((String)q.get("Time"));
                        if (startDate.getTime() <= time && time <= endDate.getTime()) {
                            validTimes.add(time);
                        }
                    }

                    Map<Carrier, Integer> metDuringInterval = new ConcurrentHashMap<>();

                    AtomicInteger done = new AtomicInteger();

                    QueryHandler updateFromTimeSlice = new QueryHandler() {
                        @Override
                        public void onSuccess(QuerySnapshot snapshot) {
                            for (QueryDocumentSnapshot q : snapshot) {

                                Carrier c = new Layman(Enum.valueOf(Carrier.InfectionStatus.class,(String) q.get("infectionStatus")), ((float)((double)q.get("illnessProbability"))));

                                int numberOfMeetings = 1;
                                if (metDuringInterval.containsKey(c)) {
                                    numberOfMeetings += metDuringInterval.get(c);
                                }
                                metDuringInterval.put(c, numberOfMeetings);
                            }

                            done.incrementAndGet();
                            if (done.get() == validTimes.size()) {
                                while (!done.compareAndSet(validTimes.size(), 0)) {
                                    if (done.get() == 0) {
                                        return;
                                    }
                                }
                                callback.onCallback(metDuringInterval);
                            }
                        }

                        @Override
                        public void onFailure() {
                            // Do nothing
                        }
                    };

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
                AccountGetting.getAccount(this));

        setContentView(R.layout.activity_dataexchange);
        exchangeStatus = findViewById(R.id.exchange_status);
        exchangeContent = findViewById(R.id.exchange_content);

        receiver = new ConcreteDataReceiver(
                new GridFirestoreInteractor(new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance())),
                AccountGetting.getAccount(this));

        //sender.registerLocation(new Layman(Carrier.InfectionStatus.INFECTED), buildLocation(10, 11), new Date(1585223373900L));
        //receiver.getUserNearbyDuring(buildLocation(10, 11), new Date(0L), new Date(1585223373903L), value -> exchangeContent.setText(value.keySet().toString()));
        //receiver.getUserNearby(buildLocation(10, 11), new Date(1585223373883L), value -> exchangeContent.setText(value.toString()));
    }
}
