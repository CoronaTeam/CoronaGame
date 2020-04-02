package ch.epfl.sdp;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import ch.epfl.sdp.Map.MapFragment;

public class TabActivity extends AppCompatActivity {

    public class TabPagerAdapter extends FragmentPagerAdapter {
        private final int NUM_ITEMS = 2;

        public TabPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new MapFragment();
                case 1:
                    return new HistoryFragment();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_map);
                case 1:
                    return getString(R.string.tab_history);
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        SwipeViewPager viewPager = findViewById(R.id.pager);
        TabPagerAdapter tabPagerAdapter = new TabPagerAdapter(
                getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(tabPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        viewPager.setSwipeEnabled(false); // disable swipe behaviour
    }

}
