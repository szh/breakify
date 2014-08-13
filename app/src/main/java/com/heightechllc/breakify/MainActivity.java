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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cocosw.undobar.UndoBarController;
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

    /**
     * Extra to instruct the Activity to open the RingingActivity. If FLAG_ACTIVITY_NO_USER_ACTION
     * is set on the Intent, the alarm will begin ringing as well.
     */
    public static final String EXTRA_ALARM_RING = "com.heightechllc.breakify.AlarmRing";
    /**
     * Extra to instruct the Activity to snooze the alarm. Add this when opening from the "Snooze"
     * action of the expanded notification.
     */
    public static final String EXTRA_SNOOZE = "com.heightechllc.breakify.Snooze";

    public static final int ALARM_MANAGER_REQUEST_CODE = 613;

    // Timer states
    public static final int RUNNING = 1;
    public static final int PAUSED = 2;
    public static final int STOPPED = 0;

    // Work states
    public static final int WORK = 1;
    public static final int BREAK = 2;

    private String tag = "MainActivity";

    private int timerState = STOPPED;
    private static int _workState = WORK;

    // For restoring when user presses "Undo" in the undo bar
    private long prevTotalTime, prevRemainingTime;
    private int prevWorkState;

    private SharedPreferences sharedPref;
    private AlarmManager alarmManager;

    // UI Components
    private CircleTimerView circleTimer;
    private TextView stateLbl, timeLbl, startStopLbl;
    private ImageButton resetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the default values for the preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, true);

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

        resetBtn = (ImageButton) findViewById(R.id.reset_btn);
        resetBtn.setOnClickListener(this);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Check if analytics are enabled in preferences
        if (sharedPref.getBoolean(SettingsFragment.KEY_ANALYTICS_ENABLED, false))
            mixpanel = MixpanelAPI.getInstance(this, "d78a075fc861c288e24664a8905a6698");

        // Handle the intent. Returns `true` if an action was taken, e.g. the RingingActivity was
        //  opened or the alarm was snoozed (see the 'extras' above), in which case we don't want
        //  to try to resume a saved alarm (since it already rang).
        if (!handleIntent(getIntent())) {

            // No action was taken on the intent, so check if an alarm is saved

            long totalTime = sharedPref.getLong("schedTotalTime", 0);
            if (totalTime < 1) return; // Means no alarm is saved

            // Get the scheduled ring time (which will only be set if the timer was running)
            // We need to convert from Unix / epoch time to elapsedRealtime
            // TODO: Move this conversion to a Utils class or helper method
            long ringUnixTime = sharedPref.getLong("schedRingTime", 0);
            long timeFromNow = ringUnixTime - System.currentTimeMillis();
            long scheduledRingTime = SystemClock.elapsedRealtime() + timeFromNow;
            // Get the saved time remaining for the paused timer (only set if the timer was paused)
            long pausedTimeRemaining = sharedPref.getLong("pausedTimeRemaining", 0);

            if (scheduledRingTime < 1 && pausedTimeRemaining < 1) return; // Defensive programming

            //TODO: What if scheduledRingTime is in the past?

            // Restore the work state
            setWorkState(sharedPref.getInt("workState", WORK));

            circleTimer.setTotalTime(totalTime);

            if (scheduledRingTime > 0) {
                // Restore running timer
                // Calculate how much time is left
                long remaining = scheduledRingTime - SystemClock.elapsedRealtime();
                // Update the time label
                circleTimer.updateTimeLbl(remaining);
                // Cause startTimer() to treat it like we're resuming (b/c we are)
                timerState = PAUSED;
                // Go!
                startTimer(remaining);

            } else {
                // Restore paused timer
                circleTimer.updateTimeLbl(pausedTimeRemaining);
                circleTimer.setPassedTime(totalTime - pausedTimeRemaining, true);
                // Set UI for paused state
                timerState = PAUSED;
                setUIForPausedState();
                resetBtn.setVisibility(View.VISIBLE);
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Send any unsent analytics events
        if (mixpanel != null) mixpanel.flush();

        // Enable or disable the boot receiver, which restores running alarms when the system boots.
        //  We only want it enabled if an alarm is scheduled.
        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        if (timerState == RUNNING) {
            getPackageManager().setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            getPackageManager().setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    /**
     * Handles a new intent, either from onNewIntent() or onCreate()
     * @param intent The intent to handle
     * @return Whether the intent was consumed (i.e. an action was taken, as instructed by an extra)
     */
    private boolean handleIntent(Intent intent) {
        boolean consumed = false;

        if (intent.getBooleanExtra(EXTRA_SNOOZE, false)) {
            // The activity was launched from the expanded notification's "Snooze" action

            snoozeTimer();
            AlarmNotifications.hideNotification(this);
            // In case user didn't interact with the RingingActivity, and instead snoozed directly
            //  from the notification
            AlarmRinger.stop(this);

            consumed = true;
        } else if (intent.getBooleanExtra(EXTRA_ALARM_RING, false)) {
            // The Activity was launched from AlarmReceiver, meaning the timer finished and we
            //  need to ring the alarm

            Intent ringingIntent = new Intent(this, RingingActivity.class);
            // Pass along FLAG_ACTIVITY_NO_USER_ACTION if it was set when calling this activity
            if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NO_USER_ACTION) != 0)
                ringingIntent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            startActivityForResult(ringingIntent, RingingActivity.REQUEST_ALARM_RING);

            consumed = true;
        }

        return consumed;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.circle_timer:
                if (timerState == RUNNING) pauseTimer();
                else startTimer();

                break;
            case R.id.reset_btn:
                cancelScheduledAlarm();

                resetTimerUI(false);

                // Analytics
                if (mixpanel != null) {
                    String eventName = getWorkState() == WORK ?
                            "Work timer reset" : "Break timer reset";
                    mixpanel.track(eventName, null);
                }

                break;
        }
    }

    /**
     * Called when we get a result from RingingActivity, meaning the user chose an action
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != RingingActivity.REQUEST_ALARM_RING)
            return; // We didn't request it and we don't know what to do with the result

        switch (resultCode) {
            case RingingActivity.RESULT_ALARM_RING_OK:
                switchWorkStates();
                break;
            case RingingActivity.RESULT_ALARM_RING_SNOOZE:
                snoozeTimer();
                break;
            case RingingActivity.RESULT_ALARM_RING_CANCEL:
                // User chose to cancel
                resetTimerUI(true);
                // Analytics
                if (mixpanel != null) {
                    // We want to have a separate event for when the user presses the "cancel" btn
                    //  in RingingActivity, vs. when they press the "reset" btn
                    String eventName = getWorkState() == WORK ?
                            "Work timer cancelled" : "Break timer cancelled";
                    mixpanel.track(eventName, null);
                }
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
            // Record the total duration, so we can resume if the activity is destroyed
            sharedPref.edit().putLong("schedTotalTime", duration).apply();
        }

        circleTimer.startIntervalAnimation();

        timerState = RUNNING;

        // Schedule the alarm to go off
        PendingIntent pi = PendingIntent.getBroadcast(this, ALARM_MANAGER_REQUEST_CODE,
                new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        long ringTime = SystemClock.elapsedRealtime() + duration;
        if (Build.VERSION.SDK_INT >= 19) {
            // API 19 needs setExact()
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ringTime, pi);
        }
        else {
            // APIs 1-18 use set()
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, ringTime, pi);
        }
        // Show the persistent notification
        AlarmNotifications.showUpcomingNotification(this, ringTime, getWorkState());

        // Record when the timer will ring and remove record of time remaining for the paused timer.
        // Save the scheduled ring time using Unix / epoch time, not elapsedRealtime, b/c
        //  that is reset on each boot.
        long timeFromNow = ringTime - SystemClock.elapsedRealtime();
        long ringUnixTime = System.currentTimeMillis() + timeFromNow;
        sharedPref.edit()
                .putLong("schedRingTime", ringUnixTime)
                .remove("pausedTimeRemaining")
                .apply();
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
                String eventName = getWorkState() == WORK ?
                        "Work timer resumed" : "Break timer resumed";
                mixpanel.track(eventName, null);
            }

        } else {
            // Get duration from preferences, in minutes
            if (getWorkState() == WORK) {
                duration = sharedPref.getInt(SettingsFragment.KEY_WORK_DURATION, 0);
            }
            else {
                duration = sharedPref.getInt(SettingsFragment.KEY_BREAK_DURATION, 0);
            }

            // Analytics
            if (mixpanel != null) {
                JSONObject props = new JSONObject();
                try {
                    props.put("Duration", duration);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String eventName = getWorkState() == WORK ?
                        "Work timer started" : "Break timer started";
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
        // Record the remaining time, so we can restore if the activity is destroyed
        sharedPref.edit().putLong("pausedTimeRemaining", circleTimer.getRemainingTime()).apply();

        cancelScheduledAlarm();
        circleTimer.pauseIntervalAnimation();

        timerState = PAUSED;

        setUIForPausedState();

        // Analytics
        if (mixpanel != null) {
            String eventName = getWorkState() == WORK ?
                    "Work timer paused" : "Break timer paused";
            mixpanel.track(eventName, null);
        }
    }

    /**
     * Sets the UI for the "paused" state
     */
    private void setUIForPausedState() {
        // Update the start / stop label
        startStopLbl.setText(R.string.resume);
        startStopLbl.setVisibility(View.VISIBLE);

        // Blink the time and state labels while paused
        Animation blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink);
        timeLbl.startAnimation(blinkAnim);
        stateLbl.startAnimation(blinkAnim);
    }

    /**
     * Snoozes the current timer for the duration stored in SharedPreferences
     *  (but show a toast if activity isn't open)
     */
    private void snoozeTimer() {
        // Get duration from preferences, in minutes
        int snoozeDuration = sharedPref.getInt(SettingsFragment.KEY_SNOOZE_DURATION, 0);
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
            String eventName = getWorkState() == WORK ?
                    "Work timer snoozed" : "Break timer snoozed";
            mixpanel.track(eventName, null);
        }
    }

    /**
     * Resets the CircleTimerView and reverts the Activity's UI to its initial state
     * @param isTimerComplete Whether the timer is complete
     */
    private void resetTimerUI(boolean isTimerComplete) {
        timerState = STOPPED;

        // Reset the UI
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();
        resetBtn.setVisibility(View.GONE);
        timeLbl.setText("");

        // Record the state we're about to reset from, in case the user chooses to undo
        if (isTimerComplete) {
            prevTotalTime = prevRemainingTime = 0;
        } else {
            prevTotalTime = circleTimer.getTotalTime();
            prevRemainingTime = circleTimer.getRemainingTime();
        }
        prevWorkState = getWorkState();

        // Back to initial state
        setWorkState(WORK);

        // Update the start / stop label
        startStopLbl.setText(R.string.start);
        startStopLbl.setVisibility(View.VISIBLE);

        circleTimer.stopIntervalAnimation();
        circleTimer.invalidate();

        // Remove record of total timer duration and the time remaining for the paused timer
        sharedPref.edit().remove("schedTotalTime").remove("pausedTimeRemaining").apply();

        // Create and show the undo bar
        new UndoBarController.UndoBar(this)
                .message(R.string.reset_toast)
                .duration(3500)
                .style(UndoBarController.UNDOSTYLE.setAnim( // Use base style with custom animations
                    AnimationUtils.loadAnimation(this, R.anim.undobar_fade_in),
                    AnimationUtils.loadAnimation(this, R.anim.undobar_fade_out)))
                .listener(new UndoBarController.UndoListener() {
                    @Override
                    public void onUndo(Parcelable parcelable) {
                        if (prevTotalTime > 0 && prevRemainingTime > 0) {
                            // Cause startTimer() to treat it like we're resuming (b/c we are)
                            timerState = PAUSED;
                            setWorkState(prevWorkState);
                            // Restore to the previous timer state, similar to how we restore a
                            //  running timer from SharedPreferences in onCreate()
                            circleTimer.setTotalTime(prevTotalTime);
                            circleTimer.updateTimeLbl(prevRemainingTime);
                            // Record the total duration, so we can resume if the activity is destroyed
                            sharedPref.edit().putLong("schedTotalTime", prevTotalTime).apply();
                            startTimer(prevRemainingTime);
                        } else {
                            // Means the timer was complete when resetTimerUI() was called, so we
                            //  need to start the timer from the beginning of the next state
                            if (prevWorkState == WORK) setWorkState(BREAK);
                            else setWorkState(WORK);
                            startTimer();
                        }
                        // Analytics
                        if (mixpanel != null) mixpanel.track("Timer reset undone", null);
                    }})
                .show();
    }

    /**
     * Switches the workState to the opposite of the current and then starts the timer
     */
    private void switchWorkStates() {
        // Set the new state
        if (getWorkState() == WORK) setWorkState(BREAK);
        else setWorkState(WORK);

        // Now start the timer
        startTimer();
    }

    /**
     * Set the timer's work state and update the state label. Not public or static like
     *  getWorkState(), since we only want methods in MainActivity to be able to change the state.
     */
    private void setWorkState(int newState) {
        _workState = newState;

        // Update the state label
        if (_workState == WORK) stateLbl.setText(R.string.state_working);
        else stateLbl.setText(R.string.state_breaking);

        // Save the work state to shared preferences
        sharedPref.edit().putInt("workState", _workState).apply();
    }

    /**
     * The current work state of the timer
     */
    public static int getWorkState() { return _workState; }

    /**
     * Cancels the alarm scheduled by startTimer()
     */
    private void cancelScheduledAlarm() {
        // Cancel the AlarmManager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, ALARM_MANAGER_REQUEST_CODE,
                    new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        // Hide the persistent notification
        AlarmNotifications.hideNotification(this);

        // Remove record of when the timer will ring
        sharedPref.edit().remove("schedRingTime").apply();
    }
}
