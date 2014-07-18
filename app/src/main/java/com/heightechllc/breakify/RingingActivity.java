package com.heightechllc.breakify;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity displayed when the time is up. Plays ringtone and vibrates using AlarmRinger
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

        // TODO: Show notification

        // Check if a call is active or ringing
        TelephonyManager telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
        boolean inCall = (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE);
        // Start ringing and / or vibrating (we do this here instead of in the broadcast receiver
        //  since we don't want the device to start ringing and vibrating before the activity shows
        //  up (in case there's a delay in opening the activity, e.g. on a slow device)
        AlarmRinger.start(this, inCall);
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
                // TODO: Ask user to confirm?
                result = RESULT_ALARM_RING_CANCEL;
                break;
            default: return;
        }

        // No need to stop alarm here, b/c it's stopped by onUserInteraction()

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
