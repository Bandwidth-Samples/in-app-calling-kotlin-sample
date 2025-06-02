package com.bandwidth.sample;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.media3.common.util.NonNullApi;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;


public class CallForegroundService extends MediaSessionService {


    private static final String TAG = "CallForeService";

    private static final String START_FOREGROUND = "START_FOREGROUND";
    private static final String STOP_FOREGROUND = "STOP_FOREGROUND";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @NonNullApi
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            Log.d(TAG, "Received intent: " + intent.getAction());
            String action = intent.getAction();
            if (START_FOREGROUND.equals(action)) {
                Log.d(TAG, "Received Start Foreground Intent ");
                Notification notification = NotificationUtils.addServiceNotification(getApplicationContext());
                startForeground(NotificationUtils.NOTIFICATION_SERVICE_ID, notification);

            } else if (STOP_FOREGROUND.equals(action)) {
                Log.d(TAG, "Received Stop Foreground Intent");
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        super.onBind(intent);
        return null;
    }

    public static void startService(Context context) {
        Log.d(TAG, "startService foreground ");
        Intent startIntent = new Intent(context, CallForegroundService.class);
        startIntent.setAction(CallForegroundService.START_FOREGROUND);
        context.startService(startIntent);
    }

    public static void stopService(Context context) {
        Log.d(TAG, "stopService foreground ");
        Intent stopIntent = new Intent(context, CallForegroundService.class);
        stopIntent.setAction(CallForegroundService.STOP_FOREGROUND);
        context.stopService(stopIntent);
        NotificationUtils.removeServiceNotification(context);
    }

}