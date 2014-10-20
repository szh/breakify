/**
 * Copyright (C) 2014  Shlomo Zalman Heigh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.heightechllc.breakify;

import android.annotation.TargetApi;
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
// `adb shell am broadcast -a "android.intent.action.BOOT_COMPLETED" -n com.heightechllc.breakify/.RescheduleReceiver`

/**
 * BroadcastReceiver for BOOT_COMPLETED, TIME_SET and TIMEZONE_CHANGED, to re-schedule the
 *  AlarmManagers from the values in SharedPreferences. (All alarms in AlarmManager are
 *  cancelled when the system shuts down.)
 */
public class RescheduleReceiver extends BroadcastReceiver {
    @TargetApi(19)
    @Override
    public void onReceive(Context context, Intent intent) {
        // Reschedule the Scheduled Start. We need to do this on all the Intent actions, since
        //  it must be scheduled for a specific time of day (e.g. 9AM), not based on elapsedRealtime
        //  (which isn't effected by time changes).
        ScheduledStart.schedule(context);

        //
        // Get the saved alarm from SharedPreferences, and schedule it with AlarmManager
        //

        // Only restore the saved timer on system boot
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) return;

        // Get the scheduled ring time (which will only be set if the timer was running)
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        long ringUnixTime = sharedPref.getLong("schedRingTime", 0);
        if (ringUnixTime < 1) return; // Means no alarm is saved
        // We need to convert from Unix / epoch time to elapsedRealtime
        long timeFromNow = ringUnixTime - System.currentTimeMillis();

        if (timeFromNow < 0) {
            // Time is already up, so ring the alarm immediately
            context.sendBroadcast(new Intent(context, AlarmReceiver.class));
        } else {
            // Construct a PendingIntent for the AlarmManager to run when the time is up
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    MainActivity.ALARM_MANAGER_REQUEST_CODE,
                    new Intent(context, AlarmReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            // Schedule the alarm to go off
            long scheduledRingTime = SystemClock.elapsedRealtime() + timeFromNow;
            AlarmManager alarmManager = (AlarmManager)
                    context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19) {
                // API 19 needs setExact()
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, scheduledRingTime, pi);
            } else {
                // APIs 1-18 use set()
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, scheduledRingTime, pi);
            }
            // Show the persistent notification
            AlarmNotifications.showUpcomingNotification(
                    context,
                    scheduledRingTime,
                    sharedPref.getInt("workState", MainActivity.WORK_STATE_WORKING)
            );
        }
    }
}
