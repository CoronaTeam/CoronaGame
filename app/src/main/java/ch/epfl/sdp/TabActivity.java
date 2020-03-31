package ch.epfl.sdp;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class TabActivity extends AppCompatActivity {

    private static class Tab {
        final Class fragment;
        final int titleRes;
        Tab(Class fragment, int titleRes) {
            this.fragment = fragment;
            this.titleRes = titleRes;
        }
    }

    private static final Tab[] tabs = new Tab[]{
            new Tab(MapFragment.class, R.string.tab_map),
            new Tab(HistoryFragment.class, R.string.tab_history)
    };

    public class TabPagerAdapter extends FragmentPagerAdapter {
        private final Tab[] tabs;

        public TabPagerAdapter(@NonNull FragmentManager fm, int behavior, Tab[] tabs) {
            super(fm, behavior);
            this.tabs = tabs;
        }

        @Override
        public int getCount() {
            return tabs.length;
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            try {
                return (Fragment) tabs[position].fragment.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(tabs[position].titleRes);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        ViewPager viewPager = findViewById(R.id.pager);
        TabPagerAdapter tabPagerAdapter = new TabPagerAdapter(
                getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
                tabs);
        viewPager.setAdapter(tabPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

}
