package com.glaciersecurity.glaciermessenger.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.BackupAccountManager;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.glaciersecurity.glaciermessenger.utils.LogoutListener; //ALF AM-143

public class SettingsFragment extends PreferenceFragment {

	private String page = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		// Remove from standard preferences if the flag ONLY_INTERNAL_STORAGE is false
		if (!Config.ONLY_INTERNAL_STORAGE) {
			PreferenceCategory mCategory = (PreferenceCategory) findPreference("security_options");
			if (mCategory != null) {
				Preference cleanCache = findPreference("clean_cache");
				Preference cleanPrivateStorage = findPreference("clean_private_storage");
				mCategory.removePreference(cleanCache);
				mCategory.removePreference(cleanPrivateStorage);
			}
		}

		if (!TextUtils.isEmpty(page)) {
			openPreferenceScreen(page);
		}

		//ALF AM-143 // GOOBER
		Preference logoutButton = findPreference(getString(R.string.logout_button_key));
		logoutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showLogoutConfirmationDialog();
				return true;
			}
		});
	}

	public void setActivityIntent(final Intent intent) {
		boolean wasEmpty = TextUtils.isEmpty(page);
		if (intent != null) {
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				if (intent.getExtras() != null) {
					this.page = intent.getExtras().getString("page");
					if (wasEmpty) {
						openPreferenceScreen(page);
					}
				}
			}
		}
	}

	private void openPreferenceScreen(final String screenName) {
		final Preference pref = findPreference(screenName);
		if (pref instanceof PreferenceScreen) {
			final PreferenceScreen preferenceScreen = (PreferenceScreen) pref;
			getActivity().setTitle(preferenceScreen.getTitle());
			preferenceScreen.setDependency("");
			setPreferenceScreen((PreferenceScreen) pref);
		}
	}

	/**
	 * Display Logout confirmation
	 */
	private void showLogoutConfirmationDialog() {
		new AlertDialog.Builder(getActivity())
				.setTitle("Logout Confirmation")
				.setMessage(getString(R.string.account_logout_confirmation))
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						BackupAccountManager backupAccountManager = new BackupAccountManager(getActivity());

						// delete private configuration file
						if (backupAccountManager.deleteAccountFile(BackupAccountManager.LOCATION_PRIVATE, BackupAccountManager.APPTYPE_MESSENGER)) {
							Log.d("GOOBER", "Private Messenger configuration file successefully deleted.");
						} else {
							Log.d("GOOBER", "Failed to delete private Messenger configuration file.");
						}

						// delete private configuration file
						if (backupAccountManager.deleteAccountFile(BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_MESSENGER)) {
							Log.d("GOOBER", "Private Messenger configuration file successefully deleted.");
						} else {
							Log.d("GOOBER", "Failed to delete private Messenger configuration file.");
						}

						// delete public configuration file
						if (backupAccountManager.deleteAccountFile(BackupAccountManager.LOCATION_PUBLIC, BackupAccountManager.APPTYPE_VOICE)) {
							Log.d("GOOBER", "Public Voice configuration file successefully deleted.");
						} else {
							Log.d("GOOBER", "Failed to delete public Voice configuration file.");
						}

						LogoutListener activity = (LogoutListener) getActivity();
						activity.onLogout();
					}})
				.setNegativeButton(android.R.string.no, null).show();
	}
}
