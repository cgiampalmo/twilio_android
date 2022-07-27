package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.ChangePasswordActivity;
import com.glaciersecurity.glaciermessenger.ui.OnSMSProfileClickListener;
import com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.twilio.conversations.Conversation;

import java.util.ArrayList;
import java.util.List;

public class SmsProfileAdapter extends RecyclerView.Adapter<SmsProfileAdapter.SMSRecyclerViewHolder> implements OnSMSProfileClickListener {

	private ArrayList<SmsProfile> smsProfileList = new ArrayList<>();
	private OnSMSProfileClickListener listener;

	public SmsProfileAdapter(OnSMSProfileClickListener listener, ArrayList<SmsProfile> smsProfileList) {
		this.smsProfileList = smsProfileList;
		this.listener = listener;
	}

	@NonNull
	@Override
	public SMSRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_profile, parent, false);
		return new SMSRecyclerViewHolder(view, listener);
	}

	@Override
	public void onBindViewHolder(@NonNull SMSRecyclerViewHolder holder, int position) {
		holder.numberView.setText(smsProfileList.get(position).getNumber());
		//holder.locationView.setText(smsProfileList.get(position).getLocation());
		holder.profileView.setBackgroundColor(UIHelper.getColorForSMS(smsProfileList.get(position).getNumber()));
		Log.d("Glacier","unread_conv_count----"+smsProfileList.get(position).getUnread_count()+"---"+smsProfileList.get(position).getNumber());
		if(smsProfileList.get(position).getUnread_count() != null && smsProfileList.get(position).getUnread_count() > 0) {
			holder.unreadCount.setUnreadCount(smsProfileList.get(position).getUnread_count());
			holder.unreadCount.setVisibility(View.VISIBLE);
		}
		SmsProfile smsPro = smsProfileList.get(holder.getAdapterPosition());

	}

	@Override
	public int getItemCount() {
		return smsProfileList.size();
	}

	@Override
	public void OnSMSProfileClick(String id, String number) {
		Log.e("onClick", " -- profile clicked2");

	}

	public class SMSRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		final View profView;
		TextView numberView;
		//TextView locationView;
		UnreadCountCustomView unreadCount;
		ImageButton removeNumBtn;
		LinearLayout profileView;
		private OnSMSProfileClickListener listener;


		public SMSRecyclerViewHolder(View view, OnSMSProfileClickListener listener){
			super(view);
			this.listener = listener;
			profileView = (LinearLayout) view.findViewById(R.id.sms_profile_view);
			numberView = (TextView) view.findViewById(R.id.sms_profile_number);
			//locationView = (TextView) view.findViewById(R.id.sms_profile_location);
			unreadCount = (UnreadCountCustomView) view.findViewById(R.id.sms_unread_count);
			removeNumBtn = view.findViewById(R.id.remove_sms_btn);
			profView = view;
			profView.setOnClickListener(this);
		}

		public void setProfileClickListener(OnSMSProfileClickListener listener) {
			this.listener = listener;
		}


		@Override
		public void onClick(View view) {
			SmsProfile smsProf = smsProfileList.get(getAdapterPosition());
			listener.OnSMSProfileClick(numberView.getText().toString(),numberView.getText().toString());
		}
	}
}