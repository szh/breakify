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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.heightechllc.breakify.preferences.ScheduledStartSettingsFragment;

import java.util.Calendar;

/**
 * The class that manages the Scheduled Start feature
 */
public class ScheduledStart {
    /**
     * The request code for the PendingIntent used to start the ScheduledStartReceiver
     */
    public static final int ALARM_MANAGER_REQUEST_CODE = 248;

    /**
     * Schedules the Scheduled Start, according to the current settings in
     *  {@link android.content.SharedPreferences}, or calls
     *  {@link #cancelScheduled(Context)} if it is disabled in settings
     */
    public static void schedule(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        // First check if scheduled start is enabled
        if (!prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_ENABLED, false)) {
            cancelScheduled(c);
            return;
        }

        // Check which days are enabled
        boolean sun = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_SUNDAY, false);
        boolean mon = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_MONDAY, false);
        boolean tue = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_TUESDAY, false);
        boolean wed = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_WEDNESDAY, false);
        boolean thu = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_THURSDAY, false);
        boolean fri = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_FRIDAY, false);
        boolean sat = prefs.getBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_SATURDAY, false);

        // Count the number of enabled days
        int numDays = 0;
        boolean[] daysArray = {sun, mon, tue, wed, thu, fri, sat};
        for (boolean dayEnabled : daysArray) {
            if (dayEnabled) numDays++;
        }

        // Make sure at least one day is enabled
        if (numDays == 0) {
            cancelScheduled(c);
            return;
        }

        // Get the time from preferences
        String timeStr = prefs.getString(ScheduledStartSettingsFragment.KEY_SCHEDULED_START_TIME, "");
        String[] split = timeStr.split(":");
        int hours = Integer.valueOf(split[0]);
        int minutes = Integer.valueOf(split[1]);

        // Calculate when the next time we should trigger the Scheduled Start is

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Check if we should set it for later today
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        // Check if today is disabled, or if `cal` is already past (i.e. we already past the
        //  scheduled time for today). Otherwise, we'll just leave `cal` as is, to schedule it for
        //  today. We ues `-1` with `daysArray` since arrays are 0-based and Calendar days aren't.
        if (!daysArray[today-1] || cal.before(Calendar.getInstance())) {

            // Set it for a future date
            int day = today;
            int i = 0;
            do {
                day++; // Go to the next day (start with tomorrow)
                if (day > 7) day = 1; // Reset to Sunday

                if (daysArray[day-1]) { // Check if the day is enabled
                    // Set the day
                    cal.set(Calendar.DAY_OF_WEEK, day);

                    if (cal.before(Calendar.getInstance())) {
                        // Means we set the DAY_OF_WEEK to the past, so set it for next week
                        cal.setLenient(true); // In case we roll over, e.g. to week 53
                        cal.add(Calendar.WEEK_OF_YEAR, 1);
                    }

                    break;
                }


                // Be extra sure this can never be an endless loop
                if (++i > 6) {
                    // At iteration 7, we're back to the day we started on
                    throw new RuntimeException(
                            "Endless loop: iterating through days of week more than once");
                }

            } while (true);
        }

        // Set the AlarmManager
        PendingIntent pi = PendingIntent.getBroadcast(c, ALARM_MANAGER_REQUEST_CODE,
                new Intent(c, ScheduledStartReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        // TODO: What if the time zone changes?
        alarmManager.set(AlarmManager.RTC, cal.getTimeInMillis(), pi);
    }

    /**
     * Cancels the Scheduled Start, if it was scheduled
     */
    public static void cancelScheduled(Context c) {
        PendingIntent pi = PendingIntent.getBroadcast(c, ALARM_MANAGER_REQUEST_CODE,
                new Intent(c, ScheduledStartReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
    }
}
