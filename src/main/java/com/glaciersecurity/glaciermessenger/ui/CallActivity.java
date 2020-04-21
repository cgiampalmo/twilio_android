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
import androidx.databinding.DataBindingUtil;

import com.glaciersecurity.glaciermessenger.R;

public class CallActivity extends AppCompatActivity {

	//private ImageView mAvatar;
	private TextView callState;
	private TextView rejectText;
	private LinearLayout acceptCall;
	private LinearLayout endCall;
	private Button rejectCallBtn;
	private Button acceptCallBtn;



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
		acceptCallBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//acceptCall
			}
		});
		this.rejectCallBtn= findViewById(R.id.reject_call_button);
		rejectCallBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//acceptCall
			}
		});

	}

	private void onIncomingCall(){
		callState.setText(getResources().getString(R.string.incoming_call));
		acceptCall.setVisibility(View.VISIBLE);
		endCall.setVisibility(View.VISIBLE);
		callState.setText(getResources().getString(R.string.incoming_call));
		rejectText.setText(getResources().getString(R.string.reject_call));

	}

	private void onOutgoingCall(){
		callState.setText(getResources().getString(R.string.outgoing_call));
		acceptCall.setVisibility(View.GONE);
		endCall.setVisibility(View.VISIBLE);
		rejectText.setText(getResources().getString(R.string.end_call));

	}

}
