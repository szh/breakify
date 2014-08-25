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
import com.heightechllc.breakify.R;

/**
 * Fragment for user preferences relating to the alarm (volume, ringtone, vibration)
 */
public class AlarmSettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Settings keys
    public static final String KEY_RINGTONE = "pref_key_ringtone";
    public static final String KEY_VOLUME = "pref_key_volume";
    public static final String KEY_VIBRATE = "pref_key_vibrate";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from XML
        addPreferencesFromResource(R.xml.alarm_preferences);

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
    public void onStart() {
        super.onStart();

        updateRingtonePrefSummary();

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
        if (key.equals(KEY_RINGTONE)) {
            updateRingtonePrefSummary();
        }
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
}
