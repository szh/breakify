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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.heightechllc.breakify.AlarmRinger;
import com.heightechllc.breakify.CustomAlarmTones;
import com.heightechllc.breakify.MainActivity;
import com.heightechllc.breakify.R;


/**
 * Fragment for user preferences. Subclass of {@link PreferenceFragment}.
 */
public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    // Settings keys
    public static final String KEY_WORK_DURATION = "pref_key_work_duration";
    public static final String KEY_BREAK_DURATION = "pref_key_break_duration";
    public static final String KEY_SNOOZE_DURATION = "pref_key_snooze_duration";

    public static final String KEY_RINGTONE = "pref_key_ringtone";
    public static final String KEY_VOLUME = "pref_key_volume";
    public static final String KEY_VIBRATE = "pref_key_vibrate";

    public static final String KEY_ANALYTICS_ENABLED = "pref_key_analytics_enabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from XML
        addPreferencesFromResource(R.xml.preferences);

        // Set up the "Volume" preference
        findPreference(KEY_VOLUME).setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Show the system volume control for the stream type set in AlarmRinger.STREAM_TYPE
                AudioManager audioManager = (AudioManager)
                        getActivity().getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AlarmRinger.STREAM_TYPE,
                        AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);

                return true;
            }
        });

        // Set the default ringtone, if one has been set by CustomAlarmTones
        String defaultRingtonePath = getPreferenceScreen().getSharedPreferences()
                .getString(CustomAlarmTones.PREF_KEY_RINGTONE_DEFAULT, "");
        if (!defaultRingtonePath.equals(""))
            findPreference(KEY_RINGTONE).setDefaultValue(defaultRingtonePath);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If the user presses the device's volume keys, we want to adjust the alarm volume
        getActivity().setVolumeControlStream(AlarmRinger.STREAM_TYPE);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Update the summaries for all of the preferences
        String[] keys = {KEY_WORK_DURATION, KEY_BREAK_DURATION, KEY_SNOOZE_DURATION};
        for (String key : keys)
            updateDurationPrefSummary(key);

        updateRingtonePrefSummary();

        getPreferenceScreen().getSharedPreferences().
                registerOnSharedPreferenceChangeListener(this);

        updateScheduledPrefSummary();
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
            key.equals(KEY_SNOOZE_DURATION))
        {
            // Refresh the preference that changed
            updateDurationPrefSummary(key);
        }
        else if (key.equals(KEY_RINGTONE)) {
            updateRingtonePrefSummary();
        }
        else if (key.equals(KEY_ANALYTICS_ENABLED)) {
            // Check if the user disabled analytics
            boolean analyticsEnabled = sharedPreferences.getBoolean(key, false);
            if (MainActivity.mixpanel != null && !analyticsEnabled)
                MainActivity.mixpanel = null;

            // If the user enabled analytics, it will take effect in MainActivity.onCreate() on next
            //  launch. But if the user disabled it, we should respect that and stop immediately.
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

    /**
     * Updates the summary of the "Ringtone" preference with the current value
     */
    private void updateRingtonePrefSummary() {
        // Get the URI of the selected ringtone
        String selected = getPreferenceScreen().getSharedPreferences().getString(KEY_RINGTONE, "");

        // Get the title of the selected ringtone
        String title;
        if (selected == null || selected.isEmpty()) {
            title = getString(R.string.ringtone_none);
        } else {
            Uri uri = Uri.parse(selected);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
            title = ringtone.getTitle(getActivity());
        }

        // Update the summary
        findPreference(KEY_RINGTONE).setSummary(title);
    }

    /**
     * Updates the summary of the "Scheduled Start" preference category
     */
    private void updateScheduledPrefSummary() {
        findPreference(ScheduledStartSettingsFragment.KEY_CATEGORY_SCHEDULED)
                .setSummary(ScheduledStartSettingsFragment.getSummary(getActivity()));
    }

}
