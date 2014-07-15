package com.heightechllc.breakify;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *  The app's main activity. Controls the timer and main UI.
 *
 *  The function of keeping the exact time that has elapsed in the timer is taken care of by
 *  the CircleTimerView, since it needs to refresh constantly anyway so it can animate. It also
 *  takes care of updating the `timeLbl` TextView to display the current time remaining.
 *
 *  The function of alerting when the time is up is handled by the AlarmManager below.
 *
 *  For analytics, I'm trying out Mixpanel to see if they're any better than Google Analytics, et al.
 *  They can be disabled in the SettingsActivity for those who don't like their usage to be tracked.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    public static MixpanelAPI mixpanel;

    public static final String EXTRA_ALARM_RING = "com.heightechllc.breakify.AlarmRing";
    public static final int EXTRA_ALARM_RING_OK = 1;
    public static final int EXTRA_ALARM_RING_SNOOZE = 2;
    public static final int  EXTRA_ALARM_RING_CANCEL = 3;

    private final int ALARM_MANAGER_REQUEST_CODE = 613;

    // Timer states
    public static final int RUNNING = 1;
    public static final int PAUSED = 2;
    public static final int STOPPED = 0;

    // Work states
    public static final int WORK = 1;
    public static final int BREAK = 2;

    private String tag = "MainActivity";

    private int timerState = STOPPED;
    private static int workState = WORK;

    SharedPreferences sharedPref;
    AlarmManager alarmManager;

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
        stateLbl = (TextView) findViewById(R.id.state_lbl);
        timeLbl = (TextView) findViewById(R.id.time_lbl);
        startStopLbl = (TextView) findViewById(R.id.start_stop_lbl);

        circleTimer = (CircleTimerView) findViewById(R.id.circle_timer);
        circleTimer.setOnClickListener(this);
        circleTimer.setTimeDisplay(timeLbl);

        resetBtn = (Button) findViewById(R.id.reset_btn);
        resetBtn.setOnClickListener(this);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Check if analytics are enabled in preferences
        if (sharedPref.getBoolean(SettingsFragment.KEY_ANALYTICS_ENABLED,
                getResources().getBoolean(R.bool.default_analytics_enabled)))
            mixpanel = MixpanelAPI.getInstance(this, "d78a075fc861c288e24664a8905a6698");

        // If the Activity is launched from RingingActivity, i.e. the timer finished and the user
        //  chose an action, the extra "EXTRA_ALARM_RING" will store the action to take.
        int action = getIntent().getIntExtra(EXTRA_ALARM_RING, -1);
        if (action > 0) {
            // We're done with the Extra now, so get rid of it, or it will stay even if
            //  onCreate() is called again when the user re-opens the app
            getIntent().removeExtra(EXTRA_ALARM_RING);
            handleAlarmFinished(action);
        } else {
            // Check if an alarm is already running
            if (isAlarmScheduled()) {
                long scheduledRingTime = sharedPref.getLong("schedRingTime", 0);
                timerState = PAUSED; // So startTimer() will treat it like we're resuming (b/c we are)
                // Get the total time, so we can correctly show progress in the CircleTimerView
                long totalTime = sharedPref.getLong("schedTotalTime", 0);
                circleTimer.setTotalTime(totalTime);
                // Calculate how much time is left
                long duration = scheduledRingTime - SystemClock.elapsedRealtime();
                // Update the time label and go go go!
                circleTimer.updateTimeLbl(duration);
                startTimer(duration);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Send any unsent analytics events
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
                cancelAlarmManager();

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
        // Stop blinking the time and state labels
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();

        // Show the "Reset" btn
        resetBtn.setVisibility(View.VISIBLE);

        // Update the start / stop label
        startStopLbl.setText(R.string.stop);
        startStopLbl.setVisibility(View.VISIBLE);

        if (timerState == PAUSED && circleTimer.getTotalTime() > 0) {
            // We're resuming from a paused state, so calculate how much time is remaining, based
            //  on the total time set in the circleTimer
            circleTimer.setPassedTime(circleTimer.getTotalTime() - duration, false);
        } else {
            circleTimer.setTotalTime(duration);
            circleTimer.setPassedTime(0, false);
            circleTimer.updateTimeLbl(duration);
            // Record the total time, so we can resume if the activity is destroyed
            sharedPref.edit().putLong("schedTotalTime", duration).apply();
        }

        // Schedule the alarm to go off
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ALARM_MANAGER_REQUEST_CODE,
                        new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
        scheduleAlarmManager(SystemClock.elapsedRealtime() + duration, pendingIntent);

        circleTimer.startIntervalAnimation();

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
            duration = circleTimer.getRemainingTime();

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
        // TODO: Implement saving and restoring paused timers between app runs

        cancelAlarmManager();
        circleTimer.pauseIntervalAnimation();

        timerState = PAUSED;

        // Update the start / stop label
        startStopLbl.setText(R.string.resume);
        startStopLbl.setVisibility(View.VISIBLE);

        // Blink the time and state labels while paused
        Animation blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink);
        timeLbl.startAnimation(blinkAnim);
        stateLbl.startAnimation(blinkAnim);

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
        timerState = STOPPED;

        // Reset the UI
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();
        resetBtn.setVisibility(View.GONE);
        timeLbl.setText("");

        // Back to initial state
        workState = WORK;
        stateLbl.setText(R.string.state_working);

        // Update the start / stop label
        startStopLbl.setText(R.string.start);
        startStopLbl.setVisibility(View.VISIBLE);

        circleTimer.stopIntervalAnimation();
        circleTimer.invalidate();
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

    public static int getWorkState() { return workState; }

    /**
     * Schedules the alarm to trigger at the specified time
     * @param time The time to go off at, using SystemClock.elapsedRealtime()
     * @param pi The broadcast receiver PendingIntent to trigger
     */
    private void scheduleAlarmManager(long time, PendingIntent pi)
    {
        if (Build.VERSION.SDK_INT >= 19) {
            // API 19 needs setExact()
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pi);
        }
        else {
            // APIs 1-18 use set()
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, time, pi);
        }

        // Record when the timer will ring
        sharedPref.edit().putLong("schedRingTime", time).apply();
    }

    /**
     * Cancels the alarm scheduled using scheduleAlarmManager()
     */
    private void cancelAlarmManager() {
        // Cancel the AlarmManager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ALARM_MANAGER_REQUEST_CODE,
                    new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.cancel(pendingIntent);

        // Remove record of when the time will ring
        sharedPref.edit().remove("schedRingTime").apply();
    }

    /**
     * Checks whether there is an alarm scheduled using scheduleAlarmManager()
     */
    private boolean isAlarmScheduled() {
        // We use FLAG_NO_CREATE to tell it not to create a new intent if one already exists.
        // So, if it comes back as `null`, that means one does already exist
        PendingIntent pi = PendingIntent.getBroadcast(this, ALARM_MANAGER_REQUEST_CODE,
                new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE);

        return pi != null;
    }

    /**
     * Called when the activity is launched from RingingActivity, meaning the alarm is finished
     * @param action The action sent by RingingActivity - EXTRA_ALARM_RING_OK, etc.
     */
    private void handleAlarmFinished(int action) {
        timerState = STOPPED;
        // Clear the time display
        timeLbl.setText("");
        // Hide the start / pause label
        startStopLbl.setVisibility(View.INVISIBLE);

        switch (action) {
            case EXTRA_ALARM_RING_OK:
                switchWorkStates();
                break;
            case EXTRA_ALARM_RING_SNOOZE:
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
                break;
            case EXTRA_ALARM_RING_CANCEL:
                // User chose to cancel
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
    }
}
