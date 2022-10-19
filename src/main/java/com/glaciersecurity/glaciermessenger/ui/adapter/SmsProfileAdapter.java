package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.content.DialogInterface;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.ChangePasswordActivity;
import com.glaciersecurity.glaciermessenger.ui.OnSMSProfileClickListener;
import com.glaciersecurity.glaciermessenger.ui.OnSMSRemoveClickListener;
import com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.google.android.material.transition.Hold;
import com.twilio.conversations.Conversation;

import java.util.ArrayList;
import java.util.List;

public class SmsProfileAdapter extends RecyclerView.Adapter<SmsProfileAdapter.SMSRecyclerViewHolder> implements OnSMSProfileClickListener, OnSMSRemoveClickListener {

	public ArrayList<SmsProfile> smsProfileList = new ArrayList<>();
	private OnSMSProfileClickListener listener;
	private OnSMSRemoveClickListener removeListener;
	private ViewGroup viewGroup;
	public SmsProfile selectedSMSforRemoval;

	public SmsProfileAdapter(OnSMSProfileClickListener listener, OnSMSRemoveClickListener removeListener, ArrayList<SmsProfile> smsProfileList) {
		this.smsProfileList = smsProfileList;
		this.removeListener = removeListener;
		this.listener = listener;
	}

	@NonNull
	@Override
	public SMSRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_profile, parent, false);
		viewGroup = parent;
		return new SMSRecyclerViewHolder(view, listener, removeListener);
	}

	@Override
	public void onBindViewHolder(@NonNull SMSRecyclerViewHolder holder, int position) {
		holder.numberView.setText(smsProfileList.get(position).getNumber());
		//holder.locationView.setText(smsProfileList.get(position).getLocation());
		holder.profileView.setBackgroundColor(UIHelper.getColorForSMS(smsProfileList.get(position).getNumber()));
		//Log.d("Glacier","unread_conv_count----"+smsProfileList.get(position).getUnread_count()+"---"+smsProfileList.get(position).getNumber());
		if(smsProfileList.get(position).getUnread_count() != null && smsProfileList.get(position).getUnread_count() > 0) {
			holder.unreadCount.setUnreadCount(smsProfileList.get(position).getUnread_count());
			holder.unreadCount.setVisibility(View.VISIBLE);
		}else{
			holder.unreadCount.setVisibility(View.INVISIBLE);
		}
		holder.removeNumBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage("Do you want to release number " + smsProfileList.get(position).getNumber() + "?");
                builder.setTitle("Confirmation");
                builder.setCancelable(true);
                builder.setPositiveButton("Release", removeListener);
				builder.setNegativeButton( "Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
				selectedSMSforRemoval = smsProfileList.get(position);
			}
		});

	}

	@Override
	public int getItemCount() {
		return smsProfileList.size();
	}

	@Override
	public void OnSMSProfileClick(String id, String number) {

	}
	@Override
	public void OnSMSRemoveClick(String conv_name) {


	}


	public void toggleDeleteVisible(){
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View v = viewGroup.getChildAt(i);
				ImageButton removeNumBtn = v.findViewById(R.id.remove_sms_btn);
				if (removeNumBtn.getVisibility() == View.VISIBLE) {
					removeNumBtn.setVisibility(View.INVISIBLE);
				} else {
					removeNumBtn.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	public void toggleDeleteOff(){
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View v = viewGroup.getChildAt(i);
				ImageButton removeNumBtn = v.findViewById(R.id.remove_sms_btn);
				removeNumBtn.setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

	}

	public class SMSRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		public final View profView;
		TextView numberView;
		//TextView locationView;
		UnreadCountCustomView unreadCount;
		ImageButton removeNumBtn;
		LinearLayout profileView;
		private OnSMSProfileClickListener listener;
		private OnSMSRemoveClickListener removeListener;


		public SMSRecyclerViewHolder(View view, OnSMSProfileClickListener listener, OnSMSRemoveClickListener removeListener){
			super(view);
			this.listener = listener;
			this.removeListener = removeListener;
			profileView = (LinearLayout) view.findViewById(R.id.sms_profile_view);
			numberView = (TextView) view.findViewById(R.id.sms_profile_number);
			//locationView = (TextView) view.findViewById(R.id.sms_profile_location);
			unreadCount = (UnreadCountCustomView) view.findViewById(R.id.sms_unread_count);
			removeNumBtn = view.findViewById(R.id.remove_sms_btn);
			removeNumBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					removeListener.OnSMSRemoveClick(numberView.getText().toString());
				}
			});
			profView = view;
			profView.setOnClickListener(this);
		}


		@Override
		public void onClick(View view) {
			SmsProfile smsProf = smsProfileList.get(getAdapterPosition());
			listener.OnSMSProfileClick(numberView.getText().toString(),numberView.getText().toString());
		}
	}
}