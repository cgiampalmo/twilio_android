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
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;

//CMG AM-410
public class CallActivity extends AppCompatActivity {

	public static final String ACTION_INCOMING_CALL = "incoming_call";
	public static final String ACTION_OUTGOING_CALL = "outgoing_code";

	//private ImageView mAvatar;
	private TextView callState;
	private TextView rejectText;
	private LinearLayout acceptCall;
	private LinearLayout endCall;
	private AppCompatImageButton rejectCallBtn;
	private AppCompatImageButton acceptCallBtn;

	private TwilioCall currentTwilioCall;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call);
		setSupportActionBar(findViewById(R.id.toolbar));
		this.callState = findViewById(R.id.call_state);
		this.acceptCall = findViewById(R.id.accept);
		this.endCall = findViewById(R.id.end_call);
		this.rejectText = findViewById(R.id.reject_text);
		this.acceptCallBtn= findViewById(R.id.accept_call_button);
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
			call.setReceiver(intent.getStringExtra("caller"));
			currentTwilioCall = call;
			this.onOutgoingCall();
		}
	}

	private void onIncomingCall(){
		String incoming = getResources().getString(R.string.incoming_call) + " from " + currentTwilioCall.getCaller();
		callState.setText(incoming);
		acceptCall.setVisibility(View.VISIBLE);
		endCall.setVisibility(View.VISIBLE);
		rejectText.setText(getResources().getString(R.string.reject_call));

	}

	private void onOutgoingCall(){
		callState.setText(getResources().getString(R.string.outgoing_call));
		acceptCall.setVisibility(View.GONE);
		endCall.setVisibility(View.VISIBLE);
		rejectText.setText(getResources().getString(R.string.end_call));

	}

}
