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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity displayed when the time is up. Plays ringtone and vibrates using AlarmRinger.
 */
public class RingingActivity extends Activity implements View.OnClickListener {
    public static int REQUEST_ALARM_RING = 0;
    public static final int RESULT_ALARM_RING_OK = RESULT_OK;
    public static final int RESULT_ALARM_RING_CANCEL = RESULT_CANCELED;
    public static final int RESULT_ALARM_RING_SNOOZE = RESULT_FIRST_USER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringing);

        // Set window flags
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                             WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                             WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Set label text
        TextView promptLbl = (TextView) findViewById(R.id.promptLbl);
        if (MainActivity.getWorkState() == MainActivity.WORK)
            promptLbl.setText(R.string.start_break_prompt);
        else
            promptLbl.setText(R.string.start_work_prompt);

        // Set button listeners
        Button okBtn = (Button) findViewById(R.id.okBtn);
        Button snoozeBtn = (Button) findViewById(R.id.snoozeBtn);
        Button cancelBtn = (Button) findViewById(R.id.cancelBtn);

        okBtn.setOnClickListener(this);
        snoozeBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);

        // Show ring notification (overwrites the persistent notification)
        AlarmNotifications.showRingNotification(this, MainActivity.getWorkState());

        // Check if a call is active or ringing
        TelephonyManager telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
        boolean inCall = (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE);

        // Check if the activity was created from the AlarmReceiver, i.e., from the AlarmManager
        //  going off, in which case we want to ring / vibrate to get the user's attention; or
        //  from the user clicking the notification, in which case we don't need to ring b/c the
        //  user already noticed it.
        // (We set `FLAG_ACTIVITY_NO_USER_ACTION` when opening from AlarmReceiver.)
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NO_USER_ACTION) != 0) {
            // Start ringing and / or vibrating (we do this here instead of in the broadcast
            //  receiver b/c we don't want the device to start ringing and vibrating before the
            //  activity shows up (in case there's a delay opening the activity, e.g. a slow device)
            AlarmRinger.start(this, inCall);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        // User did some kind of interaction to demonstrate that they noticed the alarm,
        //  so we can stop bothering them now (we want to be as gentle as possible while
        //  still ensuring the user is aware that the time is up)
        AlarmRinger.stop(this);
    }

    @Override
    public void onBackPressed() {
        // Override the device's back button to function like the "cancel" button
        Button cancelBtn = (Button) findViewById(R.id.cancelBtn);
        cancelBtn.performClick();
    }

    @Override
    public void onClick(View view) {
        int result;
        switch (view.getId()) {
            case R.id.okBtn:
                result = RESULT_ALARM_RING_OK;
                break;
            case R.id.snoozeBtn:
                result = RESULT_ALARM_RING_SNOOZE;
                break;
            case R.id.cancelBtn:
                result = RESULT_ALARM_RING_CANCEL;
                break;
            default: return;
        }

        // Remove the preferences that store when the alarm is scheduled to ring
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().remove("schedTotalTime").remove("schedRingTime").apply();

        // No need to stop alarm here, b/c it's stopped by onUserInteraction()

        // Hide the ring notification
        AlarmNotifications.hideNotification(this);

        // Return the result
        setResult(result);
        finish();
        // Use flip animation
        overridePendingTransition(R.anim.card_flip_in, R.anim.card_flip_out);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Stop the alarm immediately if the user presses any of the hardware keys.
        // Main use case is if the user forgets to turn off alarm, and it rings in a meeting. User
        //  should be able to mute instantly by pressing volume (or other) keys.
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_POWER:
                AlarmRinger.stop(this);
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}
