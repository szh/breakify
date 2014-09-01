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

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.heightechllc.breakify.CircleTimerView;
import com.heightechllc.breakify.MainActivity;
import com.heightechllc.breakify.R;
import com.heightechllc.breakify.RescheduleReceiver;
import com.heightechllc.breakify.ScheduledStart;
import com.heightechllc.breakify.preferences.ScheduledStartSettingsFragment;

/**
 * Tests RescheduleReceiver
 */
@SmallTest
public class RescheduleReceiverTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Context context;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getTargetContext();
    }

    public RescheduleReceiverTest() {
        super(MainActivity.class);
    }

    // Test that the RescheduleReceiver is enabled / disabled in the appropriate situations

    public void test_enabledWhenScheduledStartIsEnabled() {
        enableScheduledStart();
        assertRescheduleReceiverState(true);
    }
    public void test_disabledWhenScheduledStartIsDisabledAndTimerIsNotRunning() {
        disableScheduledStart();
        assertRescheduleReceiverState(false);
    }
    @UiThreadTest
    public void test_enabledWhenTimerIsRunning() {
        // The RescheduleReceiver should be enabled even when Scheduled Start is disabled, if the
        //  timer is running
        disableScheduledStart();

        CircleTimerView timerView = (CircleTimerView) getActivity().findViewById(R.id.circle_timer);
        timerView.performClick();

        assertRescheduleReceiverState(true);
    }
    @UiThreadTest
    public void test_enabledWhenScheduledStartIsEnabledAndTimerIsRunning() {
        // This should be pretty clear if the others passed, but you never know what could go wrong
        enableScheduledStart();

        CircleTimerView timerView = (CircleTimerView) getActivity().findViewById(R.id.circle_timer);
        timerView.performClick();

        assertRescheduleReceiverState(true);
    }

    //
    // Helpers
    //

    /**
     * Asserts that the RescheduleReceiver is enabled / disabled
     * @param expectedEnabled True if it should be enabled, otherwise false
     */
    private void assertRescheduleReceiverState(boolean expectedEnabled) {
        ComponentName receiver = new ComponentName(context, RescheduleReceiver.class);

        // Get the PackageManager component enabled state based on the expectedEnabled parameter
        int expectedState = expectedEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                                              PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        assertEquals(expectedState,
                context.getPackageManager().getComponentEnabledSetting(receiver));
    }

    /**
     * Get the SharedPreferences for the target Context
     */
    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(
                getInstrumentation().getTargetContext());
    }

    /**
     * Enables Scheduled Start for all the days of the week
     */
    private void enableScheduledStart() {
        // Begin the editing
        SharedPreferences.Editor editor = getPrefs().edit();

        // Enable all the settings
        String[] keys = {
                ScheduledStartSettingsFragment.KEY_SCHEDULED_ENABLED,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_SUNDAY,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_MONDAY,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_TUESDAY,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_WEDNESDAY,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_THURSDAY,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_FRIDAY,
                ScheduledStartSettingsFragment.KEY_SCHEDULED_DAYS_SATURDAY
        };
        for (String key : keys) {
            editor.putBoolean(key, true);
        }

        // Commit the changes
        editor.apply();

        ScheduledStart.schedule(context);
    }

    /**
     * Disables Scheduled Start
     */
    private void disableScheduledStart() {
        getPrefs().edit()
                .putBoolean(ScheduledStartSettingsFragment.KEY_SCHEDULED_ENABLED, false)
                .apply();

        ScheduledStart.schedule(context);
    }

}
