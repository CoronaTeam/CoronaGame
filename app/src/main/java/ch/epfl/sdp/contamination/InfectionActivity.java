package ch.epfl.sdp.contamination;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

import androidx.fragment.app.Fragment;
import ch.epfl.sdp.R;
import ch.epfl.sdp.SingleFragmentActivity;
import ch.epfl.sdp.fragment.AccountFragment;

public class InfectionActivity extends SingleFragmentActivity {
    @Override
    protected Fragment createFragment() {
        return new InfectionFragment();
    }
}
