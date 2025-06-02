package com.bandwidth.sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";

    private static final int NOTIFICATION_ID = 271;
    public static final int NOTIFICATION_SERVICE_ID = 272;
    public static final int NOTIFICATION_CALL_ID = 273;

    private static String CHANNEL_NAME;
    private static String CHANNEL_ID;
    private static String CHANNEL_ID_CALL;
    private static String CHANNEL_NAME_CALL;

    private static NotificationManager notificationManager;

    public static Notification addServiceNotification(Context context) {

        String name = context.getString(R.string.app_name);
        CHANNEL_ID = name + "_id";

        NotificationCompat.Builder notificationCompat = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationCompat.setAutoCancel(true);

        notificationCompat.setSmallIcon(R.drawable.ic_launcher_foreground);

        Intent notifyIntent = new Intent(context, SampleActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent = PendingIntent.getActivity(context, NOTIFICATION_SERVICE_ID, notifyIntent, Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_IMMUTABLE);
        notificationCompat.setContentIntent(contentIntent);

        notificationCompat.setContentText(context.getString(R.string.notification_text_in_call));

        Notification notifcation = notificationCompat.build();
        getNotificationManager(context).notify(NOTIFICATION_SERVICE_ID, notifcation);
        return notifcation;
    }

    public static void removeServiceNotification(Context context) {
        getNotificationManager(context).cancel(NOTIFICATION_SERVICE_ID);
    }

    public static void removeAllNotifications(Context context) {
        getNotificationManager(context).cancel(NOTIFICATION_ID);
        removeServiceNotification(context);
    }

    private static NotificationManager getNotificationManager(Context context) {
        if (notificationManager == null) {
            String name = context.getString(R.string.app_name);
            CHANNEL_ID = name + "_id";
            CHANNEL_NAME = name + "_name";
            CHANNEL_ID_CALL = name + "_id_call";
            CHANNEL_NAME_CALL = name + "_name_call";
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, importance);
            notificationManager.createNotificationChannel(mChannel);
            mChannel = new NotificationChannel(
                    CHANNEL_ID_CALL, CHANNEL_NAME_CALL, NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            //mChannel.enableVibration(true);
            mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), aa);
            notificationManager.createNotificationChannel(mChannel);
        }
        return notificationManager;
    }

    public static void removeCallNotification(Context context) {
        Log.d(TAG, "removeCallNotification");
        getNotificationManager(context).cancel(NOTIFICATION_CALL_ID);
    }
}
