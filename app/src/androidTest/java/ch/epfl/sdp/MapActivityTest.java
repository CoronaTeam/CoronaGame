package ch.epfl.sdp;

import android.app.Activity;

import androidx.lifecycle.Lifecycle;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.rule.ActivityTestRule;

import com.azimolabs.conditionwatcher.ConditionWatcher;
import com.azimolabs.conditionwatcher.Instruction;
import com.mapbox.mapboxsdk.maps.MapView;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

class LoadingDialogInstruction extends Instruction {

    private Boolean loaded = false;

    class bidule implements MapView.OnDidFinishLoadingMapListener {

        @Override
        public void onDidFinishLoadingMap() {
            loaded = true;
        }
    }

    @Override
    public String getDescription() {
        return "wait for map to load";
    }

    @Override
    public boolean checkCondition() {

        // Injecting the Instrumentation instance is required
        // for your test to run with AndroidJUnitRunner.
        Activity activity = ((CoronaGame)
                InstrumentationRegistry.getTargetContext().getApplicationContext()).getCurrentActivity();
        if (activity == null) return false;

        if (activity instanceof MapActivity){
            System.out.println("dldldldl");
            ((MapActivity)activity).OnDidFinishLoadingMapListener(new bidule());
            return loaded;
        }
        else{
            return false;
        }
    }
}


public class MapActivityTest {
    @Rule
    public final ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);
    @Test
    public void dummyTest() throws Exception{
        System.out.println("hello");
        //onView(withId(R.id.userIDText)).perform(typeText("from my unit test")).perform(closeSoftKeyboard());
        onView(withId(R.id.launchButton)).perform(click());
        ConditionWatcher.waitForCondition(new LoadingDialogInstruction());
        Espresso.pressBack();
        // onView(withId(R.id.greetingMessage)).check(matches(withText("Hello from my unit test!")));
    }
}
