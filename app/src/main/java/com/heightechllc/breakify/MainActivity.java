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
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cocosw.undobar.UndoBarController;
import com.heightechllc.breakify.preferences.ScheduledStartSettingsFragment;
import com.heightechllc.breakify.preferences.SettingsActivity;
import com.heightechllc.breakify.preferences.SettingsFragment;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * The app's main activity. Controls the timer and main UI.
 * Displays the timer's progress using a {@link CircleTimerView}.
 *
 * For analytics, I'm trying out Mixpanel to see if they're any better than Google Analytics, et al.
 * Analytics can be disabled in the {@link com.heightechllc.breakify.preferences.SettingsActivity}
 *  for those who don't like their usage to be tracked.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    public static MixpanelAPI mixpanel;

    /**
     * Extra to inform the Activity that it is being opened automatically by ScheduledStartReceiver.
     * FLAG_ACTIVITY_NO_USER_ACTION should also be set on the intent when using this extra.
     */
    public static final String EXTRA_SCHEDULED_START = "com.heightechllc.breakify.ScheduledStart";
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

    /**
     * The request code for the PendingIntent to ring the timer
     */
    public static final int ALARM_MANAGER_REQUEST_CODE = 613;

    // Timer states
    public static final int TIMER_STATE_RUNNING = 1;
    public static final int TIMER_STATE_PAUSED = 2;
    public static final int TIMER_STATE_STOPPED = 0;

    // Work states
    public static final int WORK_STATE_WORKING = 1;
    public static final int WORK_STATE_BREAKING = 2;

    private final String tag = "MainActivity";

    private int timerState = TIMER_STATE_STOPPED;
    private int _workState = WORK_STATE_WORKING;

    private SharedPreferences sharedPref;
    private AlarmManager alarmManager;

    // UI Components
    private CircleTimerView circleTimer;
    private TextView stateLbl, timeLbl, startStopLbl;
    private ImageButton resetBtn;
    private Button skipBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the default values for the preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);

        setContentView(R.layout.activity_main);

        // If the user presses the device's volume keys, we want to adjust the alarm volume
        setVolumeControlStream(AlarmRinger.STREAM_TYPE);

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

        skipBtn = (Button) findViewById(R.id.skip_btn);
        skipBtn.setOnClickListener(this);

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
            restoreSavedTimer();
        }

        // Add the custom alarm tones to the phone's storage, if they weren't copied yet.
        // Works on a separate thread.
        if (!sharedPref.getBoolean(CustomAlarmTones.PREF_KEY_RINGTONES_COPIED, false))
            CustomAlarmTones.installToStorage(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Send any unsent analytics events
        if (mixpanel != null) mixpanel.flush();

        // Enable or disable the boot receiver, which restores running alarms when the system boots.
        //  We only want it enabled if an alarm is scheduled.
        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        if (timerState == TIMER_STATE_RUNNING) {
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.circle_timer:
                if (timerState == TIMER_STATE_RUNNING) pauseTimer();
                else startTimer();

                break;
            case R.id.reset_btn:
                cancelScheduledAlarm();

                resetTimerUI(false);

                // Analytics
                if (mixpanel != null) {
                    String eventName = getWorkState() == WORK_STATE_WORKING ?
                            "Work timer reset" : "Break timer reset";
                    mixpanel.track(eventName, null);
                }

                break;
            case R.id.skip_btn:
                skipToNextState();
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
                // Set the new state
                if (getWorkState() == WORK_STATE_WORKING) setWorkState(WORK_STATE_BREAKING);
                else setWorkState(WORK_STATE_WORKING);

                // Now start the timer
                startTimer();

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
                    String eventName = getWorkState() == WORK_STATE_WORKING ?
                            "Work timer cancelled" : "Break timer cancelled";
                    mixpanel.track(eventName, null);
                }
        }
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
        } else if (intent.getBooleanExtra(EXTRA_SCHEDULED_START, false)) {
            // The Activity was launched from ScheduledStartReceiver, meaning it's time for the
            //  scheduled start

            // Show a dialog prompting the user to start
            new AlertDialog.Builder(this)
                    .setTitle(R.string.scheduled_dialog_title)
                    .setMessage(R.string.scheduled_dialog_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Start the timer
                            circleTimer.performClick();
                        }
                    })
                    .setNeutralButton(R.string.action_settings,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // Show the settings activity with the Scheduled Start settings
                                    Intent intent = new Intent(MainActivity.this,
                                            SettingsActivity.class);
                                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                                            ScheduledStartSettingsFragment.class.getName());
                                    intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE,
                                            R.string.pref_category_scheduled);
                                    startActivity(intent);
                                }
                            })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            // We want to go ahead and restore the saved timer state, so we need to return `false`.
            // There won't be a running timer to restore, since ScheduledStartReceiver only opens
            //  the MainActivity if the timer isn't running, but there still may be a paused timer
            //  to restore.
            consumed = false;
        }

        return consumed;
    }

    /**
     * Attempts to restore the timer state from SharedPreferences
     * @return Whether the state was restored
     */
    private boolean restoreSavedTimer() {
        long totalTime = sharedPref.getLong("schedTotalTime", 0);
        if (totalTime < 1) return false; // Means no alarm is saved

        // Get the scheduled ring time (which will only be set if the timer was running)
        long ringUnixTime = sharedPref.getLong("schedRingTime", 0);
        // Get the saved time remaining for the paused timer (only set if the timer was paused)
        long pausedTimeRemaining = sharedPref.getLong("pausedTimeRemaining", 0);

        if (ringUnixTime < 1 && pausedTimeRemaining < 1) return false; // Defensive programming

        // Restore the work state
        setWorkState(sharedPref.getInt("workState", WORK_STATE_WORKING));

        circleTimer.setTotalTime(totalTime);

        if (ringUnixTime > 0) {
            // Attempt to restore running timer

            // Convert from Unix / epoch time to elapsedRealtime
            long timeFromNow = ringUnixTime - System.currentTimeMillis();

            // Check if the timer is scheduled to ring in the future or past
            if (timeFromNow > 0) {
                // Ring time is in the future
                // Update the time label
                circleTimer.updateTimeLbl(timeFromNow);
                // Cause startTimer() to treat it like we're resuming (b/c we are)
                timerState = TIMER_STATE_PAUSED;
                // Go!
                startTimer(timeFromNow);
            } else {
                // Time past! Ring the alarm.
                Intent ringingIntent = new Intent(this, RingingActivity.class);
                startActivityForResult(ringingIntent, RingingActivity.REQUEST_ALARM_RING);
            }
        } else {
            // Attempt to restore paused timer

            circleTimer.updateTimeLbl(pausedTimeRemaining);
            circleTimer.setPassedTime(totalTime - pausedTimeRemaining, true);
            // Set UI for paused state
            timerState = TIMER_STATE_PAUSED;
            setUIForPausedState();
            resetBtn.setVisibility(View.VISIBLE);
            skipBtn.setVisibility(View.VISIBLE);
        }

        return true;
    }

    /**
     * Starts the work or break timer
     * @param duration The number of milliseconds to run the timer for. If currently paused, this is the remaining time.
     */
    private void startTimer(long duration) {
        // Stop blinking the time and state labels
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();

        // Show the "Reset" and "Skip" btns
        resetBtn.setVisibility(View.VISIBLE);
        skipBtn.setVisibility(View.VISIBLE);

        // Update the start / stop label
        startStopLbl.setText(R.string.stop);
        startStopLbl.setVisibility(View.VISIBLE);

        if (timerState == TIMER_STATE_PAUSED && circleTimer.getTotalTime() > 0) {
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

        timerState = TIMER_STATE_RUNNING;

        // Schedule the alarm to go off
        PendingIntent pi = PendingIntent.getBroadcast(this, ALARM_MANAGER_REQUEST_CODE,
                new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        long ringTime = SystemClock.elapsedRealtime() + duration;
        if (Build.VERSION.SDK_INT >= 19) {
            // API 19 needs setExact()
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, ringTime, pi);
        } else {
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
        if (timerState == TIMER_STATE_PAUSED) {
            // Timer already started before, so just get the remaining time
            duration = circleTimer.getRemainingTime();

            // Analytics
            if (mixpanel != null) {
                String eventName = getWorkState() == WORK_STATE_WORKING ?
                        "Work timer resumed" : "Break timer resumed";
                mixpanel.track(eventName, null);
            }

        } else {
            // Get duration from preferences, in minutes
            if (getWorkState() == WORK_STATE_WORKING) {
                duration = sharedPref.getInt(SettingsFragment.KEY_WORK_DURATION, 0);
            } else {
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
                String eventName = getWorkState() == WORK_STATE_WORKING ?
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

        timerState = TIMER_STATE_PAUSED;

        setUIForPausedState();

        // Analytics
        if (mixpanel != null) {
            String eventName = getWorkState() == WORK_STATE_WORKING ?
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
        setWorkState(sharedPref.getInt("workState", WORK_STATE_WORKING)); // Restore the timer state
        // Get duration from preferences, in minutes
        int snoozeDuration = sharedPref.getInt(SettingsFragment.KEY_SNOOZE_DURATION, 0);
        // Snooze the timer. startTimer() also shows the upcoming notification, which will
        //  automatically hide the ringing notification, so we don't need to do it manually
        startTimer(snoozeDuration * 60000); // Multiply into milliseconds

        // Analytics
        if (mixpanel != null) {
            JSONObject props = new JSONObject();
            try {
                props.put("Duration", snoozeDuration);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String eventName = getWorkState() == WORK_STATE_WORKING ?
                    "Work timer snoozed" : "Break timer snoozed";
            mixpanel.track(eventName, null);
        }
    }

    /**
     * Skips to the next timer state
     */
    private void skipToNextState() {
        // Record the state we're about to skip from, in case the user chooses to undo
        Bundle undoStateBundle = new Bundle();
        undoStateBundle.putLong("totalTime", circleTimer.getTotalTime());
        undoStateBundle.putLong("remainingTime", circleTimer.getRemainingTime());
        undoStateBundle.putInt("workState", getWorkState());

        String toastMessage;

        // Get duration from preferences, in minutes
        long duration;
        if (getWorkState() == WORK_STATE_WORKING) {
            duration = sharedPref.getInt(SettingsFragment.KEY_WORK_DURATION, 0);
            toastMessage = "Work ";
        } else {
            duration = sharedPref.getInt(SettingsFragment.KEY_BREAK_DURATION, 0);
            toastMessage = "Break ";
        }

        // Set the new state
        if (getWorkState() == WORK_STATE_WORKING) setWorkState(WORK_STATE_BREAKING);
        else setWorkState(WORK_STATE_WORKING);

        startTimer(duration * 60000); // Multiply into milliseconds

        // Create and show the undo bar
        toastMessage += getString(R.string.skip_toast);
        showUndoBar(toastMessage, undoStateBundle, new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable parcelable) {
                // Extract the saved state from the Parcelable
                Bundle undoStateBundle = (Bundle) parcelable;
                long prevTotalTime = undoStateBundle.getLong("totalTime");
                long prevRemainingTime = undoStateBundle.getLong("remainingTime");
                int prevWorkState = undoStateBundle.getInt("workState");

                // Cause startTimer() to treat it like we're resuming (b/c we are)
                timerState = TIMER_STATE_PAUSED;
                setWorkState(prevWorkState);
                // Restore to the previous timer state, similar to how we restore a
                //  running timer from SharedPreferences in onCreate()
                circleTimer.setTotalTime(prevTotalTime);
                circleTimer.updateTimeLbl(prevRemainingTime);
                // Record the total duration, so we can resume if the activity is destroyed
                sharedPref.edit().putLong("schedTotalTime", prevTotalTime).apply();
                startTimer(prevRemainingTime);
                // Analytics
                if (mixpanel != null) mixpanel.track("Skip undone", null);
            }
        });

        // Analytics
        if (mixpanel != null) {
            JSONObject props = new JSONObject();
            try {
                props.put("Duration", duration);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String eventName = getWorkState() == WORK_STATE_WORKING ?
                    "Skipped to work" : "Skipped to break";
            mixpanel.track(eventName, props);
        }
    }

    /**
     * Resets the CircleTimerView and reverts the Activity's UI to its initial state
     * @param isTimerComplete Whether the timer is complete
     */
    private void resetTimerUI(boolean isTimerComplete) {
        timerState = TIMER_STATE_STOPPED;

        // Reset the UI
        timeLbl.clearAnimation();
        stateLbl.clearAnimation();
        resetBtn.setVisibility(View.GONE);
        skipBtn.setVisibility(View.GONE);
        timeLbl.setText("");

        // Record the state we're about to reset from, in case the user chooses to undo
        Bundle undoStateBundle = new Bundle();
        if (isTimerComplete) {
            undoStateBundle.putLong("totalTime", 0);
            undoStateBundle.putLong("remainingTime", 0);
        } else {
            undoStateBundle.putLong("totalTime", circleTimer.getTotalTime());
            undoStateBundle.putLong("remainingTime", circleTimer.getRemainingTime());
        }
        undoStateBundle.putInt("workState", getWorkState());

        // Back to initial state
        setWorkState(WORK_STATE_WORKING);

        // Update the start / stop label
        startStopLbl.setText(R.string.start);
        startStopLbl.setVisibility(View.VISIBLE);

        circleTimer.stopIntervalAnimation();
        circleTimer.invalidate();

        // Remove record of total timer duration and the time remaining for the paused timer
        sharedPref.edit().remove("schedTotalTime").remove("pausedTimeRemaining").apply();

        // Create and show the undo bar
        showUndoBar(getString(R.string.reset_toast), undoStateBundle, new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable parcelable) {
                // Extract the saved state from the Parcelable
                Bundle undoStateBundle = (Bundle) parcelable;
                long prevTotalTime = undoStateBundle.getLong("totalTime");
                long prevRemainingTime = undoStateBundle.getLong("remainingTime");
                int prevWorkState = undoStateBundle.getInt("workState");
                if (prevTotalTime > 0 && prevRemainingTime > 0) {
                    // Cause startTimer() to treat it like we're resuming (b/c we are)
                    timerState = TIMER_STATE_PAUSED;
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
                    if (prevWorkState == WORK_STATE_WORKING)
                        setWorkState(WORK_STATE_BREAKING);
                    else setWorkState(WORK_STATE_WORKING);
                    startTimer();
                }
                // Analytics
                if (mixpanel != null) mixpanel.track("Timer reset undone", null);
            }
        });
    }

    /**
     * Set the timer's work state and update the state label
     */
    private void setWorkState(int newState) {
        _workState = newState;

        // Update the state label
        if (_workState == WORK_STATE_WORKING) stateLbl.setText(R.string.state_working);
        else stateLbl.setText(R.string.state_breaking);

        // Save the work state to shared preferences
        sharedPref.edit().putInt("workState", _workState).apply();
    }

    /**
     * The current work state of the timer
     */
    private int getWorkState() { return _workState; }

    public static int getWorkState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt("workState", WORK_STATE_WORKING);
    }

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

    /**
     * Shows an UndoBar with a duration of 3500ms and a style with fade animations
     * @param message The message to display on the toast
     * @param token The Parcelable to be passed to the undo listener if the user pressed "Undo"
     * @param listener The UndoListener to be notified if the user presses "Undo"
     */
    private void showUndoBar(String message, Parcelable token,
                             UndoBarController.UndoListener listener) {
        new UndoBarController.UndoBar(this)
                .duration(3500)
                .style(UndoBarController.UNDOSTYLE.setAnim( // Use base style with custom animations
                        AnimationUtils.loadAnimation(this, R.anim.undobar_fade_in),
                        AnimationUtils.loadAnimation(this, R.anim.undobar_fade_out)))
                .message(message)
                .token(token)
                .listener(listener)
                .show();
    }
}
