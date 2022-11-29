package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.OnSMSNameClickListener;
import com.glaciersecurity.glaciermessenger.ui.OnSMSProfileClickListener;
import com.glaciersecurity.glaciermessenger.ui.OnSMSRemoveClickListener;
import com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView;

import java.util.ArrayList;

public class SmsProfileAdapter extends RecyclerView.Adapter<SmsProfileAdapter.SMSRecyclerViewHolder> implements OnSMSProfileClickListener, OnSMSRemoveClickListener, OnSMSNameClickListener {

	public ArrayList<SmsProfile> smsProfileList = new ArrayList<>();
	private OnSMSProfileClickListener listener;
	private OnSMSRemoveClickListener removeListener;
	private OnSMSNameClickListener nameListener;
	private ViewGroup viewGroup;
	public SmsProfile selectedSMSforRemoval;
	public SmsProfile selectedSMSforName;

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
		return new SMSRecyclerViewHolder(view, listener, removeListener, nameListener);
	}

	private String nickname = "";


	@Override
	public void onBindViewHolder(@NonNull SMSRecyclerViewHolder holder, @SuppressLint("RecyclerView") int position) {
		holder.numberView.setText(smsProfileList.get(position).getFormattedNumber());
		holder.profileView.setBackgroundColor(smsProfileList.get(position).getColor());
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
                builder.setMessage("Do you want to release number " + smsProfileList.get(position).getFormattedNumber() + "?");
                builder.setTitle("Confirmation");
                builder.setCancelable(true);
                builder.setPositiveButton("Release", removeListener);
				builder.setNegativeButton( "Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
				selectedSMSforRemoval = smsProfileList.get(position);
			}
		});
		holder.nameNumBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
				final EditText input = new EditText(builder.getContext());

				input.setInputType(InputType.TYPE_CLASS_TEXT );
				input.setHint("");
				builder.setView(input);
				builder.setTitle("Display Name");
				builder.setCancelable(true);
				builder.setPositiveButton("Add name", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						nickname = input.getText().toString();
					}
				});
				builder.setNegativeButton( "No thanks", null);
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
				selectedSMSforName = smsProfileList.get(position);
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
	@Override
	public void OnSMSNameClick(String conv_name) {
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

	public void toggleNameVisible(){
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View v = viewGroup.getChildAt(i);
				ImageButton nameNumBtn = v.findViewById(R.id.name_sms_btn);
				if (nameNumBtn.getVisibility() == View.VISIBLE) {
					nameNumBtn.setVisibility(View.INVISIBLE);
				} else {
					nameNumBtn.setVisibility(View.VISIBLE);
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

	public void toggleNameOff(){
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View v = viewGroup.getChildAt(i);
				ImageButton removeNumBtn = v.findViewById(R.id.name_sms_btn);
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
		ImageButton nameNumBtn;
		LinearLayout profileView;
		private OnSMSProfileClickListener listener;
		private OnSMSRemoveClickListener removeListener;
		private OnSMSNameClickListener nameListener;



		public SMSRecyclerViewHolder(View view, OnSMSProfileClickListener listener, OnSMSRemoveClickListener removeListener, OnSMSNameClickListener nameListener){
			super(view);
			this.listener = listener;
			this.removeListener = removeListener;
			this.nameListener = nameListener;
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
			nameNumBtn = view.findViewById(R.id.name_sms_btn);
			nameNumBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					nameListener.OnSMSNameClick(numberView.getText().toString());
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