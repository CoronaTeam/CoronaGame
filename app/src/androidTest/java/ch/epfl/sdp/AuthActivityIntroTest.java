package ch.epfl.sdp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static ch.epfl.sdp.TestTools.initSafeTest;

@RunWith(AndroidJUnit4.class)
public class AuthActivityIntroTest {

    SharedPreferences.Editor preferencesEditor;

    @Rule
    public final ActivityTestRule<Authentication> mActivityRule =
            new ActivityTestRule<>(Authentication.class, true, false);

    @Before
    public void setup() {
        Context targetContext = getInstrumentation().getTargetContext();
        preferencesEditor = targetContext.getSharedPreferences(Authentication.APP_PREFERENCES, Context.MODE_PRIVATE).edit();
        initSafeTest(mActivityRule, false); //Intents.init();
    }


    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSkipsIntroWhenAlreadyOpenedOnce() {
        preferencesEditor.putBoolean(Authentication.OPENED_BEFORE_PREFERENCE, true);
        preferencesEditor.commit();

        mActivityRule.launchActivity(new Intent());
        intended(hasComponent(Authentication.class.getName()));
        assertNoUnverifiedIntents();
    }

    @Test
    public void testOpensIntroOnFirstTime() {
        preferencesEditor.putBoolean(Authentication.OPENED_BEFORE_PREFERENCE, false);
        preferencesEditor.commit();

        mActivityRule.launchActivity(new Intent());
        intended(hasComponent(IntroActivity.class.getName()));
    }

}