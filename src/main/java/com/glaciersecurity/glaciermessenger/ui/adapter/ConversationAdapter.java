package com.glaciersecurity.glaciermessenger.ui.adapter;

import androidx.databinding.DataBindingUtil;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ConversationListRowBinding;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.ui.ConversationFragment;
import com.glaciersecurity.glaciermessenger.ui.XmppActivity;
import com.glaciersecurity.glaciermessenger.ui.util.AvatarWorkerTask;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.glaciersecurity.glaciermessenger.utils.EmojiWrapper;
import com.glaciersecurity.glaciermessenger.utils.IrregularUnicodeDetector;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

	private XmppActivity activity;
	private List<Conversation> conversations;
	private OnConversationClickListener listener;

	public ConversationAdapter(XmppActivity activity, List<Conversation> conversations) {
		this.activity = activity;
		this.conversations = conversations;
	}


	@NonNull
	@Override
	public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ConversationViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.conversation_list_row, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ConversationViewHolder viewHolder, int position) {
		Conversation conversation = conversations.get(position);
		if (conversation == null) {
			return;
		}
		CharSequence name = conversation.getName();
		if (name instanceof Jid) {
			viewHolder.binding.conversationName.setText(IrregularUnicodeDetector.style(activity, (Jid) name));
			//HONEYBADGER AM-120 leading # if group and below
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				viewHolder.binding.conversationName.setText("#"+IrregularUnicodeDetector.style(activity, (Jid) name));
			}
		} else {
			viewHolder.binding.conversationName.setText(EmojiWrapper.transform(name));
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				viewHolder.binding.conversationName.setText("#"+EmojiWrapper.transform(name));
			}
		}

		if (conversation == ConversationFragment.getConversation(activity)) {
			viewHolder.binding.frame.setBackgroundColor(StyledAttributes.getColor(activity, R.attr.color_background_tertiary));
		} else {
			viewHolder.binding.frame.setBackgroundColor(StyledAttributes.getColor(activity, R.attr.color_background_primary));
		}

		Message message = conversation.getLatestMessage();
		final int unreadCount = conversation.unreadCount();
		final boolean isRead = conversation.isRead();
		final Conversation.Draft draft = isRead ? conversation.getDraft() : null;
		if (unreadCount > 0) {
			viewHolder.binding.unreadCount.setVisibility(View.VISIBLE);
			viewHolder.binding.unreadCount.setUnreadCount(unreadCount);
		} else {
			viewHolder.binding.unreadCount.setVisibility(View.GONE);
		}

		if (isRead) {
			viewHolder.binding.conversationName.setTypeface(null, Typeface.NORMAL);
		} else {
			viewHolder.binding.conversationName.setTypeface(null, Typeface.BOLD);
		}

		if (draft != null) {
			viewHolder.binding.conversationLastmsgImg.setVisibility(View.GONE);
			viewHolder.binding.conversationLastmsg.setText(EmojiWrapper.transform(draft.getMessage()));
			viewHolder.binding.senderName.setText(R.string.draft);
			viewHolder.binding.senderName.setVisibility(View.VISIBLE);
			viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.NORMAL);
			viewHolder.binding.senderName.setTypeface(null, Typeface.ITALIC);
		} else {
			final boolean fileAvailable = !message.isDeleted();
			final boolean showPreviewText;
			if (fileAvailable && (message.isFileOrImage() || message.treatAsDownloadable() || message.isGeoUri())) {
				final int imageResource;
				if (message.isGeoUri()) {
					imageResource = activity.getThemeResource(R.attr.ic_attach_location, R.drawable.ic_attach_location);
					showPreviewText = false;
				} else {
					final String mime = message.getMimeType();
					switch (mime == null ? "" : mime.split("/")[0]) {
						case "image":
							imageResource = activity.getThemeResource(R.attr.ic_attach_photo, R.drawable.ic_attach_photo);
							showPreviewText = false;
							break;
						case "video":
							imageResource = activity.getThemeResource(R.attr.ic_attach_videocam, R.drawable.ic_attach_videocam);
							showPreviewText = false;
							break;
						case "audio":
							imageResource = activity.getThemeResource(R.attr.ic_attach_record, R.drawable.ic_attach_record);
							showPreviewText = false;
							break;
						default:
							imageResource = activity.getThemeResource(R.attr.ic_attach_document, R.drawable.ic_attach_document);
							showPreviewText = true;
							break;
					}
				}
				viewHolder.binding.conversationLastmsgImg.setImageResource(imageResource);
				viewHolder.binding.conversationLastmsgImg.setVisibility(View.VISIBLE);
			} else {
				viewHolder.binding.conversationLastmsgImg.setVisibility(View.GONE);
				showPreviewText = true;
			}
			final Pair<CharSequence, Boolean> preview = UIHelper.getMessagePreview(activity, message, viewHolder.binding.conversationLastmsg.getCurrentTextColor());
			if (showPreviewText) {
				viewHolder.binding.conversationLastmsg.setText(EmojiWrapper.transform(UIHelper.shorten(preview.first)));
			} else {
				viewHolder.binding.conversationLastmsgImg.setContentDescription(preview.first);
			}
			viewHolder.binding.conversationLastmsg.setVisibility(showPreviewText ? View.VISIBLE : View.GONE);
			if (preview.second) {
				if (isRead) {
					viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.ITALIC);
					viewHolder.binding.senderName.setTypeface(null, Typeface.NORMAL);
				} else {
					viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.BOLD_ITALIC);
					viewHolder.binding.senderName.setTypeface(null, Typeface.BOLD);
				}
			} else {
				if (isRead) {
					viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.NORMAL);
					viewHolder.binding.senderName.setTypeface(null, Typeface.NORMAL);
				} else {
					viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.BOLD);
					viewHolder.binding.senderName.setTypeface(null, Typeface.BOLD);
				}
			}
			if (message.getBody().endsWith(activity.getString(R.string.added_to_group)) ||
					message.getBody().endsWith(activity.getString(R.string.left_group))) { //ALF AM-51
				viewHolder.binding.senderName.setVisibility(View.GONE);
			} else if (message.getStatus() == Message.STATUS_CALL_RECEIVED ||
					message.getStatus() == Message.STATUS_CALL_SENT ||
					message.getStatus() == Message.STATUS_CALL_MISSED) { //ALF AM-421
				viewHolder.binding.senderName.setVisibility(View.GONE);
				//viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.ITALIC); //AM-439
				//AM#10
				if (isRead) {
					viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.ITALIC);
					viewHolder.binding.senderName.setTypeface(null, Typeface.NORMAL);
				} else {
					viewHolder.binding.conversationLastmsg.setTypeface(null, Typeface.BOLD_ITALIC);
					viewHolder.binding.senderName.setTypeface(null, Typeface.BOLD);
				}
			} else if (message.getStatus() == Message.STATUS_RECEIVED) {
				if (conversation.getMode() == Conversation.MODE_MULTI) {
					viewHolder.binding.senderName.setVisibility(View.VISIBLE);
					viewHolder.binding.senderName.setText(UIHelper.getMessageDisplayName(message).split("\\s+")[0] + ':');
				} else {
					viewHolder.binding.senderName.setVisibility(View.GONE);
				}
			} else if (message.getType() != Message.TYPE_STATUS) {
				viewHolder.binding.senderName.setVisibility(View.VISIBLE);
				viewHolder.binding.senderName.setText(activity.getString(R.string.me) + ':');
			} else {
				viewHolder.binding.senderName.setVisibility(View.GONE);
			}
		}

		long muted_till = conversation.getLongAttribute(Conversation.ATTRIBUTE_MUTED_TILL, 0);
		if (muted_till == Long.MAX_VALUE) {
			viewHolder.binding.notificationStatus.setVisibility(View.VISIBLE);
			int ic_notifications_off = activity.getThemeResource(R.attr.icon_notifications_off, R.drawable.ic_notifications_off_black_24dp);
			viewHolder.binding.notificationStatus.setImageResource(ic_notifications_off);
		} else if (muted_till >= System.currentTimeMillis()) {
			viewHolder.binding.notificationStatus.setVisibility(View.VISIBLE);
			int ic_notifications_paused = activity.getThemeResource(R.attr.icon_notifications_paused, R.drawable.ic_notifications_paused_black_24dp);
			viewHolder.binding.notificationStatus.setImageResource(ic_notifications_paused);
		} else if (conversation.alwaysNotify()) {
			viewHolder.binding.notificationStatus.setVisibility(View.GONE);
		} else {
			viewHolder.binding.notificationStatus.setVisibility(View.VISIBLE);
			int ic_notifications_none = activity.getThemeResource(R.attr.icon_notifications_none, R.drawable.ic_notifications_none_black_24dp);
			viewHolder.binding.notificationStatus.setImageResource(ic_notifications_none);
		}

		long timestamp;
		if (draft != null) {
			timestamp = draft.getTimestamp();
		} else {
			timestamp = conversation.getLatestMessage().getTimeSent();
		}
		viewHolder.binding.conversationLastupdate.setText(UIHelper.readableTimeDifference(activity, timestamp));
		AvatarWorkerTask.loadAvatar(conversation, viewHolder.binding.conversationImage, R.dimen.avatar_on_conversation_overview);
		viewHolder.itemView.setOnClickListener(v -> listener.onConversationClick(v, conversation));
	}

	@Override
	public int getItemCount() {
		return conversations.size();
	}

	public void setConversationClickListener(OnConversationClickListener listener) {
		this.listener = listener;
	}


	public void insert(Conversation c, int position) {
		conversations.add(position, c);
		notifyDataSetChanged();
	}

	public void remove(Conversation conversation, int position) {
		conversations.remove(conversation);
		notifyItemRemoved(position);
	}

	public interface OnConversationClickListener {
		void onConversationClick(View view, Conversation conversation);
	}

	static class ConversationViewHolder extends RecyclerView.ViewHolder {
		private final ConversationListRowBinding binding;

		private ConversationViewHolder(ConversationListRowBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}

	}

}
