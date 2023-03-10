package com.glaciersecurity.glaciermessenger.services;

import android.content.Intent;
import androidx.core.content.ContextCompat;
import com.glaciersecurity.glaciermessenger.utils.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;

public class PushMessageReceiver extends FirebaseMessagingService {

	@Override
	public void onMessageReceived(RemoteMessage message) {
		if (!EventReceiver.hasEnabledAccounts(this)) {
			Log.d(Config.LOGTAG,"PushMessageReceiver ignored message because no accounts are enabled");
			return;
		}
		final Map<String, String> data = message.getData();
		final Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction(XmppConnectionService.ACTION_FCM_MESSAGE_RECEIVED);
		//maybe separate this if its a call and use ACTION_REPLY_TO_CALL_REQUEST //ALF AM-410
		intent.putExtra("account", data.get("account"));
		Compatibility.startService(this, intent);
	}

	@Override
	public void onNewToken(String token) {
		if (!EventReceiver.hasEnabledAccounts(this)) {
			Log.d(Config.LOGTAG,"PushMessageReceiver ignored new token because no accounts are enabled");
			return;
		}
		final Intent intent = new Intent(this, XmppConnectionService.class);
		intent.setAction(XmppConnectionService.ACTION_FCM_TOKEN_REFRESH);
		Compatibility.startService(this, intent);
	}

}
