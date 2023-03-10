package com.glaciersecurity.glaciermessenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.glaciersecurity.glaciermessenger.utils.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;

public class MaintenanceReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(Config.LOGTAG, "received intent in maintenance receiver");
		if ("com.glaciersecurity.glaciermessenger.RENEW_INSTANCE_ID".equals(intent.getAction())) {
			renewInstanceToken(context);

		}
	}

	private void renewInstanceToken(final Context context) {
		new Thread(() -> {
			try {
				FirebaseInstanceId.getInstance().deleteInstanceId();
				final Intent intent = new Intent(context, XmppConnectionService.class);
				intent.setAction(XmppConnectionService.ACTION_FCM_TOKEN_REFRESH);
				Compatibility.startService(context, intent);
			} catch (IOException e) {
				Log.d(Config.LOGTAG, "unable to renew instance token", e);
			}
		}).start();

	}
}
