package com.bandwidth.sample;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {
    public static final String TAG = "FirebaseHelper";
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    void fetchAndUpdateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (!TextUtils.isEmpty(token)) {
                Log.d(TAG, "retrieve token successful : " + token);
                updateToken(userId, token, _void -> Log.d(TAG, String.format("Token %s added for User: %s", token, userId)));
            } else {
                Log.w(TAG, "token should not be null...");
            }
        }).addOnFailureListener(e -> {
            //handle e
        }).addOnCanceledListener(() -> {
            //handle cancel
        }).addOnCompleteListener(task -> Log.v(TAG, "This is the token : " + task.getResult()));
    }

    private void updateToken(String userId, String token, OnSuccessListener<Void> listener) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("status", "Idle");
        dataMap.put("token", token);
        db.collection("agents").document(userId).set(dataMap, SetOptions.merge()).addOnSuccessListener(listener);
    }
}
