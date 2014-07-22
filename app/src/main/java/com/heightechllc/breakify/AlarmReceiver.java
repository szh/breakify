package com.heightechllc.breakify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AlarmReceiver extends BroadcastReceiver {
    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Remove the preferences that store when the alarm is scheduled to ring
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.edit().remove("schedTotalTime").remove("schedRingTime").apply();

        // Open MainActivity and add the extra to tell it to ring the alarm
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(MainActivity.EXTRA_ALARM_RING, true);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        context.startActivity(mainIntent);
    }
}
