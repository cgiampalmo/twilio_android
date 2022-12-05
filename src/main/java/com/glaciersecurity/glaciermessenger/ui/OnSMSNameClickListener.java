package com.glaciersecurity.glaciermessenger.ui;

import android.app.Dialog;

import com.glaciersecurity.glaciermessenger.entities.SmsProfile;

public interface OnSMSNameClickListener extends Dialog.OnClickListener {
    void OnSMSNameClick(String nickname, SmsProfile smsProfile);

}
