package com.glaciersecurity.glaciermessenger.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.databinding.DataBindingUtil;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;

import rocks.xmpp.addr.Jid;

//CMG AM-410
public class CallActivity extends XmppActivity {

	public static final String ACTION_INCOMING_CALL = "incoming_call";
	public static final String ACTION_OUTGOING_CALL = "outgoing_code";

	private TextView callState;
	private TextView contact;
	private LinearLayout incomingCallLayout;
	private LinearLayout outgoingCallLayout;
	private AppCompatImageButton rejectCallBtn;
	private AppCompatImageButton acceptCallBtn;
	private ImageView avatar;


	private TwilioCall currentTwilioCall;
	private Jid contactJid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call);
		setSupportActionBar(findViewById(R.id.toolbar));
		this.callState = findViewById(R.id.call_state);
		this.contact = findViewById(R.id.participant_selected_identity);
		this.incomingCallLayout = findViewById(R.id.call_status_incoming);
		this.outgoingCallLayout = findViewById(R.id.call_status_outgoing);
		this.acceptCallBtn= findViewById(R.id.accept_call_button);
		this.avatar = (ImageView) findViewById(R.id.participant_stub_image);

		if (getIntent().getAction().equals(ACTION_OUTGOING_CALL)) {
			try {
				this.contactJid = Jid.of(getIntent().getExtras().getString("receiver"));
			} catch (final IllegalArgumentException ignored) {
			}
		}
		acceptCallBtn.setOnClickListener(v -> {
			//needs access to XmppConnectionService
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_ACCEPT_CALL_REQUEST);
			Compatibility.startService(this, intent);
		});
		this.rejectCallBtn= findViewById(R.id.reject_call_button);
		rejectCallBtn.setOnClickListener(v -> {
			//needs access to XmppConnectionService
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_REJECT_CALL_REQUEST);
			Compatibility.startService(this, intent);
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = getIntent();
		if (intent == null) {
			return;
		}
		//final String type = intent.getType();
		final String action = intent.getAction();

		if (CallActivity.ACTION_INCOMING_CALL.equals(action)) {
			TwilioCall call = new TwilioCall(null);
			try {
				int callid = Integer.parseInt(intent.getStringExtra("call_id"));
				call.setCallId(callid);
			} catch (NumberFormatException nfe) {
			}
			call.setCaller(intent.getStringExtra("caller"));
			call.setRoomName(intent.getStringExtra("roomname"));
			call.setStatus(intent.getStringExtra("status"));
			currentTwilioCall = call;
			this.onIncomingCall();
		} else if (CallActivity.ACTION_OUTGOING_CALL.equals(action)) {
			TwilioCall call = new TwilioCall(null);
			call.setReceiver(intent.getStringExtra("receiver"));
			currentTwilioCall = call;
			this.onOutgoingCall();
		}
	}
	@Override
	protected void onBackendConnected() {
//		if (xmppConnectionService != null) {
//			avatar.setImageBitmap(avatarService().get(xmppConnectionService.findAccountByJid(Jid.of(contactJid)), (int) getResources().getDimension(R.dimen.avatar_on_details_screen_size)));
//		}
	}
	public void refreshUiReal() {
	}
	private void onIncomingCall(){
		String incoming = getResources().getString(R.string.incoming_call) + " from " + currentTwilioCall.getCaller();
		callState.setText(incoming);
		contact.setText(contactJid.getEscapedLocal());
		incomingCallLayout.setVisibility(View.VISIBLE);
		outgoingCallLayout.setVisibility(View.GONE);

	}

	private void onOutgoingCall(){
		callState.setText(getResources().getString(R.string.outgoing_call));
		//contact.setText(currentTwilioCall.getCaller());
		contact.setText(contactJid.getEscapedLocal());
		incomingCallLayout.setVisibility(View.GONE);
		outgoingCallLayout.setVisibility(View.VISIBLE);
	}

}
