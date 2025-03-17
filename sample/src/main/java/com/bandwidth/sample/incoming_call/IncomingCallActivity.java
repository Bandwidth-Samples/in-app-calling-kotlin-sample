package com.bandwidth.sample.incoming_call;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bandwidth.sample.R;
import com.bandwidth.sample.SampleActivity;
import com.bandwidth.sample.Util;
import com.bandwidth.sample.databinding.ActivityIncomingCallBinding;
import com.bandwidth.sample.firebase.FirebaseHelper;
import com.bandwidth.sample.incoming_call.model.IncomingPacketModel;
import com.bandwidth.sample.notification.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class IncomingCallActivity extends AppCompatActivity {
    public static final String TAG = "IncomingCallActivity";
    ActivityIncomingCallBinding incomingCallBinding;
    IncomingPacketModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        incomingCallBinding = ActivityIncomingCallBinding.inflate(getLayoutInflater());
        setContentView(incomingCallBinding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        fetchAndPopulate();
        incomingCallBinding.decline.setOnClickListener(view -> finish());
        incomingCallBinding.accept.setOnClickListener(view -> {
            if (model != null) {
                Intent intent = new Intent(IncomingCallActivity.this, SampleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("accountId", model.getAccountId());
                bundle.putString("applicationId", model.getApplicationId());
                bundle.putString("fromNo", model.getFromNo());
                bundle.putString("toNo", model.getToNo());
                bundle.putBoolean(Constants.IS_DIRECT_CALL, true);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
            }
        });
        new FirebaseHelper().updateStatus(
                Util.INSTANCE.getString("account.username", this),
                "Ringing",
                unused -> {
                    Log.d(this.getClass().getName(), "Status updated");
                }
        );
    }

    /*
    * "accountId": body.accountId,
            "applicationId": body.applicationId,
            "fromNo": body.from,
            "toNo": body.to,
            "token": authToken.data.token,
            "callerTn": authToken.data.callerTn,
            "tnToCall": authToken.data.tnToCall,
            "websocketAddr": authToken.data.websocketAddr*/

    void fetchAndPopulate() {
        String data = getIntent().getStringExtra(Constants.KEY_EXTRA_DATA);
        try {
            if (data != null) {
                Log.d(TAG, data);
                JSONObject jsonObject = new JSONObject(data);
                model = new IncomingPacketModel(
                        jsonObject.getString("accountId"),
                        jsonObject.getString("applicationId"),
                        jsonObject.getString("fromNo")
                                .replace("+", ""),
                        jsonObject.getString("toNo")
                                .replace("+", ""));
                incomingCallBinding.txtNumber.setText(model.getFromNo());
            }
        } catch (JSONException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }
}