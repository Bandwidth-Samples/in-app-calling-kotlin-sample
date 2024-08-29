package com.bandwidth.sample.firebase;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.bandwidth.sample.Util;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {
    public static final String TAG = "FirebaseHelper";
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void fetchAndUpdateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (!TextUtils.isEmpty(token)) {
                Log.d(TAG, "retrieve token successful : " + token);
                updateTokenAndStatus(userId, token, _void -> Log.d(TAG, String.format("Token %s added for User: %s", token, userId)));
            } else {
                Log.w(TAG, "token should not be null...");
            }
        }).addOnFailureListener(Throwable::printStackTrace).addOnCanceledListener(() -> {
            Log.e(TAG, "Token fetch cancelled");
        });
    }

    private void updateTokenAndStatus(String userId, String token, OnSuccessListener<Void> listener) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("status", "Idle");
        dataMap.put("token", token);
        dataMap.put("device", getDeviceName());
        db.collection("agents").document(userId).set(dataMap, SetOptions.merge()).addOnSuccessListener(listener);
    }

    public void updateToken(String userId, String token, OnSuccessListener<Void> listener) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("token", token);
        dataMap.put("device", getDeviceName());
        db.collection("agents").document(userId).set(dataMap, SetOptions.merge()).addOnSuccessListener(listener);
    }

    public void updateStatus(String userId, String status, OnSuccessListener<Void> listener) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("status", status);
        db.collection("agents").document(userId).set(dataMap, SetOptions.merge()).addOnSuccessListener(listener);
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return Util.INSTANCE.capitalize(model);
        } else {
            return Util.INSTANCE.capitalize(manufacturer) + " " + model;
        }
    }
}
