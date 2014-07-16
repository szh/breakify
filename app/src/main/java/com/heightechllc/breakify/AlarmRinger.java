package com.heightechllc.breakify;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

/**
 * Handles ringing and vibrating
 * Portions of code borrowed from AlarmKlaxon from the Android clock app (deskclock),
 *  which is licensed under the Apache License, Version 2.0.
 */
public class AlarmRinger {
    private static final long[] vibratePattern = new long[] {500, 500};

    private static final float IN_CALL_VOLUME = 0.125f;

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

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.e("AlarmRinger", "Error while playing alarm. Stopping AlarmRinger");
                AlarmRinger.stop(context);
                return true;
            }
        });

        // If user is in a call, use a lower volume so we don't disrupt the call.
        if (inTelephoneCall)
            mediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);

        //TODO: Check which ringtone to use
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); // Default alarm ringtone
        try {
            mediaPlayer.setDataSource(context, alarmUri);
            startAlarm(context, mediaPlayer);
        } catch (IOException e) {
            cleanUpMediaPlayer(context);
            Log.e("AlarmRinger", "Failed to play ringtone:");
            e.printStackTrace();
        }

        //TODO: Check if vibration is enabled
        getVibrator(context).vibrate(vibratePattern, 0);

        started = true;
    }

    /**
     * Do the common stuff when starting the alarm.
     */
    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Don't play alarm if stream volume is 0 (typically because ringer mode is silent)
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) return;

        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        player.setLooping(true);
        player.prepare();
        audioManager.requestAudioFocus(null,
                AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
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
        mediaPlayer.release();
        mediaPlayer = null;
    }

}
