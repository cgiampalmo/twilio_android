package com.glaciersecurity.glaciermessenger.ui;

import android.app.Dialog;
import android.view.View;

import com.glaciersecurity.glaciermessenger.entities.SmsProfile;

public interface OnSMSRemoveClickListener extends Dialog.OnClickListener {
    void OnSMSRemoveClick(SmsProfile selectedSMSforRelease);

}
