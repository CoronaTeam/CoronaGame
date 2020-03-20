package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    public static boolean IS_ONLINE = true;
    public static boolean IS_NETWORK_DEBUG = false;

    public static String APP_PREFERENCES = "APP_PREFERENCES";
    public static String OPENED_BEFORE_PREFERENCE = "OPENED_BEFORE";

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

        SharedPreferences sp = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        if (!sp.getBoolean(OPENED_BEFORE_PREFERENCE, false)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(OPENED_BEFORE_PREFERENCE, true);
            editor.apply();
            setIntroView();
        }
    }

    /** Called when the user opens the app for the first time */
    public void setIntroView() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user taps the DisplayFirebase button
     */
    public void setFirebaseView(View view) {
        Intent intent = new Intent(this, FirebaseActivity.class);
        startActivity(intent);
    }

    /** Called when the user taps the DisplayMap button */
    public void setMapView(View view) {
        Intent intent = new Intent(this, MapActivity.class);
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

    public void setUserInfectionView(View v) {
        Intent intent = new Intent(this, UserInfectionActivity.class);
        startActivity(intent);
    }
}
