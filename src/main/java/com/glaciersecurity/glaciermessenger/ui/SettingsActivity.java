package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.preference.CheckBoxPreference;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.preference.SwitchPreference;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.crypto.OmemoSetting;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.services.ExportBackupService;
import com.glaciersecurity.glaciermessenger.services.MemorizingTrustManager;
import com.glaciersecurity.glaciermessenger.services.QuickConversationsService;
import com.glaciersecurity.glaciermessenger.ui.util.Color;
import com.glaciersecurity.glaciermessenger.utils.GeoHelper;
import com.glaciersecurity.glaciermessenger.utils.SystemSecurityInfo;
import com.glaciersecurity.glaciermessenger.utils.TimeFrameUtils;

public class SettingsActivity extends XmppActivity implements OnSharedPreferenceChangeListener{
	//OnSharedPreferenceChangeListener, LogoutListener {  //ALF AM-143 LogoutListener

	public static final String KEEP_FOREGROUND_SERVICE = "enable_foreground_service";
	public static final String AWAY_WHEN_SCREEN_IS_OFF = "away_when_screen_off";
	public static final String TREAT_VIBRATE_AS_SILENT = "treat_vibrate_as_silent";
	public static final String DND_ON_SILENT_MODE = "dnd_on_silent_mode";
	public static final String MANUALLY_CHANGE_PRESENCE = "manually_change_presence";
	public static final String BLIND_TRUST_BEFORE_VERIFICATION = "btbv";
	public static final String AUTOMATIC_MESSAGE_DELETION = "automatic_message_deletion";
	public static final String GLOBAL_MESSAGE_TIMER = "global_message_timer"; //ALF AM-53
	public static final String BROADCAST_LAST_ACTIVITY = "last_activity";
	public static final String THEME = "theme";
	public static final String SHOW_DYNAMIC_TAGS = "show_dynamic_tags";
	public static final String OMEMO_SETTING = "omemo";
	public static final String SCREEN_SECURITY = "screen_security"; //CMG AM-137
	public static final String USE_BIOMETRICS = "use_biometrics"; //AM#14 next 2
	public static final String USE_PIN_CODE = "use_pin_code";
//	public static final String DISPLAYNAME = "displayname"; //ALF AM-48

