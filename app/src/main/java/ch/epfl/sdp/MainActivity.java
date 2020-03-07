package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    public static boolean IS_ONLINE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickLaunchGame(View view) {
        Intent intent = new Intent(this, FirebaseActivity.class);
        startActivity(intent);
    }

    public void checkNetworkStatus() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        IS_ONLINE = (activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting());
    }

}
