package com.bandwidth.sample.incoming_call.model;


import android.text.TextUtils;

public class IncomingPacketModel {
    String accountId;
    String applicationId;
    String fromNo;
    String toNo;
    String token;

    public IncomingPacketModel(String accountId, String applicationId, String fromNo, String toNo, String token) {
        this.accountId = accountId;
        this.applicationId = applicationId;
        this.fromNo = fromNo;
        this.toNo = toNo;
        this.token = token;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getFromNo() {
        return fromNo;
    }

    public String getToNo() {
        return toNo;
    }

    public String getToken() {
        return token;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(accountId) &&
                !TextUtils.isEmpty(applicationId) &&
                !TextUtils.isEmpty(fromNo) &&
                !TextUtils.isEmpty(toNo) &&
                !TextUtils.isEmpty(token);
    }
}
