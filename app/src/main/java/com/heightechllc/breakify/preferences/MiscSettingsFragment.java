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
import android.preference.PreferenceFragment;

import com.heightechllc.breakify.MainActivity;
import com.heightechllc.breakify.R;

/**
 * Fragment for user preferences that don't belong to another category
 */
public class MiscSettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Settings key
    public static final String KEY_ANALYTICS_ENABLED = "pref_key_analytics_enabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from XML
        addPreferencesFromResource(R.xml.misc_preferences);
    }

    @Override
    public void onStart() {
        super.onStart();

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
        if (key.equals(KEY_ANALYTICS_ENABLED)) {
            // Check if the user disabled analytics
            boolean analyticsEnabled = sharedPreferences.getBoolean(key, false);
            if (MainActivity.mixpanel != null && !analyticsEnabled)
                MainActivity.mixpanel = null;

            // If the user enabled analytics, it will take effect in MainActivity.onCreate() on next
            //  launch. But if the user disabled it, we should respect that and stop immediately.
        }
    }
}
