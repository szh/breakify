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

package com.heightechllc.breakify.tests;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;

import com.heightechllc.breakify.CustomAlarmTones;
import com.heightechllc.breakify.R;
import com.heightechllc.breakify.preferences.SettingsFragment;

import java.io.File;

/**
 * Tests CustomAlarmTones
 */
@LargeTest
public class CustomAlarmTonesTest extends AndroidTestCase {

    private File mFile1;
    private File mFile2;
    private SharedPreferences mSharedPrefs;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mSharedPrefs.edit().putBoolean(CustomAlarmTones.PREF_KEY_RINGTONES_COPIED, false).commit();

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
        String filename1 = getContext().getResources().getResourceEntryName(R.raw.breakify_tone_1)
                + ".mp3";
        String filename2 = getContext().getResources().getResourceEntryName(R.raw.breakify_tone_2)
                + ".mp3";

        mFile1 = new File(path, filename1);
        mFile2 = new File(path, filename2);

        // Delete the files if they already exist
        mFile1.delete();
        mFile2.delete();
    }

    public void test_copiesAlarmTones() {
        installAlarmTones();

        // Make sure the files exist and aren't blank
        assertTrue(mFile1.length() > 0);
        assertTrue(mFile2.length() > 0);

        // Make sure the shared prefs are set correctly
        assertTrue(mSharedPrefs.getBoolean(CustomAlarmTones.PREF_KEY_RINGTONES_COPIED, false));
        assertNotNull(mSharedPrefs.getString(CustomAlarmTones.PREF_KEY_RINGTONE_DEFAULT, null));
    }

    public void test_overridesDefaultAlarmTone() {
        // Set the ringtone preference to the default
        mSharedPrefs.edit().putString(SettingsFragment.KEY_RINGTONE,
                getContext().getString(R.string.default_ringtone_path)).commit();

        installAlarmTones();

        // Check if the pref was overridden
        assertEquals(mSharedPrefs.getString(CustomAlarmTones.PREF_KEY_RINGTONE_DEFAULT, ""),
                mSharedPrefs.getString(SettingsFragment.KEY_RINGTONE, ""));
    }

    public void test_doesNotOverrideNonDefaultAlarmTone() {
        // Set the ringtone preference to something other than the default
        mSharedPrefs.edit()
                .putString(SettingsFragment.KEY_RINGTONE, "something_other_than_default")
                .commit();

        installAlarmTones();

        // Make sure the ringtone pref wasn't changed
        assertEquals("something_other_than_default",
                mSharedPrefs.getString(SettingsFragment.KEY_RINGTONE, ""));
    }

    public void test_handlesFilesAlreadyExist() {
        // Run twice
        installAlarmTones();
        mSharedPrefs.edit().putBoolean(CustomAlarmTones.PREF_KEY_RINGTONES_COPIED, false).commit();
        installAlarmTones();

        assertTrue(mSharedPrefs.getBoolean(CustomAlarmTones.PREF_KEY_RINGTONES_COPIED, false));
    }

    //
    // Helpers
    //

    /**
     * Installs the alarm tones and waits for the thread to finish
     */
    private void installAlarmTones() {
        CustomAlarmTones.installToStorage(getContext());

        // Wait for the thread to complete
        // TODO: Is there a way to find when the thread finishes?
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
