package com.heightechllc.breakify;


import android.os.Bundle;
import android.preference.PreferenceFragment;


/**
 * Fragment for user preferences. Subclass of {@link PreferenceFragment}
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SettingsFragment extends PreferenceFragment {

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
    }


}
