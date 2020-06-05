package ch.epfl.sdp.tabActivity;

import android.content.Context;
import android.util.AttributeSet;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SwipeViewPagerTest {

    private SwipeViewPager swipeViewPager;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        swipeViewPager = new SwipeViewPager(context, null);
    }

    @Test
    public void touchEventIsIgnored() {
        swipeViewPager.setSwipeEnabled(false);

        assertFalse(swipeViewPager.onInterceptTouchEvent(null));
    }

    @Test(expected = NullPointerException.class)
    public void touchEventIsPassed() {
        swipeViewPager.setSwipeEnabled(true);

        // throwing null demonstrates the event has been passed
        swipeViewPager.onInterceptTouchEvent(null);
    }

}