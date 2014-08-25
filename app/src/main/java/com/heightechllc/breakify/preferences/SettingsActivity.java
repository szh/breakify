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

import android.preference.PreferenceActivity;

import java.util.List;

/**
 * The activity that displays the user preferences
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);

        // This method only gets called when no sub-preference fragment will be displayed - i.e.
        //  when the Activity is first opened (unless a specific sub-preference fragment was
        //  specified in the Intent)

        // Show the initial SettingsFragment
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return fragmentName.equals(ScheduledStartSettingsFragment.class.getName());
    }
}
