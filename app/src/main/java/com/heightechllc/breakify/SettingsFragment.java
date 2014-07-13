package com.heightechllc.breakify;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;


/**
 * Fragment for user preferences. Subclass of {@link PreferenceFragment}
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Settings keys
    public static final String KEY_WORK_DURATION = "pref_key_work_duration";
    public static final String KEY_BREAK_DURATION = "pref_key_break_duration";
    public static final String KEY_SNOOZE_DURATION = "pref_key_snooze_duration";

    /**
     * Use this factory method to create a new instance of this fragment.
     */
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Update the summaries for all of the preferences
        String[] keys = {KEY_WORK_DURATION, KEY_BREAK_DURATION, KEY_SNOOZE_DURATION};
        for (String key : keys)
            updatePrefSummary(key);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().
                registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

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
            updatePrefSummary(key);
        }
    }

    /**
     * Updates the summary of a preference with the current value, as recommended in the Android
     *  Design Guidelines, at http://developer.android.com/design/patterns/settings.html#writing
     * @param key The key of the preference to update
     */
    private void updatePrefSummary(String key) {
        // Get the preference's value, and add " minutes" to make it human-readable
        String summary = getPreferenceScreen().getSharedPreferences().getInt(key, 1)
                                    + " " + getString(R.string.minutes);
        // Set the preference's summary
        Preference pref = findPreference(key);
        pref.setSummary(summary);
    }
}
