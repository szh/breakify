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

package com.heightechllc.breakify.tests;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.heightechllc.breakify.AlarmNotifications;
import com.heightechllc.breakify.AlarmReceiver;
import com.heightechllc.breakify.CircleTimerView;
import com.heightechllc.breakify.MainActivity;
import com.heightechllc.breakify.R;

/**
 * Tests MainActivity
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mMainActivity;
    private CircleTimerView mCircleTimer;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Clear the app's shared preferences, and reset them to their defaults
        PreferenceManager.getDefaultSharedPreferences(getInstrumentation().getTargetContext())
                .edit().clear().commit();
        PreferenceManager.setDefaultValues(getInstrumentation().getTargetContext(),
                R.xml.timer_durations_preferences, true);
        PreferenceManager.setDefaultValues(getInstrumentation().getTargetContext(),
                R.xml.alarm_preferences, true);
        PreferenceManager.setDefaultValues(getInstrumentation().getTargetContext(),
                R.xml.scheduled_start_preferences, true);
        PreferenceManager.setDefaultValues(getInstrumentation().getTargetContext(),
                R.xml.misc_preferences, true);

        mMainActivity = getActivity();
        mCircleTimer = (CircleTimerView) mMainActivity.findViewById(R.id.circle_timer);
    }

    @Override
    protected void tearDown() throws Exception {
        // Cancel the AlarmManager
        Intent alarmIntent = new Intent(mMainActivity, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mMainActivity,
                MainActivity.ALARM_MANAGER_REQUEST_CODE,
                alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) mMainActivity.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        // Hide the notification
        AlarmNotifications.hideNotification(mMainActivity);

        super.tearDown();
    }

    // Test that getWorkState() is always accurate

    @SmallTest
    public void test_workState_startsAsWorking() {
        assertEquals(MainActivity.WORK_STATE_WORKING, MainActivity.getWorkState(mMainActivity));
    }

    @SmallTest
    public void test_workState_staysAccurate() {
        Button skipBtn = (Button) mMainActivity.findViewById(R.id.skip_btn);
        ImageButton resetBtn = (ImageButton) mMainActivity.findViewById(R.id.reset_btn);

        // Start the timer
        clickView(mCircleTimer);
        // Should still be "Working"
        assertEquals(MainActivity.WORK_STATE_WORKING, MainActivity.getWorkState(mMainActivity));
        // Skip to "Breaking"
        clickView(skipBtn);
        assertEquals(MainActivity.WORK_STATE_BREAKING, MainActivity.getWorkState(mMainActivity));
        // Reset timer
        clickView(resetBtn);
        assertEquals(MainActivity.WORK_STATE_WORKING, MainActivity.getWorkState(mMainActivity));
    }

    // Test that the start / stop label always gets updated

    @SmallTest
    public void test_startStopLbl_showsStartTextBeforeStart() {
        TextView startStopLbl = (TextView) mMainActivity.findViewById(R.id.start_stop_lbl);
        assertEquals(mMainActivity.getString(R.string.start), startStopLbl.getText());
    }

    @SmallTest
    public void test_startStopLbl_showsPauseTextWhileRunning() {
        TextView startStopLbl = (TextView) mMainActivity.findViewById(R.id.start_stop_lbl);
        // Start the timer
        clickView(mCircleTimer);

        assertEquals(mMainActivity.getString(R.string.stop), startStopLbl.getText());
    }

    @SmallTest
    public void test_startStopLbl_showsResumeTextWhilePaused() {
        TextView startStopLbl = (TextView) mMainActivity.findViewById(R.id.start_stop_lbl);
        // Start the timer
        clickView(mCircleTimer);
        // Pause the timer
        clickView(mCircleTimer);

        assertEquals(mMainActivity.getString(R.string.resume), startStopLbl.getText());
    }

    @SmallTest
    public void test_startStopLbl_showsStartTextWhenReset() {
        TextView startStopLbl = (TextView) mMainActivity.findViewById(R.id.start_stop_lbl);
        ImageButton resetBtn = (ImageButton) mMainActivity.findViewById(R.id.reset_btn);
        // Start the timer
        clickView(mCircleTimer);
        // Reset the timer
        clickView(resetBtn);

        assertEquals(mMainActivity.getString(R.string.start), startStopLbl.getText());
    }

    //
    // Helpers
    //

    /**
     * Perform a click on a view
     * @param v The view to click
     */
    private void clickView(View v) {

        // Based on `TouchUtils.clickView()`, but doesn't call `waitForIdleSync()`. See comment
        //  below for reason.

        // Get the location of the middle of the view
        int[] xy = new int[2];
        v.getLocationOnScreen(xy);

        float x = xy[0] + (v.getWidth() / 2.0f);
        float y = xy[1] + (v.getHeight() / 2.0f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        // Down
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        getInstrumentation().sendPointerSync(event);

        // The trick is *not* to call `getInstrumentation().waitForIdleSync()` here, since it
        //  makes the tests stop and hang, while the app keeps running (I'm not sure why - maybe
        //  because the CircleTimerView animates continuously with `onDraw()` and `invalidate()`).
        //  Instead just wait using `Thread.sleep()`.
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Up
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        getInstrumentation().sendPointerSync(event);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
