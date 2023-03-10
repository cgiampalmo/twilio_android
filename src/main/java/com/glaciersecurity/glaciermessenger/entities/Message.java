package com.glaciersecurity.glaciermessenger.entities;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import com.glaciersecurity.glaciermessenger.utils.Log;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.FingerprintStatus;
import com.glaciersecurity.glaciermessenger.services.AvatarService;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.utils.Emoticons;
import com.glaciersecurity.glaciermessenger.utils.GeoHelper;
import com.glaciersecurity.glaciermessenger.utils.MessageUtils;
import com.glaciersecurity.glaciermessenger.utils.MimeUtils;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;

import org.json.JSONException;

import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class Message extends AbstractEntity implements AvatarService.Avatarable {

	public static final String TABLENAME = "messages";

	public static final int STATUS_RECEIVED = 0;
	public static final int STATUS_UNSEND = 1;
	public static final int STATUS_SEND = 2;
	public static final int STATUS_SEND_FAILED = 3;
	public static final int STATUS_WAITING = 5;
	public static final int STATUS_OFFERED = 6;
	public static final int STATUS_SEND_RECEIVED = 7;
	public static final int STATUS_SEND_DISPLAYED = 8;

	//ALF AM-421
	public static final int STATUS_CALL_RECEIVED = 9;
	public static final int STATUS_CALL_SENT = 10;
	public static final int STATUS_CALL_MISSED = 11;

	public static final int ENCRYPTION_NONE = 0;
	public static final int ENCRYPTION_PGP = 1;
	public static final int ENCRYPTION_OTR = 2;
	public static final int ENCRYPTION_DECRYPTED = 3;
	public static final int ENCRYPTION_DECRYPTION_FAILED = 4;
	public static final int ENCRYPTION_AXOLOTL = 5;
	public static final int ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE = 6;
	public static final int ENCRYPTION_AXOLOTL_FAILED = 7;

	public static final int TYPE_TEXT = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_FILE = 2;
	public static final int TYPE_STATUS = 3;
	public static final int TYPE_PRIVATE = 4;

	//ALF AM-53
	public static final int TIMER_NONE = 0;

	public static final String CONVERSATION = "conversationUuid";
	public static final String COUNTERPART = "counterpart";
	public static final String TRUE_COUNTERPART = "trueCounterpart";
	public static final String BODY = "body";
	public static final String TIME_SENT = "timeSent";
	public static final String ENCRYPTION = "encryption";
	public static final String STATUS = "status";
	public static final String TYPE = "type";
	public static final String TIMER = "timer"; //ALF AM-53
	public static final String ENDTIME = "endtime"; //ALF AM-53
	public static final String CARBON = "carbon";
	public static final String OOB = "oob";
	public static final String EDITED = "edited";
	public static final String REMOTE_MSG_ID = "remoteMsgId";
	public static final String SERVER_MSG_ID = "serverMsgId";
	public static final String RELATIVE_FILE_PATH = "relativeFilePath";
	public static final String FINGERPRINT = "axolotl_fingerprint";
	public static final String READ = "read";
	public static final String ERROR_MESSAGE = "errorMsg";
	public static final String READ_BY_MARKERS = "readByMarkers";
	public static final String MARKABLE = "markable";
	public static final String DELETED = "deleted";
	public static final String ME_COMMAND = "/me ";

	public static final String ERROR_MESSAGE_CANCELLED = "com.glaciersecurity.glaciermessenger.cancelled";

	public boolean markable = false;
	protected String conversationUuid;
	protected Jid counterpart;
	protected Jid trueCounterpart;
	protected String body;
	protected String encryptedBody;
	protected long timeSent;
	protected int encryption;
	protected int status;
	protected int type;
	protected boolean deleted = false;
	protected int timer; //ALF AM-53
	protected long endTime; //ALF AM-53
	protected boolean carbon = false;
	protected boolean oob = false;
	protected List<Edited> edits = new ArrayList<>();
	protected String relativeFilePath;
	protected boolean read = true;
	protected String remoteMsgId = null;
	protected String serverMsgId = null;
	private final Conversational conversation;
	protected Transferable transferable = null;
	private Message mNextMessage = null;
	private Message mPreviousMessage = null;
	private String axolotlFingerprint = null;
	private String errorMessage = null;
	private Set<ReadByMarker> readByMarkers = new HashSet<>();

	private Boolean isGeoUri = null;
	private Boolean isEmojisOnly = null;
	private Boolean treatAsDownloadable = null;
	private FileParams fileParams = null;
	private List<MucOptions.User> counterparts;
	private WeakReference<MucOptions.User> user;

	protected Message(Conversational conversation) {
		this.conversation = conversation;
		this.conversationUuid = conversation.getUuid(); //ALF AM-53
	}

	public Message(Conversational conversation, String body, int encryption) {
		this(conversation, body, encryption, STATUS_UNSEND);
	}

	public Message(Conversational conversation, String body, int encryption, int status) {
		this(conversation, java.util.UUID.randomUUID().toString(),
				conversation.getUuid(),
				conversation.getJid() == null ? null : conversation.getJid().asBareJid(),
				null,
				body,
				System.currentTimeMillis(),
				encryption,
				status,
				TYPE_TEXT,
				TIMER_NONE, //ALF AM-53
				Long.MAX_VALUE, //ALF AM-53 (for end time)
				false,
				null,
				null,
				null,
				null,
				true,
				null,
				false,
				null,
				null,
				false,
				false);

		//ALF AM-53 moved from constructor below because was overwriting database
		//AM#9 removed status != STATUS_RECEIVED
		if (conversation.getMode() == Conversation.MODE_SINGLE && this.timer == TIMER_NONE) {
			if (conversation.getTimer() > 0) {
				this.setTimer(conversation.getTimer());
			} else if (conversation.getAccount().getTimer() > 0) {
				this.setTimer(conversation.getAccount().getTimer());
			}
		}
	}

	protected Message(final Conversational conversation, final String uuid, final String conversationUUid, final Jid counterpart,
	                final Jid trueCounterpart, final String body, final long timeSent,
	                final int encryption, final int status, final int type, final int timer, final long endtime, final boolean carbon,
	                final String remoteMsgId, final String relativeFilePath,
	                final String serverMsgId, final String fingerprint, final boolean read,
	                final String edited, final boolean oob, final String errorMessage, final Set<ReadByMarker> readByMarkers,
	                final boolean markable, final boolean deleted) {
		this.conversation = conversation;
		this.uuid = uuid;
		this.conversationUuid = conversationUUid;
		this.counterpart = counterpart;
		this.trueCounterpart = trueCounterpart;
		this.body = body == null ? "" : body;
		this.timeSent = timeSent;
		this.encryption = encryption;
		this.status = status;
		this.type = type;
		this.timer = timer; //ALF AM-53 (and in params list above)
		this.endTime = endtime; //ALF AM-53
		this.carbon = carbon;
		this.remoteMsgId = remoteMsgId;
		this.relativeFilePath = relativeFilePath;
		this.serverMsgId = serverMsgId;
		this.axolotlFingerprint = fingerprint;
		this.read = read;
		this.edits = Edited.fromJson(edited);
		this.oob = oob;
		this.errorMessage = errorMessage;
		this.readByMarkers = readByMarkers == null ? new HashSet<ReadByMarker>() : readByMarkers;
		this.markable = markable;
		this.deleted = deleted;
	}

	public static Message fromCursor(Cursor cursor, Conversation conversation) {
		Jid jid;
		try {
			String value = cursor.getString(cursor.getColumnIndex(COUNTERPART));
			if (value != null) {
				jid = Jid.of(value);
			} else {
				jid = null;
			}
		} catch (IllegalArgumentException e) {
			jid = null;
		} catch (IllegalStateException e) {
			return null; // message too long?
		}
		Jid trueCounterpart;
		try {
			String value = cursor.getString(cursor.getColumnIndex(TRUE_COUNTERPART));
			if (value != null) {
				trueCounterpart = Jid.of(value);
			} else {
				trueCounterpart = null;
			}
		} catch (IllegalArgumentException e) {
			trueCounterpart = null;
		}
		return new Message(conversation,
				cursor.getString(cursor.getColumnIndex(UUID)),
				cursor.getString(cursor.getColumnIndex(CONVERSATION)),
				jid,
				trueCounterpart,
				cursor.getString(cursor.getColumnIndex(BODY)),
				cursor.getLong(cursor.getColumnIndex(TIME_SENT)),
				cursor.getInt(cursor.getColumnIndex(ENCRYPTION)),
				cursor.getInt(cursor.getColumnIndex(STATUS)),
				cursor.getInt(cursor.getColumnIndex(TYPE)),
				cursor.getInt(cursor.getColumnIndex(TIMER)), //ALF AM-53
				cursor.getLong(cursor.getColumnIndex(ENDTIME)), //ALF AM-53
				cursor.getInt(cursor.getColumnIndex(CARBON)) > 0,
				cursor.getString(cursor.getColumnIndex(REMOTE_MSG_ID)),
				cursor.getString(cursor.getColumnIndex(RELATIVE_FILE_PATH)),
				cursor.getString(cursor.getColumnIndex(SERVER_MSG_ID)),
				cursor.getString(cursor.getColumnIndex(FINGERPRINT)),
				cursor.getInt(cursor.getColumnIndex(READ)) > 0,
				cursor.getString(cursor.getColumnIndex(EDITED)),
				cursor.getInt(cursor.getColumnIndex(OOB)) > 0,
				cursor.getString(cursor.getColumnIndex(ERROR_MESSAGE)),
				ReadByMarker.fromJsonString(cursor.getString(cursor.getColumnIndex(READ_BY_MARKERS))),
				cursor.getInt(cursor.getColumnIndex(MARKABLE)) > 0,
				cursor.getInt(cursor.getColumnIndex(DELETED)) > 0);
	}

	public static Message createStatusMessage(Conversation conversation, String body) {
		final Message message = new Message(conversation);
		message.setType(Message.TYPE_STATUS);
		message.setStatus(Message.STATUS_RECEIVED);
		message.body = body;
		return message;
	}

	public static Message createLoadMoreMessage(Conversation conversation) {
		final Message message = new Message(conversation);
		message.setType(Message.TYPE_STATUS);
		message.body = "LOAD_MORE";
		return message;
	}

	//ALF AM-51
	public static Message createGroupChangedSeparator(Message message) {
		final Message separator = new Message(message.getConversation());
		separator.setType(Message.TYPE_STATUS);
		separator.body = message.getBody();
		separator.setTime(message.getTimeSent());
		if (message.isRead()) { separator.markRead(); }
		return separator;
	}

	//ALF AM-421
	public static Message createCallStatusMessage(Conversation conversation, int status) {
		final Message separator = new Message(conversation);
		separator.setType(Message.TYPE_STATUS);
		separator.setStatus(status); //AM-439
		if (status == Message.STATUS_CALL_RECEIVED) {
			separator.body = "Call received";
		} else if (status == Message.STATUS_CALL_SENT) {
			separator.body = "You called";
		} else if (status == Message.STATUS_CALL_MISSED) {
			separator.body = "Missed call";
			separator.uuid = java.util.UUID.randomUUID().toString(); //AM#10 might mess up other things
		}
		separator.setTime(System.currentTimeMillis());
		//if (message.isRead()) { separator.markRead(); }
		return separator;
	}

	//ALF AM-228b
	public static Message createNotForThisDeviceSeparator(Message message, String body) {
		final Message separator = new Message(message.getConversation());
		separator.setType(Message.TYPE_STATUS);
		separator.body = body;
		separator.setTime(message.getTimeSent());
		if (message.isRead()) { separator.markRead(); }
		return separator;
	}

	@Override
	public ContentValues getContentValues() {
		ContentValues values = new ContentValues();
		values.put(UUID, uuid);
		values.put(CONVERSATION, conversationUuid);
		if (counterpart == null) {
			values.putNull(COUNTERPART);
		} else {
			values.put(COUNTERPART, counterpart.toString());
		}
		if (trueCounterpart == null) {
			values.putNull(TRUE_COUNTERPART);
		} else {
			values.put(TRUE_COUNTERPART, trueCounterpart.toString());
		}
		values.put(BODY, body.length() > Config.MAX_STORAGE_MESSAGE_CHARS ? body.substring(0, Config.MAX_STORAGE_MESSAGE_CHARS) : body);
		values.put(TIME_SENT, timeSent);
		values.put(ENCRYPTION, encryption);
		values.put(STATUS, status);
		values.put(TYPE, type);
		values.put(TIMER, timer); //ALF AM-53
		values.put(ENDTIME, endTime); //ALF AM-53
		values.put(CARBON, carbon ? 1 : 0);
		values.put(REMOTE_MSG_ID, remoteMsgId);
		values.put(RELATIVE_FILE_PATH, relativeFilePath);
		values.put(SERVER_MSG_ID, serverMsgId);
		values.put(FINGERPRINT, axolotlFingerprint);
		values.put(READ, read ? 1 : 0);
		try {
			values.put(EDITED, Edited.toJson(edits));
		} catch (JSONException e) {
			Log.e(Config.LOGTAG,"error persisting json for edits",e);
		}
		values.put(OOB, oob ? 1 : 0);
		values.put(ERROR_MESSAGE, errorMessage);
		values.put(READ_BY_MARKERS, ReadByMarker.toJson(readByMarkers).toString());
		values.put(MARKABLE, markable ? 1 : 0);
		values.put(DELETED, deleted ? 1 : 0);
		return values;
	}

	public String getConversationUuid() {
		return conversationUuid;
	}

	public Conversational getConversation() {
		return this.conversation;
	}

	public Jid getCounterpart() {
		return counterpart;
	}

	public void setCounterpart(final Jid counterpart) {
		this.counterpart = counterpart;
	}

	@Override
	public int getAvatarBackgroundColor() {
		if (type == Message.TYPE_STATUS && getCounterparts() != null && getCounterparts().size() > 1) {
			return Color.TRANSPARENT;
		} else {
			return UIHelper.getColorForName(UIHelper.getMessageDisplayName(this));
		}
	}

	public Contact getContact() {
		if (this.conversation.getMode() == Conversation.MODE_SINGLE) {
			return this.conversation.getContact();
		} else {
			if (this.trueCounterpart == null) {
				return null;
			} else {
				return this.conversation.getAccount().getRoster()
						.getContactFromContactList(this.trueCounterpart);
			}
		}
	}

	public String getBody() {
		return body;
	}

	public synchronized void setBody(String body) {
		if (body == null) {
			throw new Error("You should not set the message body to null");
		}
		this.body = body;
		this.isGeoUri = null;
		this.isEmojisOnly = null;
		this.treatAsDownloadable = null;
		this.fileParams = null;
	}

	public void setMucUser(MucOptions.User user) {
		this.user = new WeakReference<>(user);
	}

	public boolean sameMucUser(Message otherMessage) {
		final MucOptions.User thisUser = this.user == null ? null : this.user.get();
		final MucOptions.User otherUser = otherMessage.user == null ? null : otherMessage.user.get();
		return thisUser != null && thisUser == otherUser;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean setErrorMessage(String message) {
		boolean changed = (message != null && !message.equals(errorMessage))
				|| (message == null && errorMessage != null);
		this.errorMessage = message;
		return changed;
	}

	public long getTimeSent() {
		return timeSent;
	}

	public int getEncryption() {
		return encryption;
	}

	public void setEncryption(int encryption) {
		this.encryption = encryption;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getRelativeFilePath() {
		return this.relativeFilePath;
	}

	public void setRelativeFilePath(String path) {
		this.relativeFilePath = path;
	}

	public String getRemoteMsgId() {
		return this.remoteMsgId;
	}

	public void setRemoteMsgId(String id) {
		this.remoteMsgId = id;
	}

	public String getServerMsgId() {
		return this.serverMsgId;
	}

	public void setServerMsgId(String id) {
		this.serverMsgId = id;
	}

	public boolean isRead() {
		return this.read;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public void markRead() {
		this.read = true;
		if (this.timer != TIMER_NONE) {
			this.endTime = System.currentTimeMillis() + ((long)timer * 1000L); //ALF AM-53
		}
	}

	public void markUnread() {
		this.read = false;
		this.endTime = Long.MAX_VALUE; //ALF AM-53
	}

	public void setTime(long time) {
		this.timeSent = time;
	}

	public String getEncryptedBody() {
		return this.encryptedBody;
	}

	public void setEncryptedBody(String body) {
		this.encryptedBody = body;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	//ALF AM-53 (next five)
	public int getTimer() {
		return this.timer;
	}

	public void setTimer(int timer) {
		this.timer = timer;
		if (timer != TIMER_NONE) {
			this.endTime = System.currentTimeMillis() + ((long)timer * 1000L);
		}
	}

	public long getEndTime() {
		return this.endTime;
	}

	public void setEndTime(long endtime) {
		this.endTime = endtime;
	}

	public long getTimeRemaining() {
		return (this.endTime - System.currentTimeMillis()) / 1000;
	}

	public boolean isCarbon() {
		return carbon;
	}

	public void setCarbon(boolean carbon) {
		this.carbon = carbon;
	}

	public void putEdited(String edited, String serverMsgId) {
		this.edits.add(new Edited(edited, serverMsgId));
	}

	public boolean edited() {
		return this.edits.size() > 0;
	}

	public void setTrueCounterpart(Jid trueCounterpart) {
		this.trueCounterpart = trueCounterpart;
	}

	public Jid getTrueCounterpart() {
		return this.trueCounterpart;
	}

	public Transferable getTransferable() {
		return this.transferable;
	}

	public synchronized void setTransferable(Transferable transferable) {
		this.fileParams = null;
		this.transferable = transferable;
	}

	public boolean addReadByMarker(ReadByMarker readByMarker) {
		if (readByMarker.getRealJid() != null) {
			if (readByMarker.getRealJid().asBareJid().equals(trueCounterpart)) {
				return false;
			}
		} else if (readByMarker.getFullJid() != null) {
			if (readByMarker.getFullJid().equals(counterpart)) {
				return false;
			}
		}
		if (this.readByMarkers.add(readByMarker)) {
			if (readByMarker.getRealJid() != null && readByMarker.getFullJid() != null) {
				Iterator<ReadByMarker> iterator = this.readByMarkers.iterator();
				while (iterator.hasNext()) {
					ReadByMarker marker = iterator.next();
					if (marker.getRealJid() == null && readByMarker.getFullJid().equals(marker.getFullJid())) {
						iterator.remove();
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public Set<ReadByMarker> getReadByMarkers() {
		return Collections.unmodifiableSet(this.readByMarkers);
	}

	boolean similar(Message message) {
		if (type != TYPE_PRIVATE && this.serverMsgId != null && message.getServerMsgId() != null) {
			return this.serverMsgId.equals(message.getServerMsgId()) || Edited.wasPreviouslyEditedServerMsgId(edits, message.getServerMsgId());
		} else if (Edited.wasPreviouslyEditedServerMsgId(edits, message.getServerMsgId())) {
			return true;
		} else if (this.body == null || this.counterpart == null) {
			return false;
		} else {
			String body, otherBody;
			if (this.hasFileOnRemoteHost()) {
				body = getFileParams().url.toString();
				otherBody = message.body == null ? null : message.body.trim();
			} else {
				body = this.body;
				otherBody = message.body;
			}
			final boolean matchingCounterpart = this.counterpart.equals(message.getCounterpart());
			if (message.getRemoteMsgId() != null) {
				final boolean hasUuid = CryptoHelper.UUID_PATTERN.matcher(message.getRemoteMsgId()).matches();
				if (hasUuid && matchingCounterpart && Edited.wasPreviouslyEditedRemoteMsgId(edits, message.getRemoteMsgId())) {
					return true;
				}
				return (message.getRemoteMsgId().equals(this.remoteMsgId) || message.getRemoteMsgId().equals(this.uuid))
						&& matchingCounterpart
						&& (body.equals(otherBody) || (message.getEncryption() == Message.ENCRYPTION_PGP && hasUuid));
			} else {
				return this.remoteMsgId == null
						&& matchingCounterpart
						&& body.equals(otherBody)
						&& Math.abs(this.getTimeSent() - message.getTimeSent()) < Config.MESSAGE_MERGE_WINDOW * 1000;
			}
		}
	}

	public Message next() {
		if (this.conversation instanceof Conversation) {
			final Conversation conversation = (Conversation) this.conversation;
			synchronized (conversation.messages) {
				if (this.mNextMessage == null) {
					int index = conversation.messages.indexOf(this);
					if (index < 0 || index >= conversation.messages.size() - 1) {
						this.mNextMessage = null;
					} else {
						this.mNextMessage = conversation.messages.get(index + 1);
					}
				}
				return this.mNextMessage;
			}
		} else {
			throw new AssertionError("Calling next should be disabled for stubs");
		}
	}

	public Message prev() {
		if (this.conversation instanceof Conversation) {
			final Conversation conversation = (Conversation) this.conversation;
			synchronized (conversation.messages) {
				if (this.mPreviousMessage == null) {
					int index = conversation.messages.indexOf(this);
					if (index <= 0 || index > conversation.messages.size()) {
						this.mPreviousMessage = null;
					} else {
						this.mPreviousMessage = conversation.messages.get(index - 1);
					}
				}
			}
			return this.mPreviousMessage;
		} else {
			throw new AssertionError("Calling prev should be disabled for stubs");
		}
	}

	public boolean isLastCorrectableMessage() {
		Message next = next();
		while (next != null) {
			if (next.isCorrectable()) {
				return false;
			}
			next = next.next();
		}
		return isCorrectable();
	}

	private boolean isCorrectable() {
		return getStatus() != STATUS_RECEIVED && !isCarbon();
	}

	public boolean mergeable(final Message message) {
		return message != null &&
				(message.getType() == Message.TYPE_TEXT &&
						this.getTransferable() == null &&
						message.getTransferable() == null &&
						message.getEncryption() != Message.ENCRYPTION_PGP &&
						message.getEncryption() != Message.ENCRYPTION_DECRYPTION_FAILED &&
						this.getType() == message.getType() &&
						//this.getStatus() == message.getStatus() &&
						this.getTimer() == message.getTimer() && //ALF AM-53
						this.getEndTime() == message.getEndTime() && //ALF AM-53
						isStatusMergeable(this.getStatus(), message.getStatus()) &&
						this.getEncryption() == message.getEncryption() &&
						this.getCounterpart() != null &&
						this.getCounterpart().equals(message.getCounterpart()) &&
						this.edited() == message.edited() &&
						(message.getTimeSent() - this.getTimeSent()) <= (Config.MESSAGE_MERGE_WINDOW * 1000) &&
						this.getBody().length() + message.getBody().length() <= Config.MAX_DISPLAY_MESSAGE_CHARS &&
						!message.isGeoUri() &&
						!this.isGeoUri() &&
						!message.treatAsDownloadable() &&
						!this.treatAsDownloadable() &&
						!message.getBody().startsWith(ME_COMMAND) &&
						!this.getBody().startsWith(ME_COMMAND) &&
						!message.getBody().endsWith("added to the group") &&
						!message.getBody().endsWith("left the group") && //ALF AM-51
						!this.getBody().endsWith("added to the group") &&
						!this.getBody().endsWith("left the group") && //ALF AM-51, AM-75
						!message.getBody().startsWith("You called") &&
						!this.getBody().startsWith("You called") &&
						!message.getBody().startsWith("Call received") &&
						!this.getBody().startsWith("Call received") &&
						!message.getBody().startsWith("Missed call") &&
						!this.getBody().startsWith("Missed call") && //ALF AM-421
						!this.bodyIsOnlyEmojis() &&
						!message.bodyIsOnlyEmojis() &&
						((this.axolotlFingerprint == null && message.axolotlFingerprint == null) || (this.axolotlFingerprint != null && message.axolotlFingerprint != null && this.axolotlFingerprint.equals(message.getFingerprint()))) && //ALF AM-75 added != nulls
						UIHelper.sameDay(message.getTimeSent(), this.getTimeSent()) &&
						this.getReadByMarkers().equals(message.getReadByMarkers()) &&
						!this.conversation.getJid().asBareJid().equals(Config.BUG_REPORTS)
				);
	}

	private static boolean isStatusMergeable(int a, int b) {
		return a == b || (
				(a == Message.STATUS_SEND_RECEIVED && b == Message.STATUS_UNSEND)
						|| (a == Message.STATUS_SEND_RECEIVED && b == Message.STATUS_SEND)
						|| (a == Message.STATUS_SEND_RECEIVED && b == Message.STATUS_WAITING)
						|| (a == Message.STATUS_SEND && b == Message.STATUS_UNSEND)
						|| (a == Message.STATUS_SEND && b == Message.STATUS_WAITING)
		);
	}

	public void setCounterparts(List<MucOptions.User> counterparts) {
		this.counterparts = counterparts;
	}

	public List<MucOptions.User> getCounterparts() {
		return this.counterparts;
	}

	public static class MergeSeparator {
	}

	public SpannableStringBuilder getMergedBody() {
		SpannableStringBuilder body = new SpannableStringBuilder(MessageUtils.filterLtrRtl(this.body).trim());
		Message current = this;
		while (current.mergeable(current.next())) {
			current = current.next();
			if (current == null) {
				break;
			}
			body.append("\n\n");
			body.setSpan(new MergeSeparator(), body.length() - 2, body.length(),
					SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
			body.append(MessageUtils.filterLtrRtl(current.getBody()).trim());
		}
		return body;
	}

	public boolean hasMeCommand() {
		return this.body.trim().startsWith(ME_COMMAND);
	}

	public int getMergedStatus() {
		int status = this.status;
		Message current = this;
		while (current.mergeable(current.next())) {
			current = current.next();
			if (current == null) {
				break;
			}
			status = current.status;
		}
		return status;
	}

	public long getMergedTimeSent() {
		long time = this.timeSent;
		Message current = this;
		while (current.mergeable(current.next())) {
			current = current.next();
			if (current == null) {
				break;
			}
			time = current.timeSent;
		}
		return time;
	}

	public boolean wasMergedIntoPrevious() {
		Message prev = this.prev();
		return prev != null && prev.mergeable(this);
	}

	public boolean trusted() {
		Contact contact = this.getContact();
		return status > STATUS_RECEIVED || (contact != null && (contact.showInContactList() || contact.isSelf()));
	}

	public boolean fixCounterpart() {
		Presences presences = conversation.getContact().getPresences();
		if (counterpart != null && presences.has(counterpart.getResource())) {
			return true;
		} else if (presences.size() >= 1) {
			try {
				counterpart = Jid.of(conversation.getJid().getLocal(),
						conversation.getJid().getDomain(),
						presences.toResourceArray()[0]);
				return true;
			} catch (IllegalArgumentException e) {
				counterpart = null;
				return false;
			}
		} else {
			counterpart = null;
			return false;
		}
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getEditedId() {
		if (edits.size() > 0) {
			return edits.get(edits.size() - 1).getEditedId();
		} else {
			throw new IllegalStateException("Attempting to store unedited message");
		}
	}

	public void setOob(boolean isOob) {
		this.oob = isOob;
	}

	public String getMimeType() {
		String extension;
		if (relativeFilePath != null) {
			extension = MimeUtils.extractRelevantExtension(relativeFilePath);
		} else {
			try {
				final URL url = new URL(body.split("\n")[0]);
				extension = MimeUtils.extractRelevantExtension(url);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return MimeUtils.guessMimeTypeFromExtension(extension);
	}

	public synchronized boolean treatAsDownloadable() {
		if (treatAsDownloadable == null) {
			treatAsDownloadable = MessageUtils.treatAsDownloadable(this.body, this.oob);
		}
		return treatAsDownloadable;
	}

	public synchronized boolean bodyIsOnlyEmojis() {
		if (isEmojisOnly == null) {
			isEmojisOnly = Emoticons.isOnlyEmoji(body.replaceAll("\\s", ""));
		}
		return isEmojisOnly;
	}

	public synchronized boolean isGeoUri() {
		if (isGeoUri == null) {
			isGeoUri = GeoHelper.GEO_URI.matcher(body).matches();
		}
		return isGeoUri;
	}

	public synchronized void resetFileParams() {
		this.fileParams = null;
	}

	public synchronized FileParams getFileParams() {
		if (fileParams == null) {
			fileParams = new FileParams();
			if (this.transferable != null) {
				fileParams.size = this.transferable.getFileSize();
			}
			String parts[] = body == null ? new String[0] : body.split("\\|");
			switch (parts.length) {
				case 1:
					try {
						fileParams.size = Long.parseLong(parts[0]);
					} catch (NumberFormatException e) {
						fileParams.url = parseUrl(parts[0]);
					}
					break;
				case 5:
					fileParams.runtime = parseInt(parts[4]);
				case 4:
					fileParams.width = parseInt(parts[2]);
					fileParams.height = parseInt(parts[3]);
				case 2:
					fileParams.url = parseUrl(parts[0]);
					fileParams.size = parseLong(parts[1]);
					break;
				case 3:
					fileParams.size = parseLong(parts[0]);
					fileParams.width = parseInt(parts[1]);
					fileParams.height = parseInt(parts[2]);
					break;
			}
		}
		return fileParams;
	}

	private static long parseLong(String value) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static URL parseUrl(String value) {
		try {
			return new URL(value);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public void untie() {
		this.mNextMessage = null;
		this.mPreviousMessage = null;
	}

	public boolean isFileOrImage() {
		return type == TYPE_FILE || type == TYPE_IMAGE;
	}

	public boolean hasFileOnRemoteHost() {
		return isFileOrImage() && getFileParams().url != null;
	}

	public boolean needsUploading() {
		return isFileOrImage() && getFileParams().url == null;
	}

	public class FileParams {
		public URL url;
		public long size = 0;
		public int width = 0;
		public int height = 0;
		public int runtime = 0;
	}

	public void setFingerprint(String fingerprint) {
		this.axolotlFingerprint = fingerprint;
	}

	public String getFingerprint() {
		return axolotlFingerprint;
	}

	public boolean isTrusted() {
		FingerprintStatus s = conversation.getAccount().getAxolotlService().getFingerprintTrust(axolotlFingerprint);
		return s != null && s.isTrusted();
	}

	private int getPreviousEncryption() {
		for (Message iterator = this.prev(); iterator != null; iterator = iterator.prev()) {
			if (iterator.isCarbon() || iterator.getStatus() == STATUS_RECEIVED) {
				continue;
			}
			return iterator.getEncryption();
		}
		return ENCRYPTION_NONE;
	}

	private int getNextEncryption() {
		if (this.conversation instanceof Conversation) {
			Conversation conversation = (Conversation) this.conversation;
			for (Message iterator = this.next(); iterator != null; iterator = iterator.next()) {
				if (iterator.isCarbon() || iterator.getStatus() == STATUS_RECEIVED) {
					continue;
				}
				return iterator.getEncryption();
			}
			return conversation.getNextEncryption();
		} else {
			throw new AssertionError("This should never be called since isInValidSession should be disabled for stubs");
		}
	}

	public boolean isValidInSession() {
		int pastEncryption = getCleanedEncryption(this.getPreviousEncryption());
		int futureEncryption = getCleanedEncryption(this.getNextEncryption());

		boolean inUnencryptedSession = pastEncryption == ENCRYPTION_NONE
				|| futureEncryption == ENCRYPTION_NONE
				|| pastEncryption != futureEncryption;

		return inUnencryptedSession || getCleanedEncryption(this.getEncryption()) == pastEncryption;
	}

	private static int getCleanedEncryption(int encryption) {
		if (encryption == ENCRYPTION_DECRYPTED || encryption == ENCRYPTION_DECRYPTION_FAILED) {
			return ENCRYPTION_PGP;
		}
		if (encryption == ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE || encryption == ENCRYPTION_AXOLOTL_FAILED) {
			return ENCRYPTION_AXOLOTL;
		}
		return encryption;
	}
}
