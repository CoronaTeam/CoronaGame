package ch.epfl.sdp.utilities;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import ch.epfl.sdp.R;

public abstract class SingleFragmentActivity extends FragmentActivity {
    protected abstract Fragment createFragment();

    public Fragment getFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
    }

    private int getLayoutResId() {
        return R.layout.activity_fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        FragmentManager manager = getSupportFragmentManager();

        if (getFragment() == null) {
            manager.beginTransaction()
                    .add(R.id.fragmentContainer, createFragment())
                    .commit();
        }
    }
}