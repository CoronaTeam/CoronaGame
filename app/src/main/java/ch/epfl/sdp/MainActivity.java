package ch.epfl.sdp;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import ch.epfl.sdp.contamination.InfectionActivity;
import ch.epfl.sdp.firestore.FirebaseActivity;
import ch.epfl.sdp.location.LocationService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startLocationService();
    }

    private void startLocationService() {
        // LocationService will be started here
        // It's responsibility of this activity to check that it has
        // permissions to retrieve GPS location
        // TODO: Write code to stop the service somewhere
        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);

        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationService.LocationBinder binder = (LocationService.LocationBinder) service;

                if (!binder.hasGpsPermissions()) {
                    // Request permissions and try to start the aggregator
                    binder.requestGpsPermissions(MainActivity.this);
                    binder.startAggregator();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        bindService(serviceIntent, conn, BIND_AUTO_CREATE);
    }

    /**
     * Called when the user taps the DisplayFirebase button
     */
    public void setFirebaseView(View view) {
        Intent intent = new Intent(this, FirebaseActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the DisplayGps button
     */
    public void setGPSView(View view) {
        Intent intent = new Intent(this, GpsActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the DisplayGps button
     */
    public void setIntroView(View view) {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }

    public void setHistoryView(View view) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the BeginSignIn button
     */
    public void setSignInView(View v) {
        Intent intent = new Intent(this, Authentication.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the Tabs button
     */
    public void setTabsView(View v) {
        Intent intent = new Intent(this, TabActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the UserInfection button
     */
    public void setUserInfectionView(View v) {
        Intent intent = new Intent(this, UserInfectionActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the Infection button
     */
    public void setInfectionView(View v) {
        Intent intent = new Intent(this, InfectionActivity.class);
        startActivity(intent);
    }
}
