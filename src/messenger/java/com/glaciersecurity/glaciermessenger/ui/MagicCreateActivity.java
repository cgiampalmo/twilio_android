package com.glaciersecurity.glaciermessenger.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class MagicCreateActivity extends XmppActivity implements TextWatcher {

	private TextView mFullJidDisplay;
	private EditText mUsername;

	@Override
	protected void refreshUiReal() {

	}

	@Override
    protected void onBackendConnected() {

	}

	@Override
	public void onStart() {
		super.onStart();
		final int theme = findTheme();
		if (this.mTheme != theme) {
			recreate();
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		if (getResources().getBoolean(R.bool.portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.magic_create);
		setSupportActionBar(findViewById(R.id.toolbar));
		configureActionBar(getSupportActionBar());
		mFullJidDisplay = findViewById(R.id.full_jid);
		mUsername = findViewById(R.id.username);
		Button next = findViewById(R.id.create_account);
		next.setOnClickListener(v -> {
			try {
				String username = mUsername.getText().toString();
				Jid jid = Jid.of(username.toLowerCase(), Config.MAGIC_CREATE_DOMAIN, null);
				if (!jid.getEscapedLocal().equals(jid.getLocal())|| username.length() < 3) {
					mUsername.setError(getString(R.string.invalid_username));
					mUsername.requestFocus();
				} else {
					mUsername.setError(null);
					Account account = xmppConnectionService.findAccountByJid(jid);
					if (account == null) {
						account = new Account(jid, CryptoHelper.createPassword(new SecureRandom()));
						account.setOption(Account.OPTION_REGISTER, true);
						account.setOption(Account.OPTION_DISABLED, true);
						account.setOption(Account.OPTION_MAGIC_CREATE, true);
						xmppConnectionService.createAccount(account, true);
					}
					Intent intent = new Intent(MagicCreateActivity.this, EditAccountActivity.class);
					intent.putExtra("jid", account.getJid().asBareJid().toString());
					intent.putExtra("init", true);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					Toast.makeText(MagicCreateActivity.this, R.string.secure_password_generated, Toast.LENGTH_SHORT).show();
					StartConversationActivity.addInviteUri(intent, getIntent());
					startActivity(intent);
				}
			} catch (IllegalArgumentException e) {
				mUsername.setError(getString(R.string.invalid_username));
				mUsername.requestFocus();
			}
		});
		mUsername.addTextChangedListener(this);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable s) {
		if (s.toString().trim().length() > 0) {
			try {
				mFullJidDisplay.setVisibility(View.VISIBLE);
				Jid jid = Jid.of(s.toString().toLowerCase(), Config.MAGIC_CREATE_DOMAIN, null);
				mFullJidDisplay.setText(getString(R.string.your_full_jid_will_be, jid.toEscapedString()));
			} catch (IllegalArgumentException e) {
				mFullJidDisplay.setVisibility(View.INVISIBLE);
			}

		} else {
			mFullJidDisplay.setVisibility(View.INVISIBLE);
		}
	}
}
