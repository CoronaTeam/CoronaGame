package ch.epfl.sdp.contamination;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;

import androidx.fragment.app.Fragment;
import ch.epfl.sdp.R;
import ch.epfl.sdp.SingleFragmentActivity;
import ch.epfl.sdp.fragment.AccountFragment;
import ch.epfl.sdp.location.LocationService;

public class InfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new InfectionFragment();
    }
}