	public static final int REQUEST_CREATE_BACKUP = 0xbf8701;
	private SettingsFragment mSettingsFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		FragmentManager fm = getFragmentManager();
		mSettingsFragment = (SettingsFragment) fm.findFragmentById(R.id.settings_content);
		if (mSettingsFragment == null || !mSettingsFragment.getClass().equals(SettingsFragment.class)) {
			mSettingsFragment = new SettingsFragment();
			fm.beginTransaction().replace(R.id.settings_content, mSettingsFragment).commit();
		}
		mSettingsFragment.setActivityIntent(getIntent());
		this.mTheme = findTheme();
		setTheme(this.mTheme);
		getWindow().getDecorView().setBackgroundColor(Color.get(this, R.attr.color_background_primary));
		setSupportActionBar(findViewById(R.id.toolbar));
		configureActionBar(getSupportActionBar());
	}

	@Override
	void onBackendConnected() {
		//CMG AM-223
//		final EditTextPreference displayNamePreference = (EditTextPreference)mSettingsFragment.findPreference("displayname");
//		if (displayNamePreference != null && xmppConnectionService != null) {
//			String disname = null;
//			if (xmppConnectionService.getAccounts() != null){
//				disname = xmppConnectionService.getAccounts().get(0).getDisplayName();
//				if (disname == null) {
//					disname = xmppConnectionService.getAccounts().get(0).getUsername();
//				}
//				displayNamePreference.setText(disname);
//			}
//		}
	}

	@Override
	public void onStart() {
		super.onStart();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		changeOmemoSettingSummary();

		if (QuickConversationsService.isQuicksy()) {
			PreferenceCategory connectionOptions = (PreferenceCategory) mSettingsFragment.findPreference("connection_options");
			PreferenceScreen expert = (PreferenceScreen) mSettingsFragment.findPreference("expert");
			if (connectionOptions != null) {
				expert.removePreference(connectionOptions);
			}
		}

		PreferenceScreen mainPreferenceScreen = (PreferenceScreen) mSettingsFragment.findPreference("main_screen");

		PreferenceCategory attachmentsCategory = (PreferenceCategory) mSettingsFragment.findPreference("attachments");
		CheckBoxPreference locationPlugin = (CheckBoxPreference) mSettingsFragment.findPreference("use_share_location_plugin");
		if (attachmentsCategory != null && locationPlugin != null) {
			if (!GeoHelper.isLocationPluginInstalled(this)) {
				attachmentsCategory.removePreference(locationPlugin);
			}
		}


		//this feature is only available on Huawei Android 6.
		PreferenceScreen huaweiPreferenceScreen = (PreferenceScreen) mSettingsFragment.findPreference("huawei");
		if (huaweiPreferenceScreen != null) {
			Intent intent = huaweiPreferenceScreen.getIntent();
			PreferenceCategory generalCategory = (PreferenceCategory) mSettingsFragment.findPreference("general");
			generalCategory.removePreference(huaweiPreferenceScreen);
			if (generalCategory.getPreferenceCount() == 0) {
				if (mainPreferenceScreen != null) {
					mainPreferenceScreen.removePreference(generalCategory);
				}
			}
		}

		ListPreference automaticMessageDeletionList = (ListPreference) mSettingsFragment.findPreference(AUTOMATIC_MESSAGE_DELETION);
		if (automaticMessageDeletionList != null) {
			final int[] choices = getResources().getIntArray(R.array.automatic_message_deletion_values);
			CharSequence[] entries = new CharSequence[choices.length];
			CharSequence[] entryValues = new CharSequence[choices.length];
			for (int i = 0; i < choices.length; ++i) {
				entryValues[i] = String.valueOf(choices[i]);
				if (choices[i] == 0) {
					entries[i] = getString(R.string.never);
				} else {
					entries[i] = TimeFrameUtils.resolve(this, 1000L * choices[i]);
				}
			}
			automaticMessageDeletionList.setEntries(entries);
			automaticMessageDeletionList.setEntryValues(entryValues);
		}


		boolean removeLocation = new Intent("com.glaciersecurity.glaciermessenger.location.request").resolveActivity(getPackageManager()) == null;
		boolean removeVoice = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION).resolveActivity(getPackageManager()) == null;

		//AM-583
//		ListPreference quickAction = (ListPreference) mSettingsFragment.findPreference("quick_action");
//		if (quickAction != null && (removeLocation || removeVoice)) {
//			ArrayList<CharSequence> entries = new ArrayList<>(Arrays.asList(quickAction.getEntries()));
//			ArrayList<CharSequence> entryValues = new ArrayList<>(Arrays.asList(quickAction.getEntryValues()));
//			int index = entryValues.indexOf("location");
//			if (index > 0 && removeLocation) {
//				entries.remove(index);
//				entryValues.remove(index);
//			}
//			index = entryValues.indexOf("voice");
//			if (index > 0 && removeVoice) {
//				entries.remove(index);
//				entryValues.remove(index);
//			}
//			quickAction.setEntries(entries.toArray(new CharSequence[entries.size()]));
//			quickAction.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
//		}

		final Preference removeCertsPreference = mSettingsFragment.findPreference("remove_trusted_certificates");
		if (removeCertsPreference != null) {
			removeCertsPreference.setOnPreferenceClickListener(preference -> {
				final MemorizingTrustManager mtm = xmppConnectionService.getMemorizingTrustManager();
				final ArrayList<String> aliases = Collections.list(mtm.getCertificates());
				if (aliases.size() == 0) {
					displayToast(getString(R.string.toast_no_trusted_certs));
					return true;
				}
				final ArrayList<Integer> selectedItems = new ArrayList<>();
				final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
				dialogBuilder.setTitle(getResources().getString(R.string.dialog_manage_certs_title));
				dialogBuilder.setMultiChoiceItems(aliases.toArray(new CharSequence[aliases.size()]), null,
						(dialog, indexSelected, isChecked) -> {
							if (isChecked) {
								selectedItems.add(indexSelected);
							} else if (selectedItems.contains(indexSelected)) {
								selectedItems.remove(Integer.valueOf(indexSelected));
							}
							if (selectedItems.size() > 0)
								((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
							else {
								((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
							}
						});

				dialogBuilder.setPositiveButton(
						getResources().getString(R.string.dialog_manage_certs_positivebutton), (dialog, which) -> {
							int count = selectedItems.size();
							if (count > 0) {
								for (int i = 0; i < count; i++) {
									try {
										Integer item = Integer.valueOf(selectedItems.get(i).toString());
										String alias = aliases.get(item);
										mtm.deleteCertificate(alias);
									} catch (KeyStoreException e) {
										e.printStackTrace();
										displayToast("Error: " + e.getLocalizedMessage());
									}
								}
								if (xmppConnectionServiceBound) {
									reconnectAccounts();
								}
								displayToast(getResources().getQuantityString(R.plurals.toast_delete_certificates, count, count));
							}
						});
				dialogBuilder.setNegativeButton(getResources().getString(R.string.dialog_manage_certs_negativebutton), null);
				AlertDialog removeCertsDialog = dialogBuilder.create();
				removeCertsDialog.show();
				removeCertsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				return true;
			});
		}

		final Preference createBackupPreference = mSettingsFragment.findPreference("create_backup");
		if (createBackupPreference != null) {
			createBackupPreference.setSummary(getString(R.string.pref_create_backup_summary, FileBackend.getBackupDirectory(this)));
			createBackupPreference.setOnPreferenceClickListener(preference -> {
				if (hasStoragePermission(REQUEST_CREATE_BACKUP)) {
					createBackup();
				}
				return true;
			});
		}

		//CMG AM-254
		final Preference wipeAllHistoryPreference = mSettingsFragment.findPreference("wipe_all_history");
		if (wipeAllHistoryPreference != null){
			wipeAllHistoryPreference.setOnPreferenceClickListener(preference -> {
				wipeAllHistoryDialog();
				return true;
			});
		}

		if (Config.ONLY_INTERNAL_STORAGE) {
			final Preference cleanCachePreference = mSettingsFragment.findPreference("clean_cache");
			if (cleanCachePreference != null) {
				cleanCachePreference.setOnPreferenceClickListener(preference -> cleanCache());
			}

			final Preference cleanPrivateStoragePreference = mSettingsFragment.findPreference("clean_private_storage");
			if (cleanPrivateStoragePreference != null) {
				cleanPrivateStoragePreference.setOnPreferenceClickListener(preference -> cleanPrivateStorage());
			}
		}

		final Preference deleteOmemoPreference = mSettingsFragment.findPreference("delete_omemo_identities");
		if (deleteOmemoPreference != null) {
			deleteOmemoPreference.setOnPreferenceClickListener(preference -> deleteOmemoIdentities());
		}

		//AM#14 next 2
		final SwitchPreference biometricPreference = (SwitchPreference) mSettingsFragment.findPreference("use_biometrics");
		if (biometricPreference != null){
			biometricPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object o) {
					if(biometricPreference.isChecked()){
						biometricPreference.setChecked(false);
					} else {
						if (!SystemSecurityInfo.isBiometricPINReady(SettingsActivity.this)) {
							AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
							builder.setTitle(R.string.biometrics_not_ready);
							builder.setMessage(R.string.biometrics_not_ready_message);
							builder.setPositiveButton(R.string.ok, null);
							AlertDialog dialog = builder.create();
							dialog.show();
						} else {
							biometricPreference.setChecked(true);
						}
					}
					return false;
				}
			});
		}

		/*final SwitchPreference pinPreference = (SwitchPreference) mSettingsFragment.findPreference("use_pin_code");
		if (pinPreference != null){
			pinPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object o) {
					if(pinPreference.isChecked()) {
						pinPreference.setChecked(false);
					} else {
						if (!SystemSecurityInfo.isPINReady(SettingsActivity.this)) {
							AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
							builder.setTitle(R.string.pin_not_ready);
							builder.setMessage(R.string.pin_not_ready_message);
							builder.setPositiveButton(R.string.ok, null);
							AlertDialog dialog = builder.create();
							dialog.show();
						} else {
							pinPreference.setChecked(true);
						}
					}
					return false;
				}
			});
		}*/
		final ListPreference locktimePref = (ListPreference) mSettingsFragment.findPreference("automatic_locktime");
		if (locktimePref != null) {
			String locktimeStr = locktimePref.getEntry().toString();
			setCurrentLocktime(locktimeStr);

			locktimePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override public boolean onPreferenceChange(Preference preference, Object o) {
					locktimePref.setValue((String)o);
					String locktimeStr = locktimePref.getEntry().toString();
					setCurrentLocktime(locktimeStr);
					return false;
				}
			});
		}
	}

	private void setCurrentLocktime(String locktimeStr) {
		final PreferenceCategory locktimePreference = (PreferenceCategory) mSettingsFragment.findPreference("locktime_selected");
		if (locktimePreference != null) {
			if (locktimeStr != null) {
				locktimePreference.setTitle("Lock time: " + locktimeStr);
			} else {
				locktimePreference.setTitle("");
			}
		}
	}


	/**  WIPE ALL  HISTORY  **/
	/**
	 * end ALL existing conversations
	 */
	@SuppressLint("InflateParams")
	protected void wipeAllHistoryDialog() {
		android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.action_wipe_all_history));
		View dialogView = this.getLayoutInflater().inflate(
				R.layout.dialog_wipe_all_history, null);
		final CheckBox deleteAllMessagesEachChatCheckBox = (CheckBox) dialogView
				.findViewById(R.id.delete_all_messages_each_chat_checkbox);
		final CheckBox endAllChatCheckBox = (CheckBox) dialogView
				.findViewById(R.id.end_all_chat_checkbox);
		final CheckBox deleteAllCachedFilesCheckBox = (CheckBox) dialogView
				.findViewById(R.id.delete_all_cached_files_checkbox);
		// default to true
		deleteAllMessagesEachChatCheckBox.setChecked(true);
		endAllChatCheckBox.setChecked(true);
		deleteAllCachedFilesCheckBox.setChecked(true);

		builder.setView(dialogView);
		builder.setNegativeButton(getString(R.string.cancel), null);
		builder.setPositiveButton(getString(R.string.wipe_all_history), (dialog, which) -> {
			// go through each conversation and either delete or end each chat depending
			// on what was checked.
			//ALF AM-81 kicked off thread to solve the underlying problem
			new Thread(() -> {
				List<Conversation> conversations = xmppConnectionService.getConversations();

				boolean hadMulti = false;
				for (int i = (conversations.size() - 1); i >= 0; i--) {
					if (endAllChatCheckBox.isChecked() && conversations.get(i).getMode() == Conversation.MODE_MULTI) {
						sendLeavingGroupMessage(conversations.get(i));
						hadMulti = true;
					}
				}
				if (hadMulti) {
					// sleep required so message goes out before conversation thread stopped
					try { Thread.sleep(3000); } catch (InterruptedException ie) {}
				}

				for (int i = (conversations.size() - 1); i >= 0; i--) {

					// delete messages
					if (deleteAllMessagesEachChatCheckBox.isChecked()) {
						xmppConnectionService.clearConversationHistory(conversations.get(i));
					}

					// end chat
					if (endAllChatCheckBox.isChecked()) {
						xmppConnectionService.archiveConversation(conversations.get(i));
					}
				}

				// delete everything in cache
				if (deleteAllCachedFilesCheckBox.isChecked()) {
					clearCachedFiles();
				}
			}).start();
		});
		builder.create().show();
	}

	/**
	 * //ALF AM-51
	 */
	public void sendLeavingGroupMessage(final Conversation conversation) {
		final Account account = conversation.getAccount();
		String dname = account.getDisplayName();
		if (dname == null) { dname = account.getUsername(); }
		String bod = dname + " " + getString(R.string.left_group);
		Message message = new Message(conversation, bod, conversation.getNextEncryption());
		this.xmppConnectionService.sendMessage(message);
		// sleep required so message goes out before conversation thread stopped
		// maybe show a spinner?
		//try { Thread.sleep(2000); } catch (InterruptedException ie) {} //moved to each place
	}
	private void changeOmemoSettingSummary() {
		ListPreference omemoPreference = (ListPreference) mSettingsFragment.findPreference(OMEMO_SETTING);
		if (omemoPreference != null) {
			String value = omemoPreference.getValue();
			switch (value) {
				case "always":
					omemoPreference.setSummary(R.string.pref_glacier_setting_summary_always);
					break;
				case "default_on":
					omemoPreference.setSummary(R.string.pref_glacier_setting_summary_default_on);
					break;
				case "default_off":
					omemoPreference.setSummary(R.string.pref_glacier_setting_summary_default_off);
					break;
			}
		} else {
			Log.d(Config.LOGTAG,"unable to find preference named "+OMEMO_SETTING);
		}
	}

	private boolean isCallable(final Intent i) {
		return i != null && getPackageManager().queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
	}


	private boolean cleanCache() {
		Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.parse("package:" + getPackageName()));
		try {
			startActivity(intent);
		} catch (Exception e6) {
			e6.printStackTrace();
		}
		return true;
	}

	private boolean cleanPrivateStorage() {
		for(String type : Arrays.asList("Images", "Videos", "Files", "Recordings")) {
		        cleanPrivateFiles(type);
	    }
		return true;
	}

	private void cleanPrivateFiles(final String type) {
		try {
			File dir = new File(getFilesDir().getAbsolutePath(), "/" + type + "/");
			File[] array = dir.listFiles();
			if (array != null) {
				for (int b = 0; b < array.length; b++) {
					String name = array[b].getName().toLowerCase();
					if (name.equals(".nomedia")) {
						continue;
					}
					if (array[b].isFile()) {
						array[b].delete();
					}
				}
			}
		} catch (Throwable e) {
			Log.e("CleanCache", e.toString());
		}
	}

	private boolean deleteOmemoIdentities() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.pref_delete_glacier_identities);
		builder.setMessage(R.string.pref_reset_omemo_message); //ALF AM-79

		//ALF AM-79 commented out
		/*final List<CharSequence> accounts = new ArrayList<>();
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.isEnabled()) {
				accounts.add(account.getJid().asBareJid().toString());
			}
		}
		final boolean[] checkedItems = new boolean[accounts.size()];
		builder.setMultiChoiceItems(accounts.toArray(new CharSequence[accounts.size()]), checkedItems, (dialog, which, isChecked) -> {
			checkedItems[which] = isChecked;
			final AlertDialog alertDialog = (AlertDialog) dialog;
			for (boolean item : checkedItems) {
				if (item) {
					alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
					return;
				}
			}
			alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
		});*/

		builder.setNegativeButton(R.string.cancel, null);
		builder.setPositiveButton(R.string.reset, (dialog, which) -> { //ALF AM-79 to reset
			for(Account account : xmppConnectionService.getAccounts()) {
				if (account != null) {
					account.getAxolotlService().regenerateKeys(true);
				}
			}
			/*for (int i = 0; i < checkedItems.length; ++i) {
				if (checkedItems[i]) {
					try {
						Jid jid = Jid.of(accounts.get(i).toString());
						Account account = xmppConnectionService.findAccountByJid(jid);
						if (account != null) {
							account.getAxolotlService().regenerateKeys(true);
						}
					} catch (IllegalArgumentException e) {
						//
					}

				}
			}*/
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		//dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false); //ALF AM-79
		return true;
	}

	@Override
	public void onStop() {
		super.onStop();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String name) {
		final List<String> resendPresence = Arrays.asList(
				"confirm_messages",
				DND_ON_SILENT_MODE,
				AWAY_WHEN_SCREEN_IS_OFF,
				"allow_message_correction",
				TREAT_VIBRATE_AS_SILENT,
				MANUALLY_CHANGE_PRESENCE,
				BROADCAST_LAST_ACTIVITY);
		if (name.equals(OMEMO_SETTING)) {
			OmemoSetting.load(this, preferences);
			changeOmemoSettingSummary();
		} else if (name.equals(KEEP_FOREGROUND_SERVICE)) {
			xmppConnectionService.toggleForegroundService();
		} else if (resendPresence.contains(name)) {
			if (xmppConnectionServiceBound) {
				if (name.equals(AWAY_WHEN_SCREEN_IS_OFF) || name.equals(MANUALLY_CHANGE_PRESENCE)) {
					xmppConnectionService.toggleScreenEventReceiver();
				}
				xmppConnectionService.refreshAllPresences();
			}
		} else if (name.equals("dont_trust_system_cas")) {
			xmppConnectionService.updateMemorizingTrustmanager();
			reconnectAccounts();
		} else if (name.equals("use_tor")) {
			reconnectAccounts();
		} else if (name.equals(AUTOMATIC_MESSAGE_DELETION)) {
			xmppConnectionService.expireOldMessages(true);
//		} else if (name.equals(DISPLAYNAME)) { //ALF AM-48
//			String newname = preferences.getString(name, null);
//			changeDisplayName(newname);
		} else if (name.equals(GLOBAL_MESSAGE_TIMER)) { //ALF AM-53
			int timer = Message.TIMER_NONE;
			String timerStr = preferences.getString(name, null);
			if (timerStr != null)
			{
				try {
					timer = Integer.parseInt(timerStr);
				} catch(NumberFormatException nfe) {
					timer = Message.TIMER_NONE;
				}
			}
			changeGlobalTimer(timer);
		} else if (name.equals(THEME)) {
			final int theme = findTheme();
			if (this.mTheme != theme) {
				recreate();
			}
		} else if (name.equals(SCREEN_SECURITY)){
			//CMG AM-137
			initializeScreenshotSecurity();
		}

	}

	//ALF AM-53
	public void changeGlobalTimer(int timer) {
		for (Account account : xmppConnectionService.getAccounts()) {
			account.setTimer(timer);
			xmppConnectionService.databaseBackend.updateAccount(account);
		}
	}

	//ALF AM-48
	public void changeDisplayName(String newname) {
		for (Account account : xmppConnectionService.getAccounts()) {
			account.setDisplayName(newname);
			xmppConnectionService.databaseBackend.updateAccount(account);
			xmppConnectionService.publishDisplayName(account);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults.length > 0)
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				if (requestCode == REQUEST_CREATE_BACKUP) {
					createBackup();
				}
			} else {
				Toast.makeText(this, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
			}
	}

	private void createBackup() {
		ContextCompat.startForegroundService(this, new Intent(this, ExportBackupService.class));
	}

	private void displayToast(final String msg) {
		runOnUiThread(() -> Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show());
	}

	private void reconnectAccounts() {
		for (Account account : xmppConnectionService.getAccounts()) {
			if (account.isEnabled()) {
				xmppConnectionService.reconnectAccountInBackground(account);
			}
		}
	}

	public void refreshUiReal() {
		//nothing to do. This Activity doesn't implement any listeners
	}

//	//ALF AM-143 here to end
//	@Override
//	public void onLogout() {
//
//		//ALF AM-228, AM-202 store account in memory in case using same account
//		//Account curAccount = xmppConnectionService.getAccounts().get(0);
//		//if (curAccount != null) {
//		//	xmppConnectionService.setExistingAccount(curAccount);
//		//}
//
//		// clear all conversations
//		List<Conversation> conversations = xmppConnectionService.getConversations();
//
//		for (int i = (conversations.size() - 1); i >= 0; i--) {
//			xmppConnectionService.clearConversationHistory(conversations.get(i));
//			// endConversation(conversations.get(i), false, true);
//		}
//
//		// wipe all accounts
//		List<Account> accounts = xmppConnectionService.getAccounts();
//
//		for (Account account : accounts) {
//			xmppConnectionService.deleteAccount(account);
//		}
//
//		// logout of Cognito
//		// sometimes if it's been too long, I believe pool doesn't
//		// exists and user is no longer logged in
//		CognitoUserPool userPool = AppHelper.getPool();
//		if (userPool != null) {
//			CognitoUser user = userPool.getCurrentUser();
//			if (user != null) {
//				user.signOut();
//			}
//		}
//
//		// clear s3bucket client
//		Util.clearS3Client(getApplicationContext());
//
//		// clear all stored content
//		clearCachedFiles();
//
//		// login screen
//		Intent editAccount = new Intent(this, ConversationActivity.class);
//		startActivity(editAccount);
//	}

	/**
	 * Clear images, files from directory
	 */
	private void clearCachedFiles() {
		// clear images, etc
		clearLocalFiles();

		// clear images, etc
		clearPictures();

		// clear voice recordings from plugin
		clearVoiceRecordings();

		// clear shared location
		clearSharedLocations();

		// clear internal storage
		clearExternalStorage();

		//ALF AM-146
		clearAppCache();
	}

	//ALF AM-146 (and next method)
	private void clearAppCache() {
		try {
			File dir = getApplicationContext().getCacheDir();
			deleteDir(dir);
		} catch (Exception e) { e.printStackTrace();}
	}

	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
			return dir.delete();
		} else if(dir!= null && dir.isFile()) {
			return dir.delete();
		} else {
			return false;
		}
	}

	/**
	 * Clear local files for Messenger
	 */
	private void clearLocalFiles() {
		// Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Messenger";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}
	}

	/**
	 * Clear voice recordings
	 */
	private void clearVoiceRecordings() {
		// Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Voice Recorder";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}
	}

	/**
	 * Clear shared locations
	 */
	private void clearSharedLocations() {
		// Retrieve directory
		//String extStore = System.getenv("EXTERNAL_STORAGE") + "/Android/data/com.glaciersecurity.glaciermessenger.sharelocation/cache";
		ArrayList<String> deletedFiles = new ArrayList<String>();
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Android/data/com.glaciersecurity.glaciermessenger.sharelocation";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {

			String extStore2 = extStore + "/cache";
			File f_exts2 = new File(extStore2);

			if (f_exts2.exists()) {
				File[] fileDir = f_exts2.listFiles();

				// delete file
				for (int i = 0; i < fileDir.length; i++) {
					if (fileDir[i].delete()) {
						deletedFiles.add(fileDir[i].toString());
						Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
					} else {
						Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
					}
				}
				if (f_exts2.delete()) {
					deletedFiles.add(f_exts2.toString());
				}
			}

			if (f_exts.delete()) {
				deletedFiles.add(f_exts.toString());
			}

			// String[] delFile = {fileDir[fileDir.length-1].toString()};
			String[] stringArray = deletedFiles.toArray(new String[0]);
			// callBroadcast(deletedFiles.toArray(new String[0]));
		}
	}

	private void clearExternalStorage() {
		FileBackend.removeStorageDirectory(getApplicationContext()); //ALF AM-603 added context
	}

	/**
	 * Clear pictures in Pictures/Messenger directory
	 */
	private void clearPictures() {
		// Retrieve directory
		String extStore = System.getenv("EXTERNAL_STORAGE") + "/Pictures/Messenger";
		File f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				if (fileDir[i].delete()) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}

		// Remove higher level files
		extStore = System.getenv("EXTERNAL_STORAGE") + "/Pictures";
		f_exts = new File(extStore);

		// check if directory exists
		if (f_exts.exists()) {
			File[] fileDir = f_exts.listFiles();
			String[] deletedFiles = new String[fileDir.length];
			int deletedFilesIndex = 0;

			// delete file
			for (int i = 0; i < fileDir.length; i++) {
				// do not remove directory
				if ((!fileDir[i].isDirectory()) && (fileDir[i].delete())) {
					deletedFiles[deletedFilesIndex] = fileDir[i].toString();
					deletedFilesIndex++;
					Log.d("Glacier", "File list: Successfully deleted " + fileDir[i]);
				} else {
					Log.d("Glacier", "File list: Did not delete " + fileDir[i]);
				}
			}
		}
	}
}
