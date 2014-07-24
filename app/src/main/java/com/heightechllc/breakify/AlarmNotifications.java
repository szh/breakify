package com.heightechllc.breakify;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;

import java.util.Date;

/**
 * Handles showing notifications for upcoming and ringing alarms
 */
public class AlarmNotifications {
    private static final int notificationID = 0;

    /**
     * Hides any currently visible notification that was shown by this app. You don't need to call
     *  this before you show a different notification in this class, since all notifications here
     *  use the same id, so the new one will overwrite the old one.
     */
    public static void hideNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationID);
    }

    /**
     * Shows an ongoing notification for an upcoming alarm
     * @param context The context to create the notification from
     * @param ringTime The time that the alarm will ring, based on `SystemClock.elapsedRealtime()`
     */
    public static void showUpcomingNotification(Context context, long ringTime) {
        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification)
               .setOngoing(true)
               .setPriority(NotificationCompat.PRIORITY_LOW);

        // Get the appropriate title based on the current work state
        int titleId;
        if (MainActivity.getWorkState() == MainActivity.WORK)
            titleId = R.string.notif_upcoming_title_working;
        else
            titleId = R.string.notif_upcoming_title_breaking;

        builder.setContentTitle(context.getString(titleId));

        // Get formatted time for when the alarm will ring. We need to convert `ringTime`, which
        //  is based on `SystemClock.elapsedRealtime()`, to a regular Unix / epoch time
        long timeFromNow = ringTime - SystemClock.elapsedRealtime();
        long ringUnixTime = System.currentTimeMillis() + timeFromNow;
        // Construct the text, e.g., "Until 11:30"
        String contentText = context.getString(R.string.notif_upcoming_content_text) + " ";
        contentText += DateFormat.getTimeFormat(context).format(new Date(ringUnixTime));

        builder.setContentText(contentText);

        // Set up the action for the when the notification is clicked - to open MainActivity
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, mainIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pi);

        // Show the notification
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());
    }

    public static void showRingNotification(Context context) {
        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification)
               .setPriority(NotificationCompat.PRIORITY_MAX)
               .setContentTitle(context.getString(R.string.notif_ring_title))
               .setDefaults(NotificationCompat.DEFAULT_LIGHTS);

        // Get the appropriate text based on the current work state
        int textId;
        if (MainActivity.getWorkState() == MainActivity.WORK)
            textId = R.string.notif_ring_content_text_working;
        else
            textId = R.string.notif_ring_content_text_breaking;

        builder.setContentText(context.getString(textId));

        // Set up the action for when the notification is clicked - to open MainActivity, which
        //  will open RingingActivity
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(MainActivity.EXTRA_ALARM_RING, true);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pi);

        // TODO: Add "Snooze" action for expanded notification

        // Show the notification
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationID, builder.build());
    }
}