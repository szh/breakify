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

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.heightechllc.breakify.AlarmRinger;
import com.heightechllc.breakify.R;

import java.util.List;

/**
 * The activity that displays the user preferences
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the user presses the device's volume keys, we want to adjust the alarm volume
        setVolumeControlStream(AlarmRinger.STREAM_TYPE);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        // Check if the fragment matches any of the known PreferenceFragments in the app
        return fragmentName.equals(TimerDurationsSettingsFragment.class.getName()) ||
                fragmentName.equals(AlarmSettingsFragment.class.getName()) ||
                fragmentName.equals(ScheduledStartSettingsFragment.class.getName()) ||
                fragmentName.equals(MiscSettingsFragment.class.getName());
    }
}
