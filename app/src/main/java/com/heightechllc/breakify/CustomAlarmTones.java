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

package com.heightechllc.breakify;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import com.heightechllc.breakify.preferences.SettingsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class for copying the custom alarm tones bundled with the app into the alarms directory on the
 * device's shared storage
 */
public class CustomAlarmTones {
    public static final String PREF_KEY_RINGTONE_DEFAULT = "pref_key_ringtone_default";
    public static final String PREF_KEY_RINGTONES_COPIED = "pref_key_ringtones_copied";

    private static final String tag = "CustomAlarmTones";

    private static Context sContext;

    /**
     * Copies the alarms to shared storage in a separate thread
     * @param context The context to use to retrieve the resources
     */
    public static void installToStorage(Context context) {
        sContext = context.getApplicationContext();
        new Thread(runnable).start();
    }

    private static Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (sContext == null) {
                Log.e(tag, "sContext must be set before calling run()");
                return;
            }

            boolean status1, status2;

            // Copy all of the files
            status1 = copyRawFile(R.raw.breakify_tone_1,
                    sContext.getString(R.string.alarmtone_title_tone1), true);
            status2 = copyRawFile(R.raw.breakify_tone_2,
                    sContext.getString(R.string.alarmtone_title_tone2), false);

            // If both files were copied successfully, record it in SharedPreferences
            if (status1 && status2) {
                SharedPreferences sharedPrefs =
                        PreferenceManager.getDefaultSharedPreferences(sContext);
                sharedPrefs.edit().putBoolean(PREF_KEY_RINGTONES_COPIED, true).apply();
            }

            // Clean up
            sContext = null;
        }

        /**
         * Copies a raw resource into the alarms directory on the device's shared storage
         * @param resID The resource ID of the raw resource to copy, in the form of R.raw.*
         * @param title The title to use for the alarm tone
         * @param setAsDefault Set the file as the default alarm tone for the app
         * @return Whether the file was copied successfully
         */
        private boolean copyRawFile(int resID, String title, boolean setAsDefault) {

            // Make sure the shared storage is currently writable
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                return false;

            File path = Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS);
            // Make sure the directory exists
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();
            String filename = sContext.getResources().getResourceEntryName(resID) + ".mp3";
            File outFile = new File(path, filename);

            String mimeType = "audio/mpeg";

            boolean isError = false;

            // Write the file
            InputStream inputStream = null;
            FileOutputStream outputStream = null;
            try {
                inputStream = sContext.getResources().openRawResource(resID);
                outputStream = new FileOutputStream(outFile);

                // Write in 1024-byte chunks
                byte[] buffer = new byte[1024];
                int bytesRead;
                // Keep writing until `inputStream.read()` returns -1, which means we reached the
                //  end of the stream
                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // Set the file metadata
                String outAbsPath = outFile.getAbsolutePath();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DATA, outAbsPath);
                contentValues.put(MediaStore.MediaColumns.TITLE, title);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                contentValues.put(MediaStore.Audio.Media.IS_ALARM, true);
                contentValues.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                contentValues.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                contentValues.put(MediaStore.Audio.Media.IS_MUSIC, false);

                Uri contentUri = MediaStore.Audio.Media.getContentUriForPath(outAbsPath);

                // If the ringtone already exists in the database, delete it first
                sContext.getContentResolver().delete(contentUri,
                        MediaStore.MediaColumns.DATA + "=\"" + outAbsPath + "\"", null);

                // Add the metadata to the file in the database
                Uri newUri = sContext.getContentResolver().insert(contentUri, contentValues);

                // Tell the media scanner about the new ringtone
                MediaScannerConnection.scanFile(
                        sContext,
                        new String[]{newUri.toString()},
                        new String[]{mimeType},
                        null
                );

                if (setAsDefault) {
                    SharedPreferences sharedPrefs =
                            PreferenceManager.getDefaultSharedPreferences(sContext);

                    // Save this to SharedPreferences, so SettingsFragment can use it as the default
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(PREF_KEY_RINGTONE_DEFAULT, newUri.toString());

                    // Check if the ringtone preference is currently set to "default"
                    if (sharedPrefs.getString(SettingsFragment.KEY_RINGTONE, "")
                            .equals(sContext.getString(R.string.default_ringtone_path))) {
                        // Set this as the ringtone, since it's the new default
                        editor.putString(SettingsFragment.KEY_RINGTONE, newUri.toString());
                    }

                    editor.apply();
                }

                Log.d(tag, "Copied alarm tone " + title + " to " + outAbsPath);
                Log.d(tag, "ID is " + newUri.toString());

            } catch (Exception e) {
                Log.e(tag, "Error writing " + filename, e);
                isError = true;
            } finally {
                // Close the streams
                try {
                    if (inputStream != null)
                        inputStream.close();
                    if (outputStream != null)
                        outputStream.close();
                } catch (IOException e) {
                    // Means there was an error trying to close the streams, so do nothing
                }
            }

            return !isError;
        }

    };

}
