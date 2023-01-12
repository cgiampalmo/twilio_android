package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;


import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.DialogSmsNameBinding;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.OnSMSNameClickListener;
import com.glaciersecurity.glaciermessenger.ui.OnSMSProfileClickListener;
import com.glaciersecurity.glaciermessenger.ui.OnSMSRemoveClickListener;
import com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView;

import java.util.ArrayList;

import static android.content.Context.CLIPBOARD_SERVICE;

public class SmsProfileAdapter extends RecyclerView.Adapter<SmsProfileAdapter.SMSRecyclerViewHolder> {

	public ArrayList<SmsProfile> smsProfileList = new ArrayList<>();
	private OnSMSProfileClickListener listener;
	private OnSMSRemoveClickListener removeListener;
	private OnSMSNameClickListener nameListener;
	private ViewGroup viewGroup;
	public SmsProfile selectedProfile;
	public Context mContext;
	private String identity;


	public SmsProfileAdapter(Context mContext, String identity, OnSMSProfileClickListener listener, OnSMSRemoveClickListener removeListener, OnSMSNameClickListener nameListener, ArrayList<SmsProfile> smsProfileList) {
		this.smsProfileList = smsProfileList;
		this.removeListener = removeListener;
		this.listener = listener;
		this.nameListener = nameListener;
		this.mContext = mContext;
		this.identity = identity;
	}

	@NonNull
	@Override
	public SMSRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_profile, parent, false);
		viewGroup = parent;
		return new SMSRecyclerViewHolder(view, listener);
	}

	@Override
	public void onBindViewHolder(@NonNull SMSRecyclerViewHolder holder, @SuppressLint("RecyclerView") int position) {
		SmsProfile smsProf = smsProfileList.get(position);
		selectedProfile = smsProf;
		holder.primaryView.setText(smsProf.getFormattedNumber());
		if (smsProf.getNickname() == null || smsProf.getNickname().isEmpty()) {
			holder.secondaryView.setVisibility(View.GONE);
		} else {
			holder.secondaryView.setVisibility(View.VISIBLE);
			holder.secondaryView.setText(smsProf.getFormattedNumber());
			holder.primaryView.setText(smsProf.getNickname());

		}
		holder.profileView.setBackgroundColor(smsProf.getColor());
		if(smsProf.getUnread_count() != null && smsProf.getUnread_count() > 0) {
			holder.unreadCount.setUnreadCount(smsProf.getUnread_count());
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
                builder.setPositiveButton("Release",  new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
								removeListener.OnSMSRemoveClick(smsProf);
							}
						}
				);
				builder.setNegativeButton( "Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();

			}
		});
		holder.nameNumBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(view.getContext());
				builder.setNegativeButton( "No thanks", null);
				builder.setTitle("Display Name");
				builder.setCancelable(true);

				DialogSmsNameBinding binding = DataBindingUtil.inflate(LayoutInflater.from(view.getContext()), R.layout.dialog_sms_name, null, false);

				builder.setView(binding.getRoot());
				builder.setPositiveButton("Add name", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								String smsNickname = binding.smsNickname.getText().toString();
								dialog.dismiss();

								nameListener.OnSMSNameClick(smsNickname, smsProf);
							}
						}
					);
				android.app.AlertDialog dialog = builder.create();
				binding.smsNickname.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
						boolean handled = false;
						if (actionId == EditorInfo.IME_ACTION_DONE) {
							handled = true;
							String smsNickname = binding.smsNickname.getText().toString();
							dialog.dismiss();

							nameListener.OnSMSNameClick(smsNickname, smsProf);
						}
						return handled;
					}
				});
				dialog.show();

			}
		});

	}

	@Override
	public int getItemCount() {
		return smsProfileList.size();
	}




	public void toggleDeleteVisible(){
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View v = viewGroup.getChildAt(i);
				ImageButton removeNumBtn = v.findViewById(R.id.remove_sms_btn);
				if (removeNumBtn.getVisibility() == View.VISIBLE) {
					removeNumBtn.setVisibility(View.GONE);
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
					nameNumBtn.setVisibility(View.GONE);
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
				removeNumBtn.setVisibility(View.GONE);
			}
		}
	}

	public void toggleNameOff(){
		if (viewGroup != null) {
			for (int i = 0; i < viewGroup.getChildCount(); i++) {
				View v = viewGroup.getChildAt(i);
				ImageButton addNumBtn = v.findViewById(R.id.name_sms_btn);
				addNumBtn.setVisibility(View.GONE);
			}
		}
	}

	public class SMSRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		public final View profView;
		TextView primaryView;
		TextView secondaryView;
		//TextView locationView;
		UnreadCountCustomView unreadCount;
		ImageButton removeNumBtn;
		ImageButton nameNumBtn;
		RelativeLayout profileView;
		private OnSMSProfileClickListener listener;



		public SMSRecyclerViewHolder(View view, OnSMSProfileClickListener listener){
			super(view);
			this.listener = listener;
			profileView = (RelativeLayout) view.findViewById(R.id.sms_profile_view);
			primaryView = (TextView) view.findViewById(R.id.sms_profile_primary_view);
			secondaryView = (TextView) view.findViewById(R.id.sms_profile_secondary_view);
			unreadCount = (UnreadCountCustomView) view.findViewById(R.id.sms_unread_count);
			removeNumBtn = view.findViewById(R.id.remove_sms_btn);
			nameNumBtn = view.findViewById(R.id.name_sms_btn);
			profView = view;
			profView.setOnClickListener(this);
			profileView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("Glacier sms:", getNumber());
					clipboard.setPrimaryClip(clip);
					Toast.makeText(mContext,"Copied to clipboard: "+ getNumber(), Toast.LENGTH_SHORT).show();
					return false;
				}
			});
		}

		private String getNumber(){
			if (secondaryView.getVisibility() == View.GONE){
				return primaryView.getText().toString();
			} else {
				return secondaryView.getText().toString();
			}
		}


		@Override
		public void onClick(View view) {
			listener.OnSMSProfileClick(getNumber(), getNumber());
		}
	}


}