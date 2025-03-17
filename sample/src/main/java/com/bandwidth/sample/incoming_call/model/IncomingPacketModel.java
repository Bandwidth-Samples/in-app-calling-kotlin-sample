package com.bandwidth.sample.incoming_call.model;


import android.text.TextUtils;

public class IncomingPacketModel {
    String accountId;
    String applicationId;
    String fromNo;
    String toNo;

    public IncomingPacketModel(String accountId, String applicationId, String fromNo, String toNo) {
        this.accountId = accountId;
        this.applicationId = applicationId;
        this.fromNo = fromNo;
        this.toNo = toNo;
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
}
