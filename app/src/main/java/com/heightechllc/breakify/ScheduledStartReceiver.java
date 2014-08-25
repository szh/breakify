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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * The BroadcastReceiver that is notified when it's time for a scheduled start
 */
public class ScheduledStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the timer is already running
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPrefs.getLong("schedRingTime", 0) == 0) {

            // Open MainActivity and add the extra to tell it that this is a scheduled start
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.putExtra(MainActivity.EXTRA_SCHEDULED_START, true);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            context.startActivity(mainIntent);

            // TODO: Show notification
        }

        // Now reschedule for the next time
        ScheduledStart.schedule(context);
    }
}
