package ch.epfl.sdp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.sdp.contamination.ConcreteDataSender;
import ch.epfl.sdp.contamination.ConcretePositionAggregator;
import ch.epfl.sdp.contamination.GridFirestoreInteractor;
import ch.epfl.sdp.contamination.InfectionActivity;

import static ch.epfl.sdp.LocationBroker.Provider.GPS;

public class GpsActivity extends AppCompatActivity implements LocationListener {

    public final static int LOCATION_PERMISSION_REQUEST = 20201;
    private static final int MIN_UP_INTERVAL_MILLISECS = 1000;
    private static final int MIN_UP_INTERVAL_METERS = 5;

    private LocationBroker locationBroker;

    private EditText latitudeBox;
    private EditText longitudeBox;
    private TextView uploadStatus;

    private ArrayAdapter<String> trackerAdapter;

    private Location prevLocation;

    private Account account;

    private FirestoreInteractor db;

    private ConcretePositionAggregator aggregator;

    @VisibleForTesting
    void setLocationBroker(LocationBroker testBroker) {
        locationBroker = testBroker;
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
    }

    @VisibleForTesting
    void setFirestoreInteractor(FirestoreInteractor interactor) {
        db = interactor;
    }

    private void displayNewLocation(Location newLocation) {
        latitudeBox.setText(String.valueOf(newLocation.getLatitude()), TextView.BufferType.SPANNABLE);
        longitudeBox.setText(String.valueOf(newLocation.getLongitude()), TextView.BufferType.SPANNABLE);

        if (prevLocation == null) {
            prevLocation = newLocation;
        }

        double differenceThreshold = 0.0001;
        if (Math.abs(newLocation.getLatitude() - prevLocation.getLatitude()) > differenceThreshold ||
                Math.abs(newLocation.getLongitude() - prevLocation.getLongitude()) > differenceThreshold) {
            prevLocation = newLocation;
            Calendar now = Calendar.getInstance();
            trackerAdapter.insert(String.format("Last seen on %d:%d:%d @ %f, %f",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    now.get(Calendar.SECOND),
                    newLocation.getLatitude(),
                    newLocation.getLongitude()), 0);
        }
    }

    private void registerNewLocation(Location newLocation) {
        uploadStatus.setText("Uploading...");
        Map<String, PositionRecord> element = new HashMap();
        element.put("Position", new PositionRecord(Timestamp.now(), new GeoPoint(newLocation.getLatitude(), newLocation.getLongitude())));
        db.write(element, o -> {
            uploadStatus.setText("SYNC OK");
        }, e -> {
            uploadStatus.setText("SYNC ERROR!");
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationBroker.hasPermissions(GPS)) {
            registerNewLocation(location);
            displayNewLocation(location);
            //TODO : send new position the the aggregator
            aggregator.addPosition(location);
        } else {
            Toast.makeText(this, "Missing permission", Toast.LENGTH_LONG).show();
        }
    }

    private void goOnline() {
        locationBroker.requestLocationUpdates(GPS, MIN_UP_INTERVAL_MILLISECS, MIN_UP_INTERVAL_METERS, this);
        Toast.makeText(this, R.string.gps_status_on, Toast.LENGTH_SHORT).show();
        //TODO : notify the aggregator that we are back online
        aggregator.updateToOnline();
    }

    private void goOffline() {
        latitudeBox.setText(R.string.gps_signal_missing);
        longitudeBox.setText(R.string.gps_signal_missing);
        Toast.makeText(this, R.string.gps_status_off, Toast.LENGTH_LONG).show();
        //TODO : notify the aggregator that we are now offline
        aggregator.updateToOffline();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            if (locationBroker.hasPermissions(GPS)) {
                goOnline();
            } else {
                locationBroker.requestPermissions(LOCATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            goOffline();
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goOnline();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        latitudeBox = findViewById(R.id.gpsLatitude);
        longitudeBox = findViewById(R.id.gpsLongitude);
        uploadStatus = findViewById(R.id.history_upload_status);

        ListView locationTracker = findViewById(R.id.location_tracker);

        trackerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        locationTracker.setAdapter(trackerAdapter);

        // TODO: do not execute in production code
        if (locationBroker == null) {
            locationBroker = new ConcreteLocationBroker((LocationManager) getSystemService(Context.LOCATION_SERVICE), this);
        }

        // TODO: Take real account
        account = AccountGetting.getAccount(this);

        FirestoreWrapper wrapper = new ConcreteFirestoreWrapper(FirebaseFirestore.getInstance());
        db = new HistoryFirestoreInteractor(wrapper, account);
        Log.e("TEST", account.getId());

        // TODO: Instantiate an aggregator using a DataSender using changes from feature/infectmodelview
        this.aggregator = new ConcretePositionAggregator(new ConcreteDataSender(new GridFirestoreInteractor(wrapper), account), InfectionActivity.getAnalyst());
    }

    // TODO: think about using bluetooth technology to improve accuracy

    @Override
    protected void onResume() {
        super.onResume();
        if (locationBroker.isProviderEnabled(GPS) && locationBroker.hasPermissions(GPS)) {
            goOnline();
        } else if (locationBroker.isProviderEnabled(GPS)) {
            // Must ask for permissions
            locationBroker.requestPermissions(LOCATION_PERMISSION_REQUEST);
        } else {
            goOffline();
        }
    }

}
