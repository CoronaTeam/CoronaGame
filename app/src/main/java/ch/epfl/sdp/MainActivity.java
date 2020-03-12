package ch.epfl.sdp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
}
