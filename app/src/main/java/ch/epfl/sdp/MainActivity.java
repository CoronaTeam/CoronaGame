package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import ch.epfl.sdp.contamination.InfectionActivity;
import ch.epfl.sdp.firestore.FirebaseActivity;
import ch.epfl.sdp.location.LocationService;

public class MainActivity extends AppCompatActivity {

    public static boolean IS_ONLINE = true;
    public static boolean IS_NETWORK_DEBUG = false;

    public static void checkNetworkStatus(AppCompatActivity activity) {
        if (!IS_NETWORK_DEBUG) {
            ConnectivityManager cm =
                    (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            assert cm != null;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            IS_ONLINE = (activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start LocationService here
        // TODO: Write code to stop the service somewhere
        startService(new Intent(this, LocationService.class));
    }

    /**
     * Called when the user taps the DisplayFirebase button
     */
    public void setFirebaseView(View view) {
        Intent intent = new Intent(this, FirebaseActivity.class);
        startActivity(intent);
    }

    /** Called when the user taps the DisplayGps button */
    public void setGPSView(View view) {
        Intent intent = new Intent(this, GpsActivity.class);
        startActivity(intent);
    }

    /** Called when the user taps the DisplayGps button */
    public void setIntroView(View view) {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }
  
    public void setHistoryView(View view) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    /**Called when the user taps the BeginSignIn button*/
    public void setSignInView(View v){
        Intent intent = new Intent(this, Authentication.class);
        startActivity(intent);
    }

    /**Called when the user taps the Tabs button*/
    public void setTabsView(View v){
        Intent intent = new Intent(this, TabActivity.class);
        startActivity(intent);
    }

    /**Called when the user taps the UserInfection button*/
    public void setUserInfectionView(View v) {
        Intent intent = new Intent(this, UserInfectionActivity.class);
        startActivity(intent);
    }

    /**Called when the user taps the Infection button*/
    public void setInfectionView(View v) {
        Intent intent = new Intent(this, InfectionActivity.class);
        startActivity(intent);
    }
}
