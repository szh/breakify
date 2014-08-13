package com.heightechllc.breakify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

// Note: This can be tested using:
// `adb shell am broadcast -a "android.intent.action.BOOT_COMPLETED" -n com.heightechllc.breakify/.BootReceiver`

/**
 * BroadcastReceiver for BOOT_COMPLETED, to re-schedule the AlarmManager if an alarm is saved in
 *  SharedPreferences, since all alarms in AlarmManager are cancelled when the system shuts down.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Only react to BOOT_COMPLETED
        if (!intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) return;

        //
        // Get the saved alarm from SharedPreferences, and schedule it with AlarmManager
        //

        // Get the scheduled ring time (which will only be set if the timer was running)
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        // We need to convert from Unix / epoch time to elapsedRealtime
        long ringUnixTime = sharedPref.getLong("schedRingTime", 0);
        long timeFromNow = ringUnixTime - System.currentTimeMillis();
        long scheduledRingTime = SystemClock.elapsedRealtime() + timeFromNow;

        if (scheduledRingTime < 1) return; // Means no running alarm is saved

        //TODO: What if scheduledRingTime is in the past?

        // Schedule the alarm to go off
        PendingIntent pi = PendingIntent.getBroadcast(
            context,
            MainActivity.ALARM_MANAGER_REQUEST_CODE,
            new Intent(context, AlarmReceiver.class),
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 19) {
            // API 19 needs setExact()
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, scheduledRingTime, pi);
        }
        else {
            // APIs 1-18 use set()
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, scheduledRingTime, pi);
        }
        // Show the persistent notification
        AlarmNotifications.showUpcomingNotification(context, scheduledRingTime);
    }
}
