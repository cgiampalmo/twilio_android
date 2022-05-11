package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;

import java.util.ArrayList;

public class SmsProfileAdapter extends RecyclerView.Adapter<SmsProfileAdapter.SMSRecyclerViewHolder> {

	ArrayList<SmsProfile> smsProfileList = new ArrayList<>();

	public SmsProfileAdapter(ArrayList<SmsProfile> smsProfileList) {
		this.smsProfileList = smsProfileList;
	}

	@NonNull
	@Override
	public SMSRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_profile, parent, false);
		SMSRecyclerViewHolder smsViewHolder = new SMSRecyclerViewHolder(view);
		return smsViewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull SMSRecyclerViewHolder holder, int position) {
		holder.numberView.setText(smsProfileList.get(position).getNumber());
		holder.locationView.setText(smsProfileList.get(position).getLocation());
		holder.profileView.setBackgroundColor(smsProfileList.get(position).getColor());
	}

	@Override
	public int getItemCount() {
		return smsProfileList.size();
	}

	public static class SMSRecyclerViewHolder extends RecyclerView.ViewHolder{

		TextView numberView;
		TextView locationView;
		LinearLayout profileView;
		public SMSRecyclerViewHolder(View view){
			super(view);
			profileView = (LinearLayout) view.findViewById(R.id.sms_profile_view);
			numberView = (TextView) view.findViewById(R.id.sms_profile_number);
			locationView = (TextView) view.findViewById(R.id.sms_profile_location);
		}
	}
}