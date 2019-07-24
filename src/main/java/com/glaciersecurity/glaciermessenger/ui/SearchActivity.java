/*
 * Copyright (c) 2018, Daniel Gultsch All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.glaciersecurity.glaciermessenger.ui;

import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivitySearchBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Conversational;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.StubConversation;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.services.MessageSearchTask;
import com.glaciersecurity.glaciermessenger.ui.adapter.MessageAdapter;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnSearchResultsAvailable;
import com.glaciersecurity.glaciermessenger.ui.util.ChangeWatcher;
import com.glaciersecurity.glaciermessenger.ui.util.Color;
import com.glaciersecurity.glaciermessenger.ui.util.DateSeparator;
import com.glaciersecurity.glaciermessenger.ui.util.ListViewUtils;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.ShareUtil;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.glaciersecurity.glaciermessenger.utils.FtsUtils;
import com.glaciersecurity.glaciermessenger.utils.MessageUtils;

import static com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils.hideSoftKeyboard;
import static com.glaciersecurity.glaciermessenger.ui.util.SoftKeyboardUtils.showKeyboard;

public class SearchActivity extends XmppActivity implements TextWatcher, OnSearchResultsAvailable, MessageAdapter.OnContactPictureClicked, ConnectivityReceiver.ConnectivityReceiverListener {

	private static final String EXTRA_SEARCH_TERM = "search-term";

	private ActivitySearchBinding binding;
	private MessageAdapter messageListAdapter;
	private final List<Message> messages = new ArrayList<>();
	private WeakReference<Message> selectedMessageReference = new WeakReference<>(null);
	private final ChangeWatcher<List<String>> currentSearch = new ChangeWatcher<>();
	private final PendingItem<String> pendingSearchTerm = new PendingItem<>();
	private final PendingItem<List<String>> pendingSearch = new PendingItem<>();

	private ConnectivityReceiver connectivityReceiver; //CMG AM-41
	private LinearLayout offlineLayout;
	private TextView networkStatus;


	@Override
	public void onCreate(final Bundle bundle) {
		final String searchTerm = bundle == null ? null : bundle.getString(EXTRA_SEARCH_TERM);
		if (searchTerm != null) {
			pendingSearchTerm.push(searchTerm);
		}
		super.onCreate(bundle);
		this.binding = DataBindingUtil.setContentView(this, R.layout.activity_search);
		setSupportActionBar((Toolbar) this.binding.toolbar);
		configureActionBar(getSupportActionBar());
		this.messageListAdapter = new MessageAdapter(this, this.messages);
		this.messageListAdapter.setOnContactPictureClicked(this);
		this.binding.searchResults.setAdapter(messageListAdapter);
		registerForContextMenu(this.binding.searchResults);

		//CMG AM-41
        this.offlineLayout = findViewById(R.id.offline_layout);
        this.networkStatus = findViewById(R.id.network_status);
        this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
        connectivityReceiver = new ConnectivityReceiver(this);
		updateOfflineStatusBar();
		checkNetworkStatus();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.activity_search, menu);
		final MenuItem searchActionMenuItem = menu.findItem(R.id.action_search);
		final EditText searchField = searchActionMenuItem.getActionView().findViewById(R.id.search_field);
		final String term = pendingSearchTerm.pop();
		if (term != null) {
			searchField.append(term);
			List<String> searchTerm = FtsUtils.parse(term);
			if (xmppConnectionService != null) {
				if (currentSearch.watch(searchTerm)) {
					xmppConnectionService.search(searchTerm, this);
				}
			} else {
				pendingSearch.push(searchTerm);
			}
		}
		searchField.addTextChangedListener(this);
		searchField.setHint(R.string.search_messages);
		searchField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
		if (term == null) {
			showKeyboard(searchField);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final Message message = this.messages.get(acmi.position);
		this.selectedMessageReference = new WeakReference<>(message);
		getMenuInflater().inflate(R.menu.search_result_context, menu);
		MenuItem copy = menu.findItem(R.id.copy_message);
		MenuItem quote = menu.findItem(R.id.quote_message);
// DJF		MenuItem copyUrl = menu.findItem(R.id.copy_url);
		if (message.isGeoUri()) {
			copy.setVisible(false);
			quote.setVisible(false);
		}
// DJF		else {
// DJF			copyUrl.setVisible(false);
// DJF		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			hideSoftKeyboard(this);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final Message message = selectedMessageReference.get();
		if (message != null) {
			switch (item.getItemId()) {
				case R.id.open_conversation:
					switchToConversation(wrap(message.getConversation()));
					break;
				case R.id.share_with:
					ShareUtil.share(this, message);
					break;
				case R.id.copy_message:
					ShareUtil.copyToClipboard(this, message);
					break;
// DJF				case R.id.copy_url:
// DJF					ShareUtil.copyUrlToClipboard(this, message);
// DJF					break;
				case R.id.quote_message:
					quote(message);
					break;
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		List<String> term = currentSearch.get();
		if (term != null && term.size() > 0) {
			bundle.putString(EXTRA_SEARCH_TERM,FtsUtils.toUserEnteredString(term));
		}
		super.onSaveInstanceState(bundle);
	}

	private void quote(Message message) {
		switchToConversationAndQuote(wrap(message.getConversation()), MessageUtils.prepareQuote(message));
	}

	private Conversation wrap(Conversational conversational) {
		if (conversational instanceof Conversation) {
			return (Conversation) conversational;
		} else {
			return xmppConnectionService.findOrCreateConversation(conversational.getAccount(),
					conversational.getJid(),
					conversational.getMode() == Conversational.MODE_MULTI,
					true,
					true);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	protected void onStop () {
		unregisterReceiver(connectivityReceiver);
		super.onStop();
	}

	@Override
	protected void refreshUiReal() {

	}

	@Override
	void onBackendConnected() {
		final List<String> searchTerm = pendingSearch.pop();
		if (searchTerm != null && currentSearch.watch(searchTerm)) {
			xmppConnectionService.search(searchTerm, this);
		}
		//CMG AM-41
		updateOfflineStatusBar();
	}


	@Override
	public void onNetworkConnectionChanged(boolean isConnected) {
		if (isConnected) {
			onConnected();
		} else {
			onDisconnected();
		}

	}
	// CMG AM-41
	private void checkNetworkStatus() {
		updateOfflineStatusBar();
	}

	private View.OnClickListener mRefreshNetworkClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			networkStatus.setCompoundDrawables(null, null, null, null);
			String previousNetworkState = networkStatus.getText().toString();
			if (previousNetworkState != null) {
				if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_available))) {
					networkStatus.setText(getResources().getString(R.string.refreshing_status));
				}
				else if (previousNetworkState.contains(getResources().getString(R.string.disconnect_tap_to_connect)) ){
					networkStatus.setText(getResources().getString(R.string.refreshing_connection));
				}
			} else {
				networkStatus.setText(getResources().getString(R.string.refreshing));
			}

			final Account account = xmppConnectionService.getAccounts().get(0);
			if (account != null) {
				Account.State accountStatus = account.getStatus();
				Presence.Status presenceStatus = account.getPresenceStatus();
				if (!presenceStatus.equals(Presence.Status.ONLINE)){
					account.setPresenceStatus(Presence.Status.ONLINE);
					xmppConnectionService.updateAccount(account);

				} else {
					if (accountStatus == Account.State.ONLINE || accountStatus == Account.State.CONNECTING) {
					} else {
						account.setOption(Account.OPTION_DISABLED, false);
						xmppConnectionService.updateAccount(account);
					}
				}

			}
			updateOfflineStatusBar();
		}
	};

	private void updateOfflineStatusBar(){
		if (ConnectivityReceiver.isConnected(this)) {
			if (xmppConnectionService != null){
				final Account account = xmppConnectionService.getAccounts().get(0);
				Account.State accountStatus = account.getStatus();
				Presence.Status presenceStatus = account.getPresenceStatus();
				if (!presenceStatus.equals(Presence.Status.ONLINE)){
					runStatus( presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available) ,true);
					Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available));
				} else {
					if (accountStatus == Account.State.ONLINE || accountStatus == Account.State.CONNECTING) {
						runStatus("Online", false);
					} else {
						runStatus(getResources().getString(R.string.disconnect_tap_to_connect),true);
						Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + accountStatus.getReadableId());
					}
				}
			}
		} else {
			runStatus(getResources().getString(R.string.disconnect_tap_to_connect), true);
			Log.w(Config.LOGTAG ,"updateOfflineStatusBar disconnected from network");

		}
	}
	private void runStatus(String str, boolean isVisible){
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				reconfigureOfflineText(str);
				if(isVisible){
					offlineLayout.setVisibility(View.VISIBLE);
				} else {
					offlineLayout.setVisibility(View.GONE);
				}
			}
		}, 1000);
	}
	private void reconfigureOfflineText(String str) {
		networkStatus.setText(str);
		Drawable refreshIcon =
				ContextCompat.getDrawable(this, R.drawable.ic_refresh_black_24dp);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
			networkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(refreshIcon, null, null, null);
		} else{
			refreshIcon.setBounds(0, 0, refreshIcon.getIntrinsicWidth(), refreshIcon.getIntrinsicHeight());
			networkStatus.setCompoundDrawables(refreshIcon, null, null, null);
		}
	}
	public void onConnected(){
		offlineLayout.setVisibility(View.GONE);
	}

	public void onDisconnected(){
		offlineLayout.setVisibility(View.VISIBLE);
	}

	private void changeBackground(boolean hasSearch, boolean hasResults) {
		if (hasSearch) {
			if (hasResults) {
				binding.searchResults.setBackgroundColor(StyledAttributes.getColor(this, R.attr.color_background_secondary));
			} else {
				binding.searchResults.setBackground(StyledAttributes.getDrawable(this, R.attr.activity_background_no_results));
			}
		} else {
			binding.searchResults.setBackground(StyledAttributes.getDrawable(this, R.attr.activity_background_search));
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		final List<String> term = FtsUtils.parse(s.toString().trim());
		if (!currentSearch.watch(term)) {
			return;
		}
		if (term.size() > 0) {
			xmppConnectionService.search(term, this);
		} else {
			MessageSearchTask.cancelRunningTasks();
			this.messages.clear();
			messageListAdapter.setHighlightedTerm(null);
			messageListAdapter.notifyDataSetChanged();
			changeBackground(false, false);
		}
	}

	@Override
	public void onSearchResultsAvailable(List<String> term, List<Message> messages) {
		runOnUiThread(() -> {
			this.messages.clear();
			messageListAdapter.setHighlightedTerm(term);
			DateSeparator.addAll(messages);
			this.messages.addAll(messages);
			messageListAdapter.notifyDataSetChanged();
			changeBackground(true, messages.size() > 0);
			ListViewUtils.scrollToBottom(this.binding.searchResults);
		});
	}

	@Override
	public void onContactPictureClicked(Message message) {
		String fingerprint;
		if (message.getEncryption() == Message.ENCRYPTION_PGP || message.getEncryption() == Message.ENCRYPTION_DECRYPTED) {
			fingerprint = "pgp";
		} else {
			fingerprint = message.getFingerprint();
		}
		if (message.getStatus() == Message.STATUS_RECEIVED) {
			final Contact contact = message.getContact();
			if (contact != null) {
				if (contact.isSelf()) {
					switchToAccount(message.getConversation().getAccount(), fingerprint);
				} else {
					switchToContactDetails(contact, fingerprint);
				}
			}
		} else {
			switchToAccount(message.getConversation().getAccount(), fingerprint);
		}
	}
}
