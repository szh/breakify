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

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;

import com.heightechllc.breakify.AlarmNotifications;
import com.heightechllc.breakify.AlarmRinger;
import com.heightechllc.breakify.MainActivity;
import com.heightechllc.breakify.R;
import com.heightechllc.breakify.RingingActivity;

/**
 * Tests RingingActivity
 */
@SmallTest
public class RingingActivityTest extends ActivityInstrumentationTestCase2<RingingActivity> {
    private RingingActivity mActivity;

    public RingingActivityTest() {
        super(RingingActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create a new intent with FLAG_ACTIVITY_NO_USER_ACTION set
        Intent intent = new Intent(getInstrumentation().getTargetContext(), RingingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        setActivityIntent(intent);
        mActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        AlarmRinger.stop(getInstrumentation().getTargetContext());
        // Hide the notification
        AlarmNotifications.hideNotification(getInstrumentation().getTargetContext());
    }


    // Test that the correct text is displayed based on the work state

    public void test_displaysAppropriateText() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getInstrumentation().getTargetContext());

        // Set the work state in Preferences to "Working"
        prefs.edit().putInt("workState", MainActivity.WORK_STATE_WORKING).apply();
        // Restart the activity
        mActivity.finish();
        setActivity(null);
        mActivity = getActivity();

        TextView prompt = (TextView) mActivity.findViewById(R.id.prompt_lbl);
        assertEquals(mActivity.getString(R.string.start_break_prompt), prompt.getText());

        // Now set the work state in Preferences to "Breaking"
        prefs.edit().putInt("workState", MainActivity.WORK_STATE_BREAKING).apply();
        // Restart the activity again
        mActivity.finish();
        setActivity(null);
        mActivity = getActivity();

        prompt = (TextView) mActivity.findViewById(R.id.prompt_lbl);
        assertEquals(mActivity.getString(R.string.start_work_prompt), prompt.getText());
    }


    // Test that the alarm rings / doesn't ring when appropriate

    public void test_ringsWhenNoUserActionIsSet() {
        // Make sure FLAG_ACTIVITY_NO_USER_ACTION is set
        assertTrue((mActivity.getIntent().getFlags() & Intent.FLAG_ACTIVITY_NO_USER_ACTION) != 0);

        // Make sure AlarmRinger is running
        assertTrue(AlarmRinger.isRinging());
    }

    public void test_doesNotRingWhenNoUserActionIsNotSet() {
        // Alarm will be ringing because the Activity was started by `setUp()` with
        //  FLAG_ACTIVITY_NO_USER_ACTION set, so stop it first.
        AlarmRinger.stop(getInstrumentation().getTargetContext());

        restartActivityWithUserAction();

        // First make sure FLAG_ACTIVITY_NO_USER_ACTION is not set
        assertTrue((mActivity.getIntent().getFlags() & Intent.FLAG_ACTIVITY_NO_USER_ACTION) == 0);

        // Make sure AlarmRinger is *not* running
        assertFalse(AlarmRinger.isRinging());
    }


    // Test that the alarm stops when a hardware key is pressed or the screen is clicked

    public void test_stopsRingingOnClick() {
        // First make sure AlarmRinger is running
        assertTrue(AlarmRinger.isRinging());
        // Click the main label
        TouchUtils.clickView(this, mActivity.findViewById(R.id.prompt_lbl));
        // Make sure it stopped ringing
        assertFalse(AlarmRinger.isRinging());
    }

    public void test_stopsRingingOnVolumeDownKeyPress() {
        assertTrue(AlarmRinger.isRinging());
        sendKeys(KeyEvent.KEYCODE_VOLUME_DOWN);
        assertFalse(AlarmRinger.isRinging());
    }
    public void test_stopsRingingOnVolumeUpKeyPress() {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP);
        assertFalse(AlarmRinger.isRinging());
    }
    public void test_stopsRingingOnVolumeMuteKeyPress() {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_MUTE);
        assertFalse(AlarmRinger.isRinging());
    }
    public void test_stopsRingingOnCameraKeyPress() {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_CAMERA);
        assertFalse(AlarmRinger.isRinging());
    }
    public void test_stopsRingingOnFocusKeyPress() {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_FOCUS);
        assertFalse(AlarmRinger.isRinging());
    }
    public void test_stopsRingingOnPowerKeyPress() {
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_POWER);
        assertFalse(AlarmRinger.isRinging());
    }
    
    
    // Test that the buttons work correctly

    public void test_okButtonCloseActivity() {
        Button okBtn = (Button) mActivity.findViewById(R.id.ok_btn);

        // OK btn
        TouchUtils.clickView(this, okBtn);
        assertTrue(mActivity.isFinishing());
    }
    public void test_snoozeButtonClosesActivity() {
        restartActivityWithUserAction();
        Button snoozeBtn = (Button) mActivity.findViewById(R.id.snooze_btn);

        TouchUtils.clickView(this, snoozeBtn);
        assertTrue(mActivity.isFinishing());
    }
    public void test_cancelButtonClosesActivity() {
        restartActivityWithUserAction();
        Button cancelBtn = (Button) mActivity.findViewById(R.id.cancel_btn);

        TouchUtils.clickView(this, cancelBtn);
        assertTrue(mActivity.isFinishing());
    }
    public void test_deviceBackButtonClosesActivity() {
        restartActivityWithUserAction();

        sendKeys(KeyEvent.KEYCODE_BACK);
        assertTrue(mActivity.isFinishing());
    }


    // Test that the preferences that store when the alarm is scheduled to ring get reset

    public void test_alarmPrefsGetReset() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                getInstrumentation().getTargetContext());

        // Put bogus data, just to test that it gets cleared
        prefs.edit().putLong("schedTotalTime", 100).putLong("schedRingTime", 100).apply();

        // Press the device back button to close the activity
        sendKeys(KeyEvent.KEYCODE_BACK);

        // Check that the prefs were cleared
        assertEquals(0, prefs.getLong("schedTotalTime", 0));
        assertEquals(0, prefs.getLong("schedRingTime", 0));
    }


    //
    // Helpers
    //

    /**
     * Restarts `mActivity` *without* FLAG_ACTIVITY_NO_USER_ACTION
     */
    private void restartActivityWithUserAction() {
        // Create a new intent *without* FLAG_ACTIVITY_NO_USER_ACTION
        Intent intent = new Intent(getInstrumentation().getTargetContext(), RingingActivity.class);
        setActivityIntent(intent);
        // Restart the activity
        mActivity.finish();
        setActivity(null);
        mActivity = getActivity();
    }
}
