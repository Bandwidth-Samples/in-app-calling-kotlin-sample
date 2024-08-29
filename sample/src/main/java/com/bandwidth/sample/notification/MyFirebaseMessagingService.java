package com.bandwidth.sample.notification;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bandwidth.sample.R;
import com.bandwidth.sample.Util;
import com.bandwidth.sample.firebase.FirebaseHelper;
import com.bandwidth.sample.incoming_call.IncomingCallActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    FirebaseHelper firebaseHelper = new FirebaseHelper();

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d("MyFirebaseMessagingService", s);
        firebaseHelper.updateToken(getApplicationContext().getString(R.string.default_user_id), s, unused -> {
            Log.d("MyFirebaseMessagingService", "Token updated!");
        });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("Notification", remoteMessage.getData().toString());
        var data = remoteMessage.getData();
        if (!data.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClassName(getPackageName(), IncomingCallActivity.class.getName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Constants.KEY_EXTRA_DATA, Util.INSTANCE.mapToJson(data));
            startActivity(intent);
        }
    }
}