package com.glaciersecurity.glaciermessenger.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //String message = intent.getStringExtra("toastMessage");
        Toast.makeText(context, "toastMessage", Toast.LENGTH_SHORT).show();
    }
}
