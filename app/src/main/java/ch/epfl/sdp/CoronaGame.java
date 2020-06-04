package ch.epfl.sdp;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class CoronaGame extends Application implements Application.ActivityLifecycleCallbacks {

    public static final String SHARED_PREF_FILENAME = "coronagame_shared_pref";
    public static final DateFormat dateFormat = new SimpleDateFormat("E MMM dd hh:mm:ss zzz yyyy");
    public static boolean IS_DEMO = FALSE;
    public static boolean IS_ONLINE = TRUE;
    public static boolean IS_NETWORK_DEBUG = FALSE;
    private static Context context;
    private static int DEMO_SPEEDUP = 20;
    private Activity currentActivity;

    public static int getDemoSpeedup() {
        return DEMO_SPEEDUP;
    }

    public static void setDemoSpeedup(int demoSpeedup) {
        if (!IS_DEMO) {
            System.out.println("Switch to DEMO mode to change the execution speed");
            DEMO_SPEEDUP = 1;
        } else {
            DEMO_SPEEDUP = demoSpeedup;
        }
    }

    public static Context getContext() {
        return CoronaGame.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        CoronaGame.context = getApplicationContext();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(@NotNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NotNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NotNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NotNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NotNull Activity activity) {

    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
