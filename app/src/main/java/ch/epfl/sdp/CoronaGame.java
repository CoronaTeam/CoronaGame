package ch.epfl.sdp;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ch.epfl.sdp.location.LocationService;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class CoronaGame extends Application implements Application.ActivityLifecycleCallbacks {

    public static final String SHARED_PREF_FILENAME = "coronagame_shared_pref";
    public static final DateFormat dateFormat = new SimpleDateFormat("E MMM dd hh:mm:ss zzz yyyy");
    public static final String NOTIFICATION_CHANNEL_ID = "LOCATION_SERVICE_CHANNEL";
    public static final boolean IS_DEMO = FALSE;
    private static final String NOTIFICATION_CHANNEL_NAME = "SERVICE_NOTIFICATION";
    private static final String NOTIFICATION_CHANNEL_DESC = "Status of LocationService";
    public static boolean IS_ONLINE = TRUE;
    public static boolean IS_NETWORK_DEBUG = FALSE;
    private static Context context;
    private static int DEMO_SPEEDUP = 1;
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

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
        CoronaGame.context = getApplicationContext();

        // Init notification channel
        createNotificationChannel();

        startService(new Intent(this, LocationService.class));
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

}
