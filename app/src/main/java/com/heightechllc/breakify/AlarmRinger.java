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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

/**
 * Handles ringing and vibrating
 * Portions of code borrowed from AlarmKlaxon from the Android clock app (deskclock),
 *  which is licensed under the Apache License, Version 2.0.
 */
public class AlarmRinger {
    private static String tag = "AlarmRinger";

    private static final float IN_CALL_VOLUME = 0.125f;
    private static final int STREAM_TYPE = AudioManager.STREAM_ALARM;

    private static boolean started;
    private static MediaPlayer mediaPlayer;

    public static void stop(Context context) {
        // Check if alarm is already stopped
        if (!started) return;

        started = false;

        // Stop ringing and clean up the media player
        cleanUpMediaPlayer(context);

        // Stop vibrating
        getVibrator(context).cancel();
    }

    public static void start(final Context context, boolean inTelephoneCall) {
        // Make sure we are stopped before starting
        stop(context);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Check which ringtone is set in preferences
        String alarmUriStr = sharedPrefs.getString(SettingsFragment.KEY_RINGTONE, "");
        // Check if the ringtone is "None" (an empty string), or if the volume is 0
        boolean ring = !alarmUriStr.isEmpty() && audioManager.getStreamVolume(STREAM_TYPE) > 0;
        // Check if vibration is enabled in preferences
        boolean vibrate = sharedPrefs.getBoolean(SettingsFragment.KEY_VIBRATE, false);

        if (ring || vibrate) started = true; // Only set `started` if we'll actually do something

        if (vibrate) // Start vibrating the device
            getVibrator(context).vibrate(new long[]{500, 500}, 0);

        if (!ring) return;

        // Set up the media player
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            // Handles asynchronous errors. Synchronous exceptions are handled by try-catch
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.e(tag, "Error while playing alarm");
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                return true;
            }
        });
        mediaPlayer.setAudioStreamType(STREAM_TYPE);
        mediaPlayer.setLooping(true);

        // If user is in a call, use a lower volume so we don't disrupt the call.
        if (inTelephoneCall)
            mediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);

        try {
            Uri alarmUri = Uri.parse(alarmUriStr);
            mediaPlayer.setDataSource(context, alarmUri);
            startAlarm(mediaPlayer, audioManager);
        } catch (IOException e) {
            try {
                Log.e(tag, "Failed to play selected ringtone. Trying to use default ringtone");
                e.printStackTrace();
                mediaPlayer.reset(); // Reset error state
                Uri defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mediaPlayer.setDataSource(context, defaultAlarmUri);
                startAlarm(mediaPlayer, audioManager);
            } catch (IOException e1) {
                cleanUpMediaPlayer(context);
                Log.e(tag, "Failed to play default ringtone:");
                e1.printStackTrace();
            }
        }
    }

    /**
     * Prepare the media player and play the ringtone
     */
    private static void startAlarm(MediaPlayer player, AudioManager audioManager) throws IOException {
        player.prepare();
        audioManager.requestAudioFocus(null, STREAM_TYPE, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        player.start();
    }

    /**
     * Get the Vibrator system service
     */
    private static Vibrator getVibrator(Context context) {
        // Use getApplicationContext(), b/c when we call vibrator.cancel() it will
        //  only stop if we used the same context as when we start it.
        return (Vibrator) context.getApplicationContext().
                    getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Clean up after the media player to release system resources
     */
    private static void cleanUpMediaPlayer(Context context) {
        // Stop ringing
        if (mediaPlayer == null) return;

        mediaPlayer.stop();
        AudioManager audioManager = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(null);
        mediaPlayer.reset(); // Fixes logcat warning "mediaplayer went away with unhandled events"
        mediaPlayer.release();
        mediaPlayer = null;
    }

}
