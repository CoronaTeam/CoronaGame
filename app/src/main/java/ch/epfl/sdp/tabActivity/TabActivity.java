package ch.epfl.sdp.tabActivity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.google.android.material.tabs.TabLayout;

import org.jetbrains.annotations.NotNull;

import ch.epfl.sdp.R;
import ch.epfl.sdp.contamination.fragment.StatusFragment;
import ch.epfl.sdp.identity.fragment.AccountFragment;
import ch.epfl.sdp.map.fragment.MapFragment;

/**
 * Contains fragments as Tabs, use special ViewPager to disable swipe motions
 */
public class TabActivity extends AppCompatActivity {

    private static final Tab[] tabs = new Tab[]{
            new Tab(MapFragment.class, R.string.tab_map, R.drawable.tab_map),
            new Tab(StatusFragment.class, R.string.tab_status, R.drawable.tab_status),
            new Tab(AccountFragment.class, R.string.tab_account, R.drawable.tab_account)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        SwipeViewPager viewPager = findViewById(R.id.pager);
        TabPagerAdapter tabPagerAdapter = new TabPagerAdapter(
                getSupportFragmentManager(),
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
                tabs);
        viewPager.setAdapter(tabPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < tabs.length; ++i) {
            tabLayout.getTabAt(i).setIcon(tabs[i].iconRes);
        }
        viewPager.setSwipeEnabled(false); // disable swipe behaviour
    }

    private static class Tab {
        final Class fragment;
        final int titleRes;
        final int iconRes;

        Tab(Class fragment, int titleRes, int iconRes) {
            this.fragment = fragment;
            this.titleRes = titleRes;
            this.iconRes = iconRes;
        }
    }

    class TabPagerAdapter extends FragmentPagerAdapter {
        private final Tab[] tabs;

        TabPagerAdapter(@NonNull FragmentManager fm, int behavior, Tab[] tabs) {
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

}
