package com.heightechllc.breakify;


import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;


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

    public static final String KEY_RINGTONE = "pref_key_ringtone";
    public static final String KEY_VIBRATE = "pref_key_vibrate";

    public static final String KEY_ANALYTICS_ENABLED = "pref_key_analytics_enabled";

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
            updateDurationPrefSummary(key);

        updateRingtonePrefSummary();
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
            boolean analyticsEnabled = sharedPreferences.getBoolean(key,
                        getResources().getBoolean(R.bool.default_analytics_enabled));
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
        String summary = getPreferenceScreen().getSharedPreferences().getInt(key, 1)
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
        String selected = getPreferenceScreen().getSharedPreferences().getString(KEY_RINGTONE,
                Settings.System.DEFAULT_ALARM_ALERT_URI.toString());

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
