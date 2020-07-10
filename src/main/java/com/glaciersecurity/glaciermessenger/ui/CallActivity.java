package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.util.SoundPoolManager;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;
import com.twilio.audioswitch.selection.AudioDevice;
import com.twilio.audioswitch.selection.AudioDeviceSelector;

import kotlin.Unit;
import rocks.xmpp.addr.Jid;

//CMG AM-410
public class CallActivity extends XmppActivity {

	public static final String ACTION_INCOMING_CALL = "incoming_call";
	public static final String ACTION_OUTGOING_CALL = "outgoing_code";
	public static final String ACTION_ACCEPTED_CALL = "call_accepted";

	private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;

	private AudioManager audioManager;

	private TextView callState;
	private TextView contactText;
	private LinearLayout incomingCallLayout;
	private LinearLayout outgoingCallLayout;
	private AppCompatImageButton rejectCallBtn;
	private AppCompatImageButton acceptCallBtn;
	private AppCompatImageButton endCallBtn;
	private AppCompatImageButton audioBtn;
	private AppCompatImageButton audioBtnOff;
	private AppCompatImageButton speakerBtn;
	private AppCompatImageButton speakerBtnOff;
	private ImageView avatar;

	//private AudioDeviceSelector audioDeviceSelector; //AM-440

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
		//audioManager.setSpeakerphoneOn(isSpeakerphoneOn); //speaker is never true at this point

		//AM-440 Setup audio device management
//		audioDeviceSelector = new AudioDeviceSelector(getApplicationContext());
//		audioDeviceSelector.start((audioDevices, audioDevice) -> Unit.INSTANCE); //AM-440
//		AudioDevice selectedDevice = audioDeviceSelector.getSelectedAudioDevice();
//		Log.d(Config.LOGTAG, "Selected Device: " + selectedDevice.getName());

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
			if(checkPermissionForCameraAndMicrophone()){
				acceptCall();
			} else {
				requestPermissionForCameraAndMicrophone();
			}
		});
		this.rejectCallBtn= findViewById(R.id.reject_call_button);
		rejectCallBtn.setOnClickListener(v -> {
			SoundPoolManager.getInstance(CallActivity.this).stopRinging();
			SoundPoolManager.getInstance(CallActivity.this).playDisconnect();
			//needs access to XmppConnectionService
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_REJECT_CALL_REQUEST);
			Compatibility.startService(this, intent);
		});


		this.endCallBtn= findViewById(R.id.end_call_button);
		endCallBtn.setOnClickListener(v -> {
			SoundPoolManager.getInstance(CallActivity.this).stopRinging();
			SoundPoolManager.getInstance(CallActivity.this).playDisconnect();
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_CANCEL_CALL_REQUEST);
			Compatibility.startService(this, intent);
		});

		this.audioBtn = findViewById(R.id.audio_image_button);
		this.audioBtnOff = findViewById(R.id.audio_image_button_off);
		this.audioBtnOff.setEnabled(false);
		/*audioBtnOff.setOnClickListener(v -> {
			audioManager.setMicrophoneMute(true);
			audioBtn.setVisibility(View.VISIBLE);
			audioBtnOff.setVisibility(View.GONE);

		});
		audioBtn.setOnClickListener(v -> {
			audioManager.setMicrophoneMute(false);
			audioBtnOff.setVisibility(View.VISIBLE);
			audioBtn.setVisibility(View.GONE);
		});*/


		// TODO in twilio roomActiviy audio manager is used to manage speaker phone status
		this.speakerBtn= findViewById(R.id.speaker_button);
		this.speakerBtnOff= findViewById(R.id.speaker_button_off);
		this.speakerBtnOff.setEnabled(false);

		/*speakerBtnOff.setOnClickListener(v -> {
			audioManager.setSpeakerphoneOn(true);
			speakerBtn.setVisibility(View.VISIBLE);
			speakerBtnOff.setVisibility(View.GONE);
		});
		speakerBtn.setOnClickListener(v -> {
			audioManager.setSpeakerphoneOn(false);
			speakerBtn.setVisibility(View.GONE);
			speakerBtnOff.setVisibility(View.VISIBLE);
		});*/

	}

	public void acceptCall(){
			SoundPoolManager.getInstance(CallActivity.this).stopRinging();
			SoundPoolManager.getInstance(CallActivity.this).playJoin();
			//needs access to XmppConnectionService
			final Intent intent = new Intent(this, XmppConnectionService.class);
			intent.setAction(XmppConnectionService.ACTION_ACCEPT_CALL_REQUEST);
			Compatibility.startService(this, intent);
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
//		audioDeviceSelector.deactivate();
//		audioDeviceSelector.stop(); //AM-440
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
		SoundPoolManager.getInstance(CallActivity.this).playRinging();
//		audioDeviceSelector.activate(); //AM-440
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
		SoundPoolManager.getInstance(CallActivity.this).playRinging();
//		audioDeviceSelector.activate(); //AM-440
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

	@TargetApi(Build.VERSION_CODES.M)
	protected boolean hasStoragePermissions() {
		return (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
				checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
	}

	@TargetApi(Build.VERSION_CODES.M)
	protected void requestPermissions(final int request_code) {
		if (!hasStoragePermissions()) {
			requestPermissions(
					new String[]{
							Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE,
					},
					request_code
			);
		}
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode,
										   @NonNull final String[] permissions,
										   @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		for (int i = 0; i < grantResults.length; i++) {
			if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[i]) ||
					Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
				if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
					acceptCall();
				}
			}
		}
	}



	private boolean checkPermissionForCameraAndMicrophone() {
		int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
		int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
		return resultCamera == PackageManager.PERMISSION_GRANTED &&
				resultMic == PackageManager.PERMISSION_GRANTED;
	}

	private void requestPermissionForCameraAndMicrophone() {
		if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
				ActivityCompat.shouldShowRequestPermissionRationale(this,
						Manifest.permission.RECORD_AUDIO)) {
			Toast.makeText(this,
					R.string.permissions_needed,
					Toast.LENGTH_LONG).show();
		} else {
			ActivityCompat.requestPermissions(
					this,
					new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
					CAMERA_MIC_PERMISSION_REQUEST_CODE);
		}
	}


}
