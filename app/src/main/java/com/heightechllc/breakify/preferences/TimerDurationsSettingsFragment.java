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

package com.heightechllc.breakify.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.heightechllc.breakify.R;

/**
 * Fragment for user preferences relating to the timer durations
 */
public class TimerDurationsSettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Settings keys
    public static final String KEY_WORK_DURATION = "pref_key_work_duration";
    public static final String KEY_BREAK_DURATION = "pref_key_break_duration";
    public static final String KEY_SNOOZE_DURATION = "pref_key_snooze_duration";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from XML
        addPreferencesFromResource(R.xml.timer_durations_preferences);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Update the summaries for all of the preferences
        String[] keys = {KEY_WORK_DURATION, KEY_BREAK_DURATION, KEY_SNOOZE_DURATION};
        for (String key : keys)
            updateDurationPrefSummary(key);

        getPreferenceScreen().getSharedPreferences().
                registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        getPreferenceScreen().getSharedPreferences().
                unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Check which preference changed
        if (key.equals(KEY_WORK_DURATION) ||
            key.equals(KEY_BREAK_DURATION) ||
            key.equals(KEY_SNOOZE_DURATION)) {
            // Refresh the preference that changed
            updateDurationPrefSummary(key);
        }
    }

    /**
     * Updates the summary of a duration preference with the current value, as per the Android
     *  Design Guidelines, at http://developer.android.com/design/patterns/settings.html#writing
     * @param key The key of the preference to update
     */
    private void updateDurationPrefSummary(String key) {
        // Get the preference's value, and add " minutes" to make it human-readable
        String summary = getPreferenceScreen().getSharedPreferences().getInt(key, 0)
                + " " + getString(R.string.minutes);
        // Set the preference's summary
        Preference pref = findPreference(key);
        pref.setSummary(summary);
    }
}
