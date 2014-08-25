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
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.heightechllc.breakify.R;
import com.heightechllc.breakify.ScheduledStart;

/**
 * Fragment for the preferences regarding the Scheduled Start feature.
 * Subclass of {@link PreferenceFragment}.
 */
public class ScheduledStartSettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener
{

    // Settings keys
    public static final String KEY_CATEGORY_SCHEDULED = "pref_key_category_scheduled";
    public static final String KEY_SCHEDULED_ENABLED = "pref_key_scheduled_enabled";
    public static final String KEY_SCHEDULED_START_TIME = "pref_key_scheduled_start_time";
    public static final String KEY_SCHEDULED_DAYS_SUNDAY = "pref_key_scheduled_days_sunday";
    public static final String KEY_SCHEDULED_DAYS_MONDAY = "pref_key_scheduled_days_monday";
    public static final String KEY_SCHEDULED_DAYS_TUESDAY = "pref_key_scheduled_days_tuesday";
    public static final String KEY_SCHEDULED_DAYS_WEDNESDAY = "pref_key_scheduled_days_wednesday";
    public static final String KEY_SCHEDULED_DAYS_THURSDAY = "pref_key_scheduled_days_thursday";
    public static final String KEY_SCHEDULED_DAYS_FRIDAY = "pref_key_scheduled_days_friday";
    public static final String KEY_SCHEDULED_DAYS_SATURDAY = "pref_key_scheduled_days_saturday";

    /**
     * The summary to display in the PreferenceCategory that links to this fragment
     * @param c The context to use to retrieve the SharedPreferences
     */
    public static String getSummary(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        // First check if scheduled start is enabled
        if (!prefs.getBoolean(KEY_SCHEDULED_ENABLED, false)) {
            return c.getString(R.string.pref_scheduled_disabled);
        }

        // Check which days are enabled
        boolean sun = prefs.getBoolean(KEY_SCHEDULED_DAYS_SUNDAY, false);
        boolean mon = prefs.getBoolean(KEY_SCHEDULED_DAYS_MONDAY, false);
        boolean tue = prefs.getBoolean(KEY_SCHEDULED_DAYS_TUESDAY, false);
        boolean wed = prefs.getBoolean(KEY_SCHEDULED_DAYS_WEDNESDAY, false);
        boolean thu = prefs.getBoolean(KEY_SCHEDULED_DAYS_THURSDAY, false);
        boolean fri = prefs.getBoolean(KEY_SCHEDULED_DAYS_FRIDAY, false);
        boolean sat = prefs.getBoolean(KEY_SCHEDULED_DAYS_SATURDAY, false);

        // Count the number of enabled days
        int numDays = 0;
        boolean[] daysArray = {sun, mon, tue, wed, thu, fri, sat};
        for (boolean dayEnabled : daysArray) {
            if (dayEnabled) numDays++;
        }

        // Make sure at least one day is enabled
        if (numDays == 0) {
            return c.getString(R.string.pref_scheduled_disabled);
        }

        String daysStr = "";

        if (numDays == 7) {
            daysStr = c.getString(R.string.pref_scheduled_days_everyday);
        } else {
            // Use abbreviations of the days
            if (sun) daysStr += c.getString(R.string.pref_scheduled_days_sunday_initials) + " ";
            if (mon) daysStr += c.getString(R.string.pref_scheduled_days_monday_initials) + " ";
            if (tue) daysStr += c.getString(R.string.pref_scheduled_days_tuesday_initials) + " ";
            if (wed) daysStr += c.getString(R.string.pref_scheduled_days_wednesday_initials) + " ";
            if (thu) daysStr += c.getString(R.string.pref_scheduled_days_thursday_initials) + " ";
            if (fri) daysStr += c.getString(R.string.pref_scheduled_days_friday_initials) + " ";
            if (sat) daysStr += c.getString(R.string.pref_scheduled_days_saturday_initials) + " ";
            // Make the list of days readable
            daysStr = daysStr.replace(" ", ", ").trim(); // Remove space left at the end
        }

        // Return the days + time
        String timeStr = prefs.getString(KEY_SCHEDULED_START_TIME, "");
        return daysStr + " " + c.getString(R.string.pref_scheduled_at) + " " +
                TimePickerPreference.getHumanReadable(timeStr, c);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from XML
        addPreferencesFromResource(R.xml.scheduled_start_preferences);

        // So `onCreateOptionsMenu()` will be called
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Add the on / off switch to the action bar
        inflater.inflate(R.menu.switch_menu, menu);

        // Set up the action bar switch
        Switch abSwitch = (Switch) menu.findItem(R.id.item_switch)
                .getActionView().findViewById(R.id.action_bar_switch);

        // Restore the current state from preferences
        boolean enabled = getPreferenceScreen().getSharedPreferences()
                .getBoolean(KEY_SCHEDULED_ENABLED, false);

        abSwitch.setChecked(enabled);
        getPreferenceScreen().setEnabled(enabled);

        abSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                // Enable / disable the preferences in this fragment
                getPreferenceScreen().setEnabled(checked);

                // Save the user's choice to SharedPreferences
                getPreferenceScreen().getSharedPreferences().edit()
                        .putBoolean(KEY_SCHEDULED_ENABLED, checked).apply();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        updateStartTimePrefSummary();

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
        // Update the preference's summary
        if (key.equals(KEY_SCHEDULED_START_TIME))
            updateStartTimePrefSummary();

        // Update the scheduled start
        if (key.startsWith("pref_key_scheduled_"))
            ScheduledStart.schedule(getActivity());
    }

    /**
     * Updates the summary of the "Start Time" preference with the current value
     */
    private void updateStartTimePrefSummary() {
        String timeValue = getPreferenceScreen().getSharedPreferences()
                .getString(KEY_SCHEDULED_START_TIME, "");

        String summary = TimePickerPreference.getHumanReadable(timeValue, getActivity());
        findPreference(KEY_SCHEDULED_START_TIME).setSummary(summary);
    }
}
