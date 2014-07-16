package com.heightechllc.breakify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class RingingActivity extends Activity implements View.OnClickListener {

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
        int action;
        switch (view.getId()) {
            case R.id.okBtn:
                action = MainActivity.EXTRA_ALARM_RING_OK;
                break;
            case R.id.snoozeBtn:
                action = MainActivity.EXTRA_ALARM_RING_SNOOZE;
                break;
            case R.id.cancelBtn:
                // TODO: Ask user to confirm?
                action = MainActivity.EXTRA_ALARM_RING_CANCEL;
                break;
            default: return;
        }

        // Alarm is stopped in onUserInteraction()

        // Launch MainActivity and tell it what the user selected
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_ALARM_RING, action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
