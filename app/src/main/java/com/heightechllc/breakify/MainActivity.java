package com.heightechllc.breakify;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements View.OnClickListener {
    public static MixpanelAPI mixpanel;

    // Timer states
    public static int RUNNING = 1;
    public static int PAUSED = 2;
    public static int STOPPED = 0;

    // Work states
    public static int WORK = 1;
    public static int BREAK = 2;

    private String tag = "MainActivity";

    private CountDownTimer _countDownTimer;
    private int timerState = STOPPED;
    private int workState = WORK;

    SharedPreferences sharedPref;

    // UI Components
    CircleTimerView circleTimer;
    TextView stateLbl;
    TextView timeLbl;
    TextView startStopLbl;
    Button resetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //
        // Set up components
        //
        circleTimer = (CircleTimerView) findViewById(R.id.circle_timer);
        circleTimer.setOnClickListener(this);

        stateLbl = (TextView) findViewById(R.id.state_lbl);
        timeLbl = (TextView) findViewById(R.id.time_lbl);
        startStopLbl = (TextView) findViewById(R.id.start_stop_lbl);

        resetBtn = (Button) findViewById(R.id.reset_btn);
        resetBtn.setOnClickListener(this);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if analytics are enabled in preferences
        if (sharedPref.getBoolean(SettingsFragment.KEY_ANALYTICS_ENABLED,
                getResources().getBoolean(R.bool.default_analytics_enabled)))
            mixpanel = MixpanelAPI.getInstance(this, "d78a075fc861c288e24664a8905a6698");
    }

    @Override
    protected void onDestroy() {
        if (mixpanel != null) mixpanel.flush();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Resume the timer's animation
        if (timerState == RUNNING)
            circleTimer.startIntervalAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Pause the timer's animation
        if (timerState == RUNNING)
            circleTimer.pauseIntervalAnimation();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.circle_timer:
                if (timerState == RUNNING) pauseTimer();
                else startTimer();

                break;
            case R.id.reset_btn:
                resetTimer();

                // Analytics
                if (mixpanel != null) {
                    String eventName = workState == WORK ?
                            "Work timer reset" : "Break timer reset";
                    mixpanel.track(eventName, null);
                }

                break;
        }
    }

    /**
     * Starts the work or break timer
     * @param duration The number of milliseconds to run the timer for
     */
    private void startTimer(long duration) {
        // Show the "Reset" btn
        resetBtn.setVisibility(View.VISIBLE);

        // We don't need to call updateTimeLbl() now, since it's called in _countDownTimer.onTick(),
        //  which is called for the first time immediately after starting it.

        /*
         *  Create the count down timer, which functions are:
         *  1) To notify when the time is up
         *  2) To notify every second to update the time display label
         *
         *  The function of keeping the exact time that has elapsed is taken care of by the
         *  CircleTimerView, since it needs to refresh constantly anyway so it can animate, and it
         *  would be silly to also require the CountDownTimer to tick often to record it as well.
         */
        _countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the UI
                updateTimeLbl(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timerState = STOPPED;
                // Update the time display
                updateTimeLbl(0);
                // Hide the start / pause btn
                startStopLbl.setVisibility(View.INVISIBLE);

                // Prompt to start break / resume work with dialog
                promptToSwitchWorkStates();
            }
        };

        // Update the start / stop label
        startStopLbl.setVisibility(View.VISIBLE);
        startStopLbl.setText(R.string.stop);

        if (circleTimer.getIntervalTime() > 0 && timerState == PAUSED) {
            // We're resuming from a paused state, so calculate how much time is remaining, based
            //  on the total time set in the circleTimer (the intervalTime)
            circleTimer.setPassedTime(circleTimer.getIntervalTime() - duration, false);
        } else {
            circleTimer.setIntervalTime(duration);
            circleTimer.setPassedTime(0, false);
        }

        circleTimer.startIntervalAnimation();

        _countDownTimer.start();
        timerState = RUNNING;
    }

    /**
     * Starts the work or break timer
     * Calculates the duration automatically, and then calls startTimer(duration)
     */
    private void startTimer() {
        long duration;
        if (timerState == PAUSED) {
            // Timer already started before, so just get the remaining time
            duration = circleTimer.getPassedTime();

            // Analytics
            if (mixpanel != null) {
                String eventName = workState == WORK ? "Work timer resumed" : "Break timer resumed";
                mixpanel.track(eventName, null);
            }

        } else {
            // Get duration from preferences, in minutes
            if (workState == WORK) {
                duration = sharedPref.getInt(SettingsFragment.KEY_WORK_DURATION,
                            getResources().getInteger(R.integer.default_work_duration));
            }
            else {
                duration = sharedPref.getInt(SettingsFragment.KEY_BREAK_DURATION,
                            getResources().getInteger(R.integer.default_break_duration));
            }

            // Analytics
            if (mixpanel != null) {
                JSONObject props = new JSONObject();
                try {
                    props.put("Duration", duration);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String eventName = workState == WORK ? "Work timer started" : "Break timer started";
                mixpanel.track(eventName, props);
            }

            duration *= 60000; // Multiply into milliseconds
        }

        startTimer(duration);
    }

    /**
     * Pauses the timer
     */
    private void pauseTimer() {
        _countDownTimer.cancel();
        timerState = PAUSED;

        // Update the start / stop label
        startStopLbl.setVisibility(View.VISIBLE);
        startStopLbl.setText(R.string.resume);

        // Blink the time and state labels while paused
        Animation blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink);
        timeLbl.startAnimation(blinkAnim);
        stateLbl.startAnimation(blinkAnim);

        circleTimer.pauseIntervalAnimation();

        // Analytics
        if (mixpanel != null) {
            String eventName = workState == WORK ? "Work timer paused" : "Break timer paused";
            mixpanel.track(eventName, null);
        }
    }

    /**
     * Resets the timer and reverts the UI to its initial state
     */
    private void resetTimer() {
        _countDownTimer.cancel();
        timerState = STOPPED;

        // Reset the UI
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();
        timeLbl.setText("");
        resetBtn.setVisibility(View.GONE);

        // Back to initial state
        workState = WORK;
        stateLbl.setText(R.string.state_working);

        // Update the start / stop label
        startStopLbl.setVisibility(View.VISIBLE);
        startStopLbl.setText(R.string.start);

        circleTimer.stopIntervalAnimation();
        circleTimer.invalidate();
    }

    /**
     * Updates the label that displays how much time is remaining
     * @param millis The The number of milliseconds remaining
     */
    private void updateTimeLbl(long millis) {
        // Stop blinking the time and state labels
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();

        // Get formatted time string
        String timeStr = formatTime(millis);

        // Update the clock
        timeLbl.setText(timeStr);
    }

    /**
     * Prompts the user to switch to the opposite work state (work or break)
     */
    private void promptToSwitchWorkStates() {
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        if (workState == WORK)
            builder.setMessage(R.string.start_break_prompt);
        else
            builder.setMessage(R.string.start_work_prompt);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switchWorkStates();
            }
        });
        builder.setNeutralButton(R.string.snooze, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Get duration from preferences, in minutes
                int snoozeDuration = sharedPref.getInt(SettingsFragment.KEY_SNOOZE_DURATION,
                                        getResources().getInteger(R.integer.default_work_duration));
                // Snooze the timer
                startTimer(snoozeDuration * 60000); // Multiply into milliseconds

                // Analytics
                if (mixpanel != null) {
                    JSONObject props = new JSONObject();
                    try {
                        props.put("Duration", snoozeDuration);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String eventName = workState == WORK ?
                                    "Work timer snoozed" : "Break timer snoozed";
                    mixpanel.track(eventName, null);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User chose to cancel (TODO: Ask user to confirm?)
                resetTimer();
                // Analytics
                if (mixpanel != null) {
                    // We want to have a separate event for when the user presses the "cancel" btn
                    //  in this dialog, vs. when they press the "reset" btn
                    String eventName = workState == WORK ?
                                    "Work timer cancelled" : "Break timer cancelled";
                    mixpanel.track(eventName, null);
                }
            }
        });

        builder.show();
    }

    /**
     * Switches the workState to the opposite of the current and then starts the timer
     */
    private void switchWorkStates() {
        // Set the new state
        if (workState == WORK) {
            workState = BREAK;
            stateLbl.setText(R.string.state_breaking);
        } else {
            workState = WORK;
            stateLbl.setText(R.string.state_working);
        }

        // Now start the timer
        startTimer();
    }

    //
    // HELPERS
    //

    /**
     * Creates a formatted string representing a time value, in the format Min:Sec, e.g. 06:13
     *
     * @param millis The number of milliseconds in the time
     * @return The formatted string
     */
    private String formatTime(long millis) {
        String timeStr = "";

        // Start with hours, if there are any
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        if (hours > 0)
        {
            timeStr = hours + ":";
            // Remove the milliseconds of the hours from `millis`, so they don't get counted as
            //  minutes too (we do similar thing in calculating seconds, see below)
            millis -= TimeUnit.HOURS.toMillis(hours);
        }

        // Now calculate the minutes and seconds
        // The '02' makes sure it's 2 digits
        timeStr += String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );

        return timeStr;
    }
}
