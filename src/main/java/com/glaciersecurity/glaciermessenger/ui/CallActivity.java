package com.glaciersecurity.glaciermessenger.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;

import rocks.xmpp.addr.Jid;

//CMG AM-410
public class CallActivity extends XmppActivity {

	public static final String ACTION_INCOMING_CALL = "incoming_call";
	public static final String ACTION_OUTGOING_CALL = "outgoing_code";
	public static final String ACTION_ACCEPTED_CALL = "call_accepted";
	private AudioManager audioManager;


	private Boolean isAudioMuted = false;
	private Boolean isVideoMuted = true;
	private Boolean isSpeakerphoneOn = false;
	private Boolean isVideoEnabled = false;

	private TextView callState;
	private TextView contactText;
	private LinearLayout incomingCallLayout;
	private LinearLayout outgoingCallLayout;
	private AppCompatImageButton rejectCallBtn;
	private AppCompatImageButton acceptCallBtn;
	private AppCompatImageButton endCallBtn;
	private AppCompatImageButton videoBtn;
	private AppCompatImageButton audioBtn;
	private AppCompatImageButton speakerBtn;
	private ImageView avatar;


	private TwilioCall currentTwilioCall;
	private Jid contactJid;
	private String conversationUuid;
	private Handler handler = new Handler();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_call);
		setSupportActionBar(findViewById(R.id.toolbar));
		this.callState = findViewById(R.id.call_state);
		this.contactText = findViewById(R.id.participant_selected_identity);
		this.incomingCallLayout = findViewById(R.id.call_status_incoming);
		this.outgoingCallLayout = findViewById(R.id.call_status_outgoing);
		this.avatar = (ImageView) findViewById(R.id.participant_stub_image);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			this.setTurnScreenOn(true);
			this.setShowWhenLocked(true);
		}

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.setSpeakerphoneOn(isSpeakerphoneOn);

		registerReceiver(mMessageReceiver, new IntentFilter("callActivityFinish"));

		if (getIntent().getAction().equals(ACTION_OUTGOING_CALL)) {
			try {
				this.contactJid = Jid.of(getIntent().getExtras().getString("receiver"));
				this.conversationUuid = getIntent().getExtras().getString("uuid");

			} catch (final IllegalArgumentException ignored) {
			}
		}
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


		this.endCallBtn= findViewById(R.id.end_call_button);
		endCallBtn.setOnClickListener(v -> {
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_CANCEL_CALL_REQUEST);
			Compatibility.startService(this, intent);
		});
		this.videoBtn = findViewById(R.id.local_video_image_button);
//		videoBtn.setOnClickListener(v -> {
//
//			int icon = isVideoMuted ?
//					R.drawable.ic_videocam_white_24px : R.drawable.ic_videocam_off_gray_24px;
//			videoBtn.setImageDrawable(ContextCompat.getDrawable(
//					CallActivity.this, icon));
//
//		});
		this.audioBtn = findViewById(R.id.audio_image_button);
		audioBtn.setOnClickListener(v -> {
			audioManager.setMicrophoneMute(isAudioMuted);

			int icon = isAudioMuted ?
					R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
			audioBtn.setImageDrawable(ContextCompat.getDrawable(
					CallActivity.this, icon));
		});

		// TODO in twilio roomActiviy audio manager is used to manage speaker phone status
		this.speakerBtn= findViewById(R.id.speaker_button);
		speakerBtn.setOnClickListener(v -> {
			audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
			int icon = isSpeakerphoneOn ?
					R.drawable.ic_volume_up_white_24dp : R.drawable.ic_volume_off_gray_24dp;
			speakerBtn.setImageDrawable(ContextCompat.getDrawable(
					CallActivity.this, icon));
		});

	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = getIntent();
		if (intent == null) {
			return;
		}

		final String action = intent.getAction();

		if (CallActivity.ACTION_INCOMING_CALL.equals(action)) {
			TwilioCall call = new TwilioCall(null);
			call.setCallId(intent.getIntExtra("call_id", 0));
			call.setCaller(intent.getStringExtra("caller"));
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

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			handler.removeCallbacksAndMessages(null);
			finish();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		handler.removeCallbacksAndMessages(null);
		unregisterReceiver(mMessageReceiver);
	}

	@Override
	protected void onBackendConnected() {
		try {
			if (xmppConnectionService != null) {
				Account mAccount = xmppConnectionService.getAccounts().get(0);
				if (mAccount != null && conversationUuid != null) {
					Conversation conversation = xmppConnectionService.findConversationByUuid(conversationUuid);
					avatar.setImageBitmap(avatarService().get(conversation.getContact(), (int) getResources().getDimension(R.dimen.avatar_on_incoming_call_screen_size)));

				}
			}
		} catch (Exception e){

		}
	}


	public void refreshUiReal() {
	}
	private void onIncomingCall(){
		String incoming = getResources().getString(R.string.incoming_call);
		callState.setText(incoming);
		contactText.setText(currentTwilioCall.getCaller());
		incomingCallLayout.setVisibility(View.VISIBLE);
		outgoingCallLayout.setVisibility(View.GONE);

		handler.postDelayed(() -> {
			Log.d(Config.LOGTAG, "CallActivity - Cancelling call after 30 sec");
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_REJECT_CALL_REQUEST);
			Compatibility.startService(this, intent);
		},30000);
	}

	private void onOutgoingCall(){
		callState.setText(getResources().getString(R.string.outgoing_call));
		try {
			contactText.setText(Jid.of(currentTwilioCall.getReceiver()).getEscapedLocal());
		}
		catch (final IllegalArgumentException ignored) {
			contactText.setText(contactJid);
		}
		incomingCallLayout.setVisibility(View.GONE);
		outgoingCallLayout.setVisibility(View.VISIBLE);
	}

}
