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

        // Check which ringtone to use
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        String alarmUriStr = sharedPrefs.getString(SettingsFragment.KEY_RINGTONE, "");

        // The URI Will be empty if set to "None". If it is, don't bother setting up a MediaPlayer
        if (!alarmUriStr.isEmpty()) {
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

            // If user is in a call, use a lower volume so we don't disrupt the call.
            if (inTelephoneCall)
                mediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);

            try {
                Uri alarmUri = Uri.parse(alarmUriStr);
                mediaPlayer.setDataSource(context, alarmUri);
                startAlarm(context, mediaPlayer);
            } catch (IOException e) {
                try {
                    Log.e(tag, "Failed to play selected ringtone. Trying to use default ringtone");
                    e.printStackTrace();
                    mediaPlayer.reset(); // Reset error state
                    Uri defaultAlarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    mediaPlayer.setDataSource(context, defaultAlarmUri);
                    startAlarm(context, mediaPlayer);
                } catch (IOException e1) {
                    cleanUpMediaPlayer(context);
                    Log.e(tag, "Failed to play default ringtone:");
                    e1.printStackTrace();
                }
            }
        }

        // Check if vibration is enabled
        boolean vibrate = sharedPrefs.getBoolean(SettingsFragment.KEY_VIBRATE, false);

        if (vibrate)
            getVibrator(context).vibrate(new long[]{500, 500}, 0);

        started = true;
    }

    /**
     * Do the common stuff when starting the alarm.
     */
    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Don't play alarm if stream volume is 0 (typically because ringer mode is silent)
        if (audioManager.getStreamVolume(STREAM_TYPE) == 0) {
            cleanUpMediaPlayer(context);
            return;
        }

        player.setAudioStreamType(STREAM_TYPE);
        player.setLooping(true);
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
