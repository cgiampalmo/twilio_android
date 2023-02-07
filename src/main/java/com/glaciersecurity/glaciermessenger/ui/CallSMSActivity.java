package com.glaciersecurity.glaciermessenger.ui;

import android.os.Bundle;
import com.glaciersecurity.glaciermessenger.R;

public class CallSMSActivity extends XmppActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_twilio_call);
		setSupportActionBar(findViewById(R.id.toolbar));
	}

	@Override
	protected void refreshUiReal() {
	}

	@Override
	void onBackendConnected() {
	}

}
