package com.glaciersecurity.glaciermessenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;

public class EventReceiver extends BroadcastReceiver {

	public static final String SETTING_ENABLED_ACCOUNTS = "enabled_accounts";
	public static final String EXTRA_NEEDS_FOREGROUND_SERVICE = "needs_foreground_service"; //ALF AM-184

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent mIntentForService = new Intent(context, XmppConnectionService.class);
		if (intent.getAction() != null) {
			mIntentForService.setAction(intent.getAction());
		} else {
			mIntentForService.setAction("other");
		}
		final String action = intent.getAction();
		if (action.equals("ui") || hasEnabledAccounts(context)) {
			try {
				Compatibility.startService(context, mIntentForService);
			} catch (RuntimeException e) {
				Log.d(Config.LOGTAG,"EventReceiver was unable to start service");
			}
		} else {
			Log.d(Config.LOGTAG,"EventReceiver ignored action "+mIntentForService.getAction());
		}
	}

	public static boolean hasEnabledAccounts(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SETTING_ENABLED_ACCOUNTS,true);
	}

}
