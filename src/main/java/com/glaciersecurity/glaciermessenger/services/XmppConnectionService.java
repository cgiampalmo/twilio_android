package com.glaciersecurity.glaciermessenger.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.security.KeyChain;
import androidx.annotation.BoolRes;
import androidx.annotation.IntegerRes;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.util.LruCache;
import android.util.Pair;
import android.widget.Toast;

import org.conscrypt.Conscrypt;
//import org.openintents.openpgp.util.OpenPgpApi;
//import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.whispersystems.libsignal.state.PreKeyRecord;

import java.io.File;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.android.JabberIdContact;
import com.glaciersecurity.glaciermessenger.crypto.OmemoSetting;
//import com.glaciersecurity.glaciermessenger.crypto.PgpEngine;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.AxolotlService;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.FingerprintStatus;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.XmppAxolotlMessage;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Blockable;
import com.glaciersecurity.glaciermessenger.entities.Bookmark;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Conversational;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.entities.MucOptions.OnRenameListener;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.entities.Roster;
import com.glaciersecurity.glaciermessenger.entities.ServiceDiscoveryResult;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.generator.AbstractGenerator;
import com.glaciersecurity.glaciermessenger.generator.IqGenerator;
import com.glaciersecurity.glaciermessenger.generator.MessageGenerator;
import com.glaciersecurity.glaciermessenger.generator.PresenceGenerator;
import com.glaciersecurity.glaciermessenger.http.HttpConnectionManager;
import com.glaciersecurity.glaciermessenger.http.CustomURLStreamHandlerFactory;
import com.glaciersecurity.glaciermessenger.parser.AbstractParser;
import com.glaciersecurity.glaciermessenger.parser.IqParser;
import com.glaciersecurity.glaciermessenger.parser.MessageParser;
import com.glaciersecurity.glaciermessenger.parser.PresenceParser;
import com.glaciersecurity.glaciermessenger.persistance.DatabaseBackend;
import com.glaciersecurity.glaciermessenger.persistance.FileBackend;
import com.glaciersecurity.glaciermessenger.ui.CallActivity;
import com.glaciersecurity.glaciermessenger.ui.ChooseAccountForProfilePictureActivity;
import com.glaciersecurity.glaciermessenger.ui.SettingsActivity;
import com.glaciersecurity.glaciermessenger.ui.UiCallback;
import com.glaciersecurity.glaciermessenger.ui.VideoActivity;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnAvatarPublication;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnMediaLoaded;
import com.glaciersecurity.glaciermessenger.ui.interfaces.OnSearchResultsAvailable;
import com.glaciersecurity.glaciermessenger.ui.util.SoundPoolManager;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.ConversationsFileObserver;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.glaciersecurity.glaciermessenger.utils.ExceptionHelper;
import com.glaciersecurity.glaciermessenger.utils.MimeUtils;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;
import com.glaciersecurity.glaciermessenger.utils.QuickLoader;
import com.glaciersecurity.glaciermessenger.utils.ReplacingSerialSingleThreadExecutor;
import com.glaciersecurity.glaciermessenger.utils.ReplacingTaskManager;
import com.glaciersecurity.glaciermessenger.utils.Resolver;
import com.glaciersecurity.glaciermessenger.utils.SMSdbInfo;
import com.glaciersecurity.glaciermessenger.utils.SerialSingleThreadExecutor;
import com.glaciersecurity.glaciermessenger.utils.StringUtils;
import com.glaciersecurity.glaciermessenger.utils.SystemSecurityInfo;
import com.glaciersecurity.glaciermessenger.utils.WakeLockHelper;
import com.glaciersecurity.glaciermessenger.xml.Namespace;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import com.glaciersecurity.glaciermessenger.xml.Element;
import com.glaciersecurity.glaciermessenger.xmpp.OnBindListener;
import com.glaciersecurity.glaciermessenger.xmpp.OnContactStatusChanged;
import com.glaciersecurity.glaciermessenger.xmpp.OnIqPacketReceived;
import com.glaciersecurity.glaciermessenger.xmpp.OnKeyStatusUpdated;
import com.glaciersecurity.glaciermessenger.xmpp.OnMessageAcknowledged;
import com.glaciersecurity.glaciermessenger.xmpp.OnMessagePacketReceived;
import com.glaciersecurity.glaciermessenger.xmpp.OnPresencePacketReceived;
import com.glaciersecurity.glaciermessenger.xmpp.OnStatusChanged;
import com.glaciersecurity.glaciermessenger.xmpp.OnUpdateBlocklist;
import com.glaciersecurity.glaciermessenger.xmpp.Patches;
import com.glaciersecurity.glaciermessenger.xmpp.XmppConnection;
import com.glaciersecurity.glaciermessenger.xmpp.chatstate.ChatState;
import com.glaciersecurity.glaciermessenger.xmpp.forms.Data;
import com.glaciersecurity.glaciermessenger.xmpp.jingle.JingleConnectionManager;
import com.glaciersecurity.glaciermessenger.xmpp.jingle.OnJinglePacketReceived;
import com.glaciersecurity.glaciermessenger.xmpp.jingle.stanzas.JinglePacket;
import com.glaciersecurity.glaciermessenger.xmpp.mam.MamReference;
import com.glaciersecurity.glaciermessenger.xmpp.pep.Avatar;
import com.glaciersecurity.glaciermessenger.xmpp.pep.PublishOptions;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.IqPacket;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.MessagePacket;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.PresencePacket;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import me.leolin.shortcutbadger.ShortcutBadger;
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class XmppConnectionService extends Service implements ServiceConnection, Handler.Callback { //ALF AM-57 placeholder, AM-344

	//ALF AM-410
	public static final String ACTION_REPLY_TO_CALL_REQUEST = "reply_to_call_request";
	public static final String ACTION_ACCEPT_CALL_REQUEST = "accept_call_request";
	public static final String ACTION_REJECT_CALL_REQUEST = "reject_call_request";
	public static final String ACTION_CANCEL_CALL_REQUEST = "cancel_call_request";
	public static final String ACTION_FINISH_CALL = "finish_call";

	public static final String ACTION_REPLY_TO_CONVERSATION = "reply_to_conversations";
	public static final String ACTION_MARK_AS_READ = "mark_as_read";
	public static final String ACTION_SNOOZE = "snooze";
	public static final String ACTION_CLEAR_NOTIFICATION = "clear_notification";
	public static final String ACTION_DISMISS_ERROR_NOTIFICATIONS = "dismiss_error";
	public static final String ACTION_TRY_AGAIN = "try_again";
	public static final String ACTION_IDLE_PING = "idle_ping";
	public static final String ACTION_FCM_TOKEN_REFRESH = "fcm_token_refresh";
	public static final String ACTION_FCM_MESSAGE_RECEIVED = "fcm_message_received";
	private static final String ACTION_POST_CONNECTIVITY_CHANGE = "com.glaciersecurity.glaciermessenger.POST_CONNECTIVITY_CHANGE";

	private static final String SETTING_LAST_ACTIVITY_TS = "last_activity_timestamp";

	static {
		URL.setURLStreamHandlerFactory(new CustomURLStreamHandlerFactory());
	}

	public final CountDownLatch restoredFromDatabaseLatch = new CountDownLatch(1);
	private final SerialSingleThreadExecutor mFileAddingExecutor = new SerialSingleThreadExecutor("FileAdding");
	private final SerialSingleThreadExecutor mVideoCompressionExecutor = new SerialSingleThreadExecutor("VideoCompression");
	private final SerialSingleThreadExecutor mDatabaseWriterExecutor = new SerialSingleThreadExecutor("DatabaseWriter");
	private final SerialSingleThreadExecutor mDatabaseReaderExecutor = new SerialSingleThreadExecutor("DatabaseReader");
	private final SerialSingleThreadExecutor mNotificationExecutor = new SerialSingleThreadExecutor("NotificationExecutor");
	private final ReplacingTaskManager mRosterSyncTaskManager = new ReplacingTaskManager();
	private final IBinder mBinder = new XmppConnectionBinder();
	private final List<Conversation> conversations = new CopyOnWriteArrayList<>();
	private final IqGenerator mIqGenerator = new IqGenerator(this);
	private final Set<String> mInProgressAvatarFetches = new HashSet<>();
	private final Set<String> mOmittedPepAvatarFetches = new HashSet<>();
	private final HashSet<Jid> mLowPingTimeoutMode = new HashSet<>();
	private final OnIqPacketReceived mDefaultIqHandler = (account, packet) -> {
		if (packet.getType() != IqPacket.TYPE.RESULT) {
			Element error = packet.findChild("error");
			String text = error != null ? error.findChildContent("text") : null;
			if (text != null) {
				Log.d(Config.LOGTAG, account.getLogJid() + ": received iq error - " + text);
			}
		}
	};
	public DatabaseBackend databaseBackend;
	private ReplacingSerialSingleThreadExecutor mContactMergerExecutor = new ReplacingSerialSingleThreadExecutor("ContactMerger");
	private long mLastActivity = 0;
	private FileBackend fileBackend = new FileBackend(this);
	private MemorizingTrustManager mMemorizingTrustManager;
	private NotificationService mNotificationService = new NotificationService(this);
	private ShortcutService mShortcutService = new ShortcutService(this);
	private AtomicBoolean mInitialAddressbookSyncCompleted = new AtomicBoolean(false);
	private AtomicBoolean mForceForegroundService = new AtomicBoolean(false);
	private AtomicBoolean mForceDuringOnCreate = new AtomicBoolean(false);
	private OnMessagePacketReceived mMessageParser = new MessageParser(this);
	private OnPresencePacketReceived mPresenceParser = new PresenceParser(this);
	private IqParser mIqParser = new IqParser(this);
	private MessageGenerator mMessageGenerator = new MessageGenerator(this);
	public OnContactStatusChanged onContactStatusChanged = (contact, online) -> {
		Conversation conversation = find(getConversations(), contact);
		if (conversation != null) {
			if (online) {
				if (contact.getPresences().size() == 1) {
					sendUnsentMessages(conversation);
				}
			}
		}
	};
	private PresenceGenerator mPresenceGenerator = new PresenceGenerator(this);
	private List<Account> accounts;
	private JingleConnectionManager mJingleConnectionManager = new JingleConnectionManager(
			this);
	private final OnJinglePacketReceived jingleListener = new OnJinglePacketReceived() {

		@Override
		public void onJinglePacketReceived(Account account, JinglePacket packet) {
			mJingleConnectionManager.deliverPacket(account, packet);
		}
	};
	private HttpConnectionManager mHttpConnectionManager = new HttpConnectionManager(
			this);
	private AvatarService mAvatarService = new AvatarService(this);
	private MessageArchiveService mMessageArchiveService = new MessageArchiveService(this);
	//private VPNConnectionService mVPNConnectionService = new VPNConnectionService(); //ALF AM-57 placeholder
	private PushManagementService mPushManagementService = new PushManagementService(this);
	private QuickConversationsService mQuickConversationsService = new QuickConversationsService(this);

	private CallManager callManager = new CallManager(this); //ALF AM-478;

	//AM#14
	private ProcessLifecycleListener processListener;
	private boolean needsProcessLifecycleUpdate = false;
	private boolean ignoreLifecycleUpdate = false;
	private boolean lastBioauthFailed = false;
	private AtomicLong mLastGlacierUsage = new AtomicLong(0);

	public static final long BIOAUTH_INTERVAL_DEFAULT = 0L;

	//AM#52, AM#53
	private AtomicLong mLastSecInfoUpdate = new AtomicLong(0);
	public static final long SECHUB_INTERVAL = 86400L;
	private SystemSecurityInfo secInfo;
	private SMSdbInfo smsInfo;
	private boolean needsSecurityInfoUpdate = false;

	private ConversationsFileObserver fileObserver; //ALF AM-603 moved to onCreate and changed mechanism
	/*private final ConversationsFileObserver fileObserver = new ConversationsFileObserver(
			Environment.getExternalStorageDirectory().getAbsolutePath()
	) {
		@Override
		public void onEvent(int event, String path) {
			markFileDeleted(path);
		}
	};*/
	private final OnMessageAcknowledged mOnMessageAcknowledgedListener = new OnMessageAcknowledged() {

		@Override
		public boolean onMessageAcknowledged(Account account, String uuid) {
			for (final Conversation conversation : getConversations()) {
				if (conversation.getAccount() == account) {
					Message message = conversation.findUnsentMessageWithUuid(uuid);
					if (message != null) {
						message.setStatus(Message.STATUS_SEND);
						message.setErrorMessage(null);
						databaseBackend.updateMessage(message, false);
						return true;
					}
				}
			}
			return false;
		}
	};

	private boolean destroyed = false;

	private int unreadCount = -1;

	//ALF AM-344
	protected IOpenVPNAPIService mService=null;
	private Handler mHandler;
	private static final int MSG_UPDATE_STATE = 0;

	//ALF AM-410
	private TwilioCall currentTwilioCall;
	private Handler callHandler = new Handler();
	private Handler busyHandler = new Handler(); //ALF AM-420
	private int cancelledCall = -1; //AM-492

	//AM-541
	private boolean glacierOwnerInForeground = true;
	private TwilioCall waitingToStartCall = null;
	private BroadcastReceiver mUserBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_USER_BACKGROUND)) {
				glacierOwnerInForeground = false;
			} else if (intent.getAction().equals(Intent.ACTION_USER_FOREGROUND)) {
				glacierOwnerInForeground = true;

				if (waitingToStartCall != null) {
					callHandler.removeCallbacksAndMessages(null);
					final Intent recallIntent = new Intent(getApplicationContext(), XmppConnectionService.class);

					try {
						recallIntent.setAction(XmppConnectionService.ACTION_REPLY_TO_CALL_REQUEST);
						recallIntent.putExtra("call_id", Integer.toString(waitingToStartCall.getCallId()));
						recallIntent.putExtra("caller", waitingToStartCall.getCaller());
						recallIntent.putExtra("status", waitingToStartCall.getStatus());
						recallIntent.putExtra("calltime", Long.toString(waitingToStartCall.getCallTime()));

						onStartCommand(recallIntent,0,0);
						Log.d(Config.LOGTAG, "*** XmppConnectionService - Entered foreground, resent call broadcast");
					} catch (Exception nfe) {
						Log.d(Config.LOGTAG, "*** XmppConnectionService - Error Resending call broadcast");
						waitingToStartCall = null;
					}
				}
			}
		}
	};

	private Presence.Status lastStatus = Presence.Status.OFFLINE; //AM-642

	//Ui callback listeners
	private final Set<OnConversationUpdate> mOnConversationUpdates = Collections.newSetFromMap(new WeakHashMap<OnConversationUpdate, Boolean>());
	private final Set<OnShowErrorToast> mOnShowErrorToasts = Collections.newSetFromMap(new WeakHashMap<OnShowErrorToast, Boolean>());
	private final Set<OnAccountUpdate> mOnAccountUpdates = Collections.newSetFromMap(new WeakHashMap<OnAccountUpdate, Boolean>());
	private final Set<OnCaptchaRequested> mOnCaptchaRequested = Collections.newSetFromMap(new WeakHashMap<OnCaptchaRequested, Boolean>());
	private final Set<OnRosterUpdate> mOnRosterUpdates = Collections.newSetFromMap(new WeakHashMap<OnRosterUpdate, Boolean>());
	private final Set<OnUpdateBlocklist> mOnUpdateBlocklist = Collections.newSetFromMap(new WeakHashMap<OnUpdateBlocklist, Boolean>());
	private final Set<OnMucRosterUpdate> mOnMucRosterUpdate = Collections.newSetFromMap(new WeakHashMap<OnMucRosterUpdate, Boolean>());
	private final Set<OnKeyStatusUpdated> mOnKeyStatusUpdated = Collections.newSetFromMap(new WeakHashMap<OnKeyStatusUpdated, Boolean>());
	private final Set<OnProcessLifecycleUpdate> mOnProcessLifecycleUpdates = Collections.newSetFromMap(new WeakHashMap<OnProcessLifecycleUpdate, Boolean>()); //AM#14

	private final Object LISTENER_LOCK = new Object();


	private final OnBindListener mOnBindListener = new OnBindListener() {

		@Override
		public void onBind(final Account account) {
			//ALF AM-222
			//check if already scheduled
			scheduleNextIdlePing();
			synchronized (mInProgressAvatarFetches) {
				for (Iterator<String> iterator = mInProgressAvatarFetches.iterator(); iterator.hasNext(); ) {
					final String KEY = iterator.next();
					if (KEY.startsWith(account.getJid().asBareJid() + "_")) {
						iterator.remove();
					}
				}
			}
			boolean loggedInSuccessfully = account.setOption(Account.OPTION_LOGGED_IN_SUCCESSFULLY, true);
			boolean gainedFeature = account.setOption(Account.OPTION_HTTP_UPLOAD_AVAILABLE, account.getXmppConnection().getFeatures().httpUpload(0));
			if (loggedInSuccessfully || gainedFeature) {
				databaseBackend.updateAccount(account);
			}

			if (loggedInSuccessfully) {
				if (!TextUtils.isEmpty(account.getDisplayName())) {
					Log.d(Config.LOGTAG, account.getLogJid()+": display name wasn't empty on first log in. publishing");
					publishDisplayName(account);
				}
			}
			account.getRoster().clearPresences();
			mJingleConnectionManager.cancelInTransmission();
			mQuickConversationsService.considerSyncBackground(false);
			fetchRosterFromServer(account);
			if (!account.getXmppConnection().getFeatures().bookmarksConversion()) {
				fetchBookmarks(account);
			}
			final boolean flexible = account.getXmppConnection().getFeatures().flexibleOfflineMessageRetrieval();
			final boolean catchup = getMessageArchiveService().inCatchup(account);
			if (flexible && catchup && account.getXmppConnection().isMamPreferenceAlways()) {
				sendIqPacket(account, mIqGenerator.purgeOfflineMessages(), (acc, packet) -> {
					if (packet.getType() == IqPacket.TYPE.RESULT) {
						Log.d(Config.LOGTAG, acc.getLogJid()+ ": successfully purged offline messages");
					}
				});
			}
			sendPresence(account);
			if (mPushManagementService.available(account)) {
				mPushManagementService.registerPushTokenOnServer(account);
			}
			connectMultiModeConversations(account);
			syncDirtyContacts(account);
		}
	};
	private AtomicLong mLastExpiryRun = new AtomicLong(0);
	private SecureRandom mRandom;
	private LruCache<Pair<String, String>, ServiceDiscoveryResult> discoCache = new LruCache<>(20);
	private OnStatusChanged statusListener = new OnStatusChanged() {

		@Override
		public void onStatusChanged(final Account account) {
			XmppConnection connection = account.getXmppConnection();
			updateAccountUi();

			if (account.getStatus() == Account.State.ONLINE || account.getStatus().isError()) {
				mQuickConversationsService.signalAccountStateChange();
			}

			if (account.getStatus() == Account.State.ONLINE) {
				synchronized (mLowPingTimeoutMode) {
					if (mLowPingTimeoutMode.remove(account.getJid().asBareJid())) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": leaving low ping timeout mode");
					}
				}
				if (account.setShowErrorNotification(true)) {
					databaseBackend.updateAccount(account);
				}
				mMessageArchiveService.executePendingQueries(account);
				if (connection != null && connection.getFeatures().csi()) {
					if (checkListeners()) {
						Log.d(Config.LOGTAG, account.getLogJid() + " sending csi//inactive");
						connection.sendInactive();
					} else {
						Log.d(Config.LOGTAG, account.getLogJid() + " sending csi//active");
						connection.sendActive();
					}
				}
				List<Conversation> conversations = getConversations();
				for (Conversation conversation : conversations) {
					if (conversation.getAccount() == account && !account.pendingConferenceJoins.contains(conversation)) {
						sendUnsentMessages(conversation);
					}
				}
				for (Conversation conversation : account.pendingConferenceLeaves) {
					leaveMuc(conversation);
				}
				account.pendingConferenceLeaves.clear();
				for (Conversation conversation : account.pendingConferenceJoins) {
					joinMuc(conversation);
				}
				account.pendingConferenceJoins.clear();
				scheduleWakeUpCall(Config.PING_MAX_INTERVAL, account.getUuid().hashCode());
			} else if (account.getStatus() == Account.State.OFFLINE || account.getStatus() == Account.State.DISABLED ||
					account.getStatus() == Account.State.TLS_ERROR) { //ALF AM-275
				resetSendingToWaiting(account);
				if (account.isEnabled() && isInLowPingTimeoutMode(account)) {
					Log.d(Config.LOGTAG, account.getLogJid() + ": went into offline state during low ping mode. reconnecting now");
					reconnectAccount(account, true, false);
				} else {
					int timeToReconnect = mRandom.nextInt(10) + 2;
					scheduleWakeUpCall(timeToReconnect, account.getUuid().hashCode());
				}
			} else if (account.getStatus() == Account.State.REGISTRATION_SUCCESSFUL) {
				databaseBackend.updateAccount(account);
				reconnectAccount(account, true, false);
			} else if (account.getStatus() != Account.State.CONNECTING && account.getStatus() != Account.State.NO_INTERNET) {
				resetSendingToWaiting(account);
				if (connection != null && account.getStatus().isAttemptReconnect()) {
					final int next = connection.getTimeToNextAttempt();
					final boolean lowPingTimeoutMode = isInLowPingTimeoutMode(account);
					if (next <= 0) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": error connecting account. reconnecting now. lowPingTimeout=" + Boolean.toString(lowPingTimeoutMode));
						reconnectAccount(account, true, false);
					} else {
						final int attempt = connection.getAttempt() + 1;
						Log.d(Config.LOGTAG, account.getLogJid() + ": error connecting account. try again in " + next + "s for the " + attempt + " time. lowPingTimeout=" + Boolean.toString(lowPingTimeoutMode));
						scheduleWakeUpCall(next, account.getUuid().hashCode());
					}
				}
			}
			getNotificationService().updateErrorNotification();
		}
	};
	//private OpenPgpServiceConnection pgpServiceConnection;
	//private PgpEngine mPgpEngine = null;
	private WakeLock wakeLock;
	private PowerManager pm;
	private LruCache<String, Bitmap> mBitmapCache;
	//private EventReceiver mEventReceiver = new EventReceiver(); //ALF AM-184 from Conversations an following two
	private BroadcastReceiver mInternalEventReceiver = new InternalEventReceiver();
	private BroadcastReceiver mInternalScreenEventReceiver = new InternalEventReceiver();

	private static String generateFetchKey(Account account, final Avatar avatar) {
		return account.getJid().asBareJid() + "_" + avatar.owner + "_" + avatar.sha1sum;
	}

	private boolean isInLowPingTimeoutMode(Account account) {
		synchronized (mLowPingTimeoutMode) {
			return mLowPingTimeoutMode.contains(account.getJid().asBareJid());
		}
	}

	public void startForcingForegroundNotification() {
		mForceForegroundService.set(true);
		toggleForegroundService();
	}

	public void stopForcingForegroundNotification() {
		mForceForegroundService.set(false);
		mNotificationService.dismissForcedForegroundNotification(); //AM#3
		//toggleForegroundService();
	}

	public void checkNewPermission(){
		if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED )) {
			startContactObserver();
		}
	}

	public boolean areMessagesInitialized() {
		return this.restoredFromDatabaseLatch.getCount() == 0;
	}

	public FileBackend getFileBackend() {
		return this.fileBackend;
	}

	public AvatarService getAvatarService() {
		return this.mAvatarService;
	}

	public CallManager getCallManager() {
		return this.callManager;
	} //AM-478

	public void attachLocationToConversation(final Conversation conversation, final Uri uri, final UiCallback<Message> callback) {
		int encryption = conversation.getNextEncryption();
		if (encryption == Message.ENCRYPTION_PGP) {
			encryption = Message.ENCRYPTION_DECRYPTED;
		}
		Message message = new Message(conversation, uri.toString(), encryption);
		if (conversation.getNextCounterpart() != null) {
			message.setCounterpart(conversation.getNextCounterpart());
		}
		//if (encryption == Message.ENCRYPTION_DECRYPTED) {
		//	getPgpEngine().encrypt(message, callback);
		//} else {
		sendMessage(message);
		callback.success(message);
		//}
	}

	public void attachFileToConversation(final Conversation conversation, final Uri uri, final String type, final UiCallback<Message> callback) {
//		if (FileBackend.weOwnFile(this, uri)) {
//			Log.d(Config.LOGTAG, "trying to attach file that belonged to us");
//			callback.error(R.string.security_error_invalid_file_access, null);
//			return;
//		}
		final Message message;
		if (conversation.getNextEncryption() == Message.ENCRYPTION_PGP) {
			message = new Message(conversation, "", Message.ENCRYPTION_DECRYPTED);
		} else {
			message = new Message(conversation, "", conversation.getNextEncryption());
		}
		message.setCounterpart(conversation.getNextCounterpart());
		message.setType(Message.TYPE_FILE);
		final AttachFileToConversationRunnable runnable = new AttachFileToConversationRunnable(this, uri, type, message, callback);
		setCompressionPercent(0); //ALF AM-321
		if (runnable.isVideoMessage()) {
			mVideoCompressionExecutor.execute(runnable);
		} else {
			setCompressionPercent(100); //AM#3
			mFileAddingExecutor.execute(runnable);
		}
	}

	//ALF AM-321
	private int compressionPercent = 0;
	public void setCompressionPercent(int percent) {
		compressionPercent = percent;
	}

	public int getCompressionPercent() {
		return compressionPercent;
	}

	public void attachImageToConversation(final Conversation conversation, final Uri uri, final UiCallback<Message> callback) {
		if (FileBackend.weOwnFile(this, uri)) {
			Log.d(Config.LOGTAG, "trying to attach file that belonged to us");
			callback.error(R.string.security_error_invalid_file_access, null);
			return;
		}

		final String mimeType = MimeUtils.guessMimeTypeFromUri(this, uri);
		final String compressPictures = getCompressPicturesPreference();

		if ("never".equals(compressPictures)
				|| ("auto".equals(compressPictures) && getFileBackend().useImageAsIs(uri, getApplicationContext()))
				|| (mimeType != null && mimeType.endsWith("/gif"))
				|| getFileBackend().unusualBounds(uri)) {
			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid() + ": not compressing picture. sending as file");
			attachFileToConversation(conversation, uri, mimeType, callback);
			return;
		}
		final Message message;
		if (conversation.getNextEncryption() == Message.ENCRYPTION_PGP) {
			message = new Message(conversation, "", Message.ENCRYPTION_DECRYPTED);
		} else {
			message = new Message(conversation, "", conversation.getNextEncryption());
		}
		message.setCounterpart(conversation.getNextCounterpart());
		message.setType(Message.TYPE_IMAGE);
		mFileAddingExecutor.execute(() -> {
			try {
				getFileBackend().copyImageToPrivateStorage(message, uri);
				/*if (conversation.getNextEncryption() == Message.ENCRYPTION_PGP) {
					final PgpEngine pgpEngine = getPgpEngine();
					if (pgpEngine != null) {
						pgpEngine.encrypt(message, callback);
					} else if (callback != null) {
						callback.error(R.string.unable_to_connect_to_keychain, null);
					}
				} else {*/
				sendMessage(message);
				callback.success(message);
				//}
			} catch (final FileBackend.FileCopyException e) {
				callback.error(e.getResId(), message);
			}
		});
	}

	public Conversation find(Bookmark bookmark) {
		return find(bookmark.getAccount(), bookmark.getJid());
	}

	public Conversation find(final Account account, final Jid jid) {
		return find(getConversations(), account, jid);
	}

	public boolean isMuc(final Account account, final Jid jid) {
		final Conversation c = find(account, jid);
		return c != null && c.getMode() == Conversational.MODE_MULTI;
	}

	public void search(List<String> term, OnSearchResultsAvailable onSearchResultsAvailable) {
		MessageSearchTask.search(this, term, onSearchResultsAvailable);
	}

	//ALF AM-57 placeholder
	/*@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		//
	}

	@Override
	public void onServiceDisconnected(ComponentName className) {
		mVPNConnectionService = null;
	}*/

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent == null ? null : intent.getAction();

		//ALF AM-184 from Conversations
		final boolean needsForegroundService = intent != null && intent.getBooleanExtra(EventReceiver.EXTRA_NEEDS_FOREGROUND_SERVICE, false);
		if (needsForegroundService) {
			Log.d(Config.LOGTAG,"toggle forced foreground service after receiving event (action="+action+")");
			//toggleForegroundService(true);
			toggleForegroundService();
		}

		//ALF AM-57 placeholder?
		//bindService(new Intent(this, VPNConnectionService.class), this, Context.BIND_AUTO_CREATE);

		String pushedAccountHash = null;
		boolean interactive = false;
		if (action != null) {
			final String uuid = intent.getStringExtra("uuid");
			switch (action) {
				case ConnectivityManager.CONNECTIVITY_ACTION:
					if (hasInternetConnection()) {
						if (Config.POST_CONNECTIVITY_CHANGE_PING_INTERVAL > 0) {
							schedulePostConnectivityChange();
						}
						if (Config.RESET_ATTEMPT_COUNT_ON_NETWORK_CHANGE) {
							resetAllAttemptCounts(true, false);
						}
					}
					break;
				case Intent.ACTION_SHUTDOWN:
					logoutAndSave(true);
					return START_NOT_STICKY;
				case ACTION_CLEAR_NOTIFICATION:
					mNotificationExecutor.execute(() -> {
						try {
							final Conversation c = findConversationByUuid(uuid);
							if (c != null) {
								mNotificationService.clear(c);
							} else {
								mNotificationService.clear();
							}
							restoredFromDatabaseLatch.await();

						} catch (InterruptedException e) {
							Log.d(Config.LOGTAG, "unable to process clear notification");
						}
					});
					break;
				case ACTION_DISMISS_ERROR_NOTIFICATIONS:
					dismissErrorNotifications();
					break;
				case ACTION_TRY_AGAIN:
					resetAllAttemptCounts(false, true);
					interactive = true;
					break;
				case ACTION_REPLY_TO_CONVERSATION:
					Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
					if (remoteInput == null) {
						break;
					}
					final CharSequence body = remoteInput.getCharSequence("text_reply");
					final boolean dismissNotification = intent.getBooleanExtra("dismiss_notification", false);
					if (body == null || body.length() <= 0) {
						break;
					}
					mNotificationExecutor.execute(() -> {
						try {
							restoredFromDatabaseLatch.await();
							final Conversation c = findConversationByUuid(uuid);
							if (c != null) {
								directReply(c, body.toString(), dismissNotification);
							}
						} catch (InterruptedException e) {
							Log.d(Config.LOGTAG, "unable to process direct reply");
						}
					});
					break;
				case ACTION_REPLY_TO_CALL_REQUEST: //ALF AM-410 (and next 3 cases)
					pushedAccountHash = intent.getStringExtra("account");
					Account acct = null;
					for (Account account : accounts) {
						if (CryptoHelper.getAccountFingerprint(account,PhoneHelper.getAndroidId(this)).equals(pushedAccountHash)) {
							acct = account;
						}
					}

					//AM-541
					if (waitingToStartCall != null && pushedAccountHash == null) {
						acct = waitingToStartCall.getAccount();
						waitingToStartCall = null;
					}

					if (acct != null) {
						SoundPoolManager.getInstance(XmppConnectionService.this).stopRinging();

						TwilioCall call = new TwilioCall(acct);
						try {
							int callid = Integer.parseInt(intent.getStringExtra("call_id"));
							call.setCallId(callid);
						} catch (NumberFormatException nfe) {
						}

						call.setStatus(intent.getStringExtra("status"));

						//repurposing for AM-612, IOSM-569 this happens now when answered from other MAM device
						if (call.getStatus().equalsIgnoreCase("reject") ||
								call.getStatus().equalsIgnoreCase("busy") || call.getStatus().equalsIgnoreCase("accept")) {
							//stop CallActivity
							Intent intent1 = new Intent("callActivityFinish");
							LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
							//SoundPoolManager.getInstance(XmppConnectionService.this).stopRinging();
							callHandler.removeCallbacksAndMessages(null);
							currentTwilioCall = null;
						} else {
							call.setCaller(intent.getStringExtra("caller"));

							//ALF AM-420 if is already in call, respond with busy
							//AM-480 include native call...
							if (currentTwilioCall != null|| isInNativeCall()) {
								rejectCall(call, true);
							} else {
								currentTwilioCall = call;

								//AM-492
								long curtime = System.currentTimeMillis();
								long calltime = curtime;
								try {
									calltime = Long.parseLong(intent.getStringExtra("calltime"));
								} catch (NumberFormatException nfe) {
									calltime = curtime;
								}
								call.setCallTime(calltime); //AM-624

								//AM-492 if cancelled already don't post notification
								if ((curtime - calltime) > 45000 || cancelledCall == currentTwilioCall.getCallId()) {
									Log.d(Config.LOGTAG, "push message arrived for cancelled call");
								} else if (!glacierOwnerInForeground) {  //ALF AM-541
									Log.d(Config.LOGTAG, "XmppConnectionService - not glacierOwnerInForeground, holding call");
									currentTwilioCall = null;
									waitingToStartCall = call.getCopy();
									SoundPoolManager.getInstance(XmppConnectionService.this).vibrateIfNeeded();
									callHandler.postDelayed(() -> {
										SoundPoolManager.getInstance(XmppConnectionService.this).stopRinging(); //ALF AM-444
										Log.d(Config.LOGTAG, "XmppConnectionService - Cancelling background call after 30 sec");
										rejectCall(waitingToStartCall, false);
										waitingToStartCall = null;
										currentTwilioCall = null;
										callHandler.removeCallbacksAndMessages(null);
									}, 30000);
								} else if (!getNotificationService().pushForCall(call, pushedAccountHash)) {
									Intent callIntent = new Intent(getApplicationContext(), CallActivity.class);
									callIntent.setAction(CallActivity.ACTION_INCOMING_CALL);
									callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									callIntent.putExtra("caller", call.getCaller());
									callIntent.putExtra("status", call.getStatus());
									callIntent.putExtra("call_id", call.getCallId());
									callIntent.putExtra("account", pushedAccountHash);
									//ALF AM-447, no notification in this case because app is open, so manually play ringtone
									callIntent.putExtra("ring", true); //AM-581
									this.startActivity(callIntent);
								} else if (this.isInteractive()) {
									callHandler.postDelayed(() -> {
										SoundPoolManager.getInstance(XmppConnectionService.this).stopRinging(); //ALF AM-444
										Log.d(Config.LOGTAG, "XmppConnectionService - Cancelling call after 30 sec");
										rejectCall(currentTwilioCall, false);
										currentTwilioCall = null;
										callHandler.removeCallbacksAndMessages(null);
									}, 30000);
								}

								//AM-502 if cancelled, nullify current call so we don't get busy signal on next call
								if (currentTwilioCall != null && cancelledCall == currentTwilioCall.getCallId()) {
									currentTwilioCall = null;
								}
							}
						}

						Log.d(Config.LOGTAG, "push message arrived in service. account=" + pushedAccountHash);
					}
					break;
				case ACTION_ACCEPT_CALL_REQUEST:
					SoundPoolManager.getInstance(XmppConnectionService.this).playJoin();
					acceptCall(currentTwilioCall);
					callHandler.removeCallbacksAndMessages(null);
					break;
				case ACTION_REJECT_CALL_REQUEST:
					SoundPoolManager.getInstance(XmppConnectionService.this).playDisconnect();
					rejectCall(currentTwilioCall, false);
					currentTwilioCall = null;
					callHandler.removeCallbacksAndMessages(null);
					break;
				case ACTION_CANCEL_CALL_REQUEST: //CMG
					SoundPoolManager.getInstance(XmppConnectionService.this).playDisconnect();
					callManager.stopCallAudio(); //AM-581
					cancelCall(currentTwilioCall);
					currentTwilioCall = null;
					callHandler.removeCallbacksAndMessages(null);

					break;
				case ACTION_FINISH_CALL: //ALF AM-420
					currentTwilioCall = null;
					break;
				case ACTION_MARK_AS_READ:
					mNotificationExecutor.execute(() -> {
						final Conversation c = findConversationByUuid(uuid);
						if (c == null) {
							Log.d(Config.LOGTAG, "received mark read intent for unknown conversation (" + uuid + ")");
							return;
						}
						try {
							restoredFromDatabaseLatch.await();
							sendReadMarker(c, null);
						} catch (InterruptedException e) {
							Log.d(Config.LOGTAG, "unable to process notification read marker for conversation " + c.getName());
						}

					});
					break;
				case ACTION_SNOOZE:
					mNotificationExecutor.execute(() -> {
						final Conversation c = findConversationByUuid(uuid);
						if (c == null) {
							Log.d(Config.LOGTAG, "received snooze intent for unknown conversation (" + uuid + ")");
							return;
						}
						c.setMutedTill(System.currentTimeMillis() + 30 * 60 * 1000);
						mNotificationService.clear(c);
						updateConversation(c);
					});
				case AudioManager.RINGER_MODE_CHANGED_ACTION:
				case NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED:
					if (dndOnSilentMode()) {
						refreshAllPresences();
					}
					break;
				case Intent.ACTION_SCREEN_ON:
					deactivateGracePeriod();
				case Intent.ACTION_SCREEN_OFF:
					if (awayWhenScreenOff()) {
						refreshAllPresences();
					}
					break;
				case ACTION_FCM_TOKEN_REFRESH:
					refreshAllFcmTokens();
					break;
				case ACTION_IDLE_PING:
					scheduleNextIdlePing();
					break;
				case ACTION_FCM_MESSAGE_RECEIVED:
					pushedAccountHash = intent.getStringExtra("account");
					Log.d(Config.LOGTAG, "push message arrived in service. account=" + pushedAccountHash);
					break;
				case Intent.ACTION_SEND:
					Uri uri = intent.getData();
					if (uri != null) {
						Log.d(Config.LOGTAG, "received uri permission for " + uri.toString());
					}
					return START_STICKY;
			}
		}
		synchronized (this) {
			WakeLockHelper.acquire(wakeLock);
			boolean pingNow = ConnectivityManager.CONNECTIVITY_ACTION.equals(action) || (Config.POST_CONNECTIVITY_CHANGE_PING_INTERVAL > 0 && ACTION_POST_CONNECTIVITY_CHANGE.equals(action));
			HashSet<Account> pingCandidates = new HashSet<>();
			for (Account account : accounts) {
				pingNow |= processAccountState(account,
						interactive,
						"ui".equals(action),
						CryptoHelper.getAccountFingerprint(account,PhoneHelper.getAndroidId(this)).equals(pushedAccountHash),
						pingCandidates);
			}
			if (pingNow || action == ACTION_IDLE_PING) { //ALF AM-103 added ||
				for (Account account : pingCandidates) {
					final boolean lowTimeout = isInLowPingTimeoutMode(account);
					account.getXmppConnection().sendPing();
					Log.d(Config.LOGTAG, account.getLogJid() + " send ping (action=" + action + ",lowTimeout=" + Boolean.toString(lowTimeout) + ")");
					scheduleWakeUpCall(lowTimeout ? Config.LOW_PING_TIMEOUT : Config.PING_TIMEOUT, account.getUuid().hashCode());
				}
			}
			WakeLockHelper.release(wakeLock);
		}
		if (SystemClock.elapsedRealtime() - mLastExpiryRun.get() >= Config.EXPIRY_INTERVAL) {
			expireOldMessages();
		}
		return START_STICKY;
	}

	//AM-480
	private boolean isInNativeCall() {
		try {
			final TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
			if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
				return true;
			}
		} catch (Exception e) {
			Log.d(Config.LOGTAG, "Failed getting Telephony service ");
		}
		return false;
	}

	//AM-492
	public int getCancelledCall() {
		return cancelledCall;
	}

	//ALF AM-431
	public void handleCallSetupMessage(Account account, TwilioCall call) {
		if (account != null && call != null) {
			SoundPoolManager.getInstance(XmppConnectionService.this).stopRinging();

			if (call.getStatus().equalsIgnoreCase("reject")) {
				//stop CallActivity
				//AM-558 should not stop call activity yet if it was a group call in case others accept
				//AM-592
				if (currentTwilioCall != null && currentTwilioCall.getRoomTitle() != null && currentTwilioCall.getRoomTitle().startsWith("#")) {
					return;
				}

				Intent intent1 = new Intent("callActivityFinish");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
				callHandler.removeCallbacksAndMessages(null);
				//TODO: notify user of rejection from other party

				currentTwilioCall = null;
			} else if (call.getStatus().equalsIgnoreCase("cancel")) {
				Intent intent1 = new Intent("callActivityFinish");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
				callHandler.removeCallbacksAndMessages(null);

				cancelledCall = call.getCallId(); //AM-492
				this.getNotificationService().dismissCallNotification();

				//ALF AM-421
				if (call.getCaller() != null) {
					//Conversation c = find(getConversations(), account, Jid.of(call.getCaller())); //DJF DJF AM-438
					Conversation c = findOrCreateConversation(account, Jid.of(call.getCaller()), false, true);
					if (c != null) {
						Message msg = Message.createCallStatusMessage(c, Message.STATUS_CALL_MISSED);
						msg.markUnread(); //AM#10
						getNotificationService().notifyMissedCall(c); //ALF AM-468
						c.add(msg);
						databaseBackend.createMessage(msg);
					}
				}
				currentTwilioCall = null;
			} else if (call.getStatus().equalsIgnoreCase("busy")) { //ALF AM-420
				//just play busy tone but do nothing else. Up to them to cancel
				this.playTone(ToneGenerator.TONE_SUP_BUSY);
				callHandler.removeCallbacksAndMessages(null);
				currentTwilioCall = null;
			} else if (call.getStatus().equalsIgnoreCase("accept")) {
				// other party accepted call
				//stop CallActivity
				Intent intent1 = new Intent("callActivityFinish");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);

				//ALF AM-558 if call is already active we don't want to initCall here
				if (currentTwilioCall != null && call.getCallId() == currentTwilioCall.getCallId() &&
						currentTwilioCall.getStatus().equals("inprogress")) {
					return;
				}

				if (currentTwilioCall != null) {
					currentTwilioCall.setStatus("inprogress"); //AM-558
					callManager.initCall(currentTwilioCall); //AM-478
				}
			}
		}
	}

	//ALF AM-420
	private void playTone(int tone) {
		ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
		toneGenerator.startTone(tone, 4000);
		busyHandler.postDelayed(() -> {
			toneGenerator.stopTone();
			toneGenerator.release();
			Intent intent1 = new Intent("callActivityFinish");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
		},4500);
	}

	//AM#14 (next 3)
	public void handleLifecycleUpdate(ProcessLifecycleListener.LifecycleStatus value) {
		switch (value) {
			case CREATE:
			case DESTROY:
			case PAUSE:
			case RESUME:
				break;
			case START: // app moved to foreground
				if (!ignoreLifecycleUpdate && SystemSecurityInfo.isBiometricPINReady(this)) {
					updateProcessLifecycle();
				}
				ignoreLifecycleUpdate = false; //activity won't need to track return to app this way
				updateSecurityInfo(); //AM#52, AM#53
				updateSmsInfo();
				break;
			case STOP: // app moved to background
				mLastGlacierUsage.set(SystemClock.elapsedRealtime());
				needsProcessLifecycleUpdate = false;
				break;
			default:
				break;
		}
	}

	public void setChoosingFile(boolean choosingFile) {
		ignoreLifecycleUpdate = choosingFile;
	}

	public void setLastBioauthAttemptFailed(boolean failed) {
		lastBioauthFailed = failed;
	}

	private boolean processAccountState(Account account, boolean interactive, boolean isUiAction, boolean isAccountPushed, HashSet<Account> pingCandidates) {
		boolean pingNow = false;
		if (account.getStatus().isAttemptReconnect()) {
			//ALF AM-57 placeholder
			/*if (!mVPNConnectionService.hasVpnConnection()) {
				account.setStatus(Account.State.NO_INTERNET);
				if (statusListener != null) {
					statusListener.onStatusChanged(account);
				}
			}*/
			if (!hasInternetConnection()) {
				account.setStatus(Account.State.NO_INTERNET);
				if (statusListener != null) {
					statusListener.onStatusChanged(account);
				}
			} else {
				if (account.getStatus() == Account.State.NO_INTERNET) {
					account.setStatus(Account.State.OFFLINE);
					if (statusListener != null) {
						statusListener.onStatusChanged(account);
					}
				}
				if (account.getStatus() == Account.State.ONLINE) {
					synchronized (mLowPingTimeoutMode) {
						long lastReceived = account.getXmppConnection().getLastPacketReceived();
						long lastSent = account.getXmppConnection().getLastPingSent();
						long pingInterval = isUiAction ? Config.PING_MIN_INTERVAL * 1000 : Config.PING_MAX_INTERVAL * 1000;
						long msToNextPing = (Math.max(lastReceived, lastSent) + pingInterval) - SystemClock.elapsedRealtime();
						int pingTimeout = mLowPingTimeoutMode.contains(account.getJid().asBareJid()) ? Config.LOW_PING_TIMEOUT * 1000 : Config.PING_TIMEOUT * 1000;
						long pingTimeoutIn = (lastSent + pingTimeout) - SystemClock.elapsedRealtime();
						if (lastSent > lastReceived) {
							if (pingTimeoutIn < 0) {
								Log.d(Config.LOGTAG, account.getLogJid() + ": ping timeout");
								this.reconnectAccount(account, true, interactive);
							} else {
								int secs = (int) (pingTimeoutIn / 1000);
								this.scheduleWakeUpCall(secs, account.getUuid().hashCode());
							}
						} else {
							pingCandidates.add(account);
							if (isAccountPushed) {
								pingNow = true;
								if (mLowPingTimeoutMode.add(account.getJid().asBareJid())) {
									Log.d(Config.LOGTAG, account.getLogJid() + ": entering low ping timeout mode");
								}
							} else if (msToNextPing <= 0) {
								pingNow = true;
							} else {
								this.scheduleWakeUpCall((int) (msToNextPing / 1000), account.getUuid().hashCode());
								if (mLowPingTimeoutMode.remove(account.getJid().asBareJid())) {
									Log.d(Config.LOGTAG, account.getLogJid() + ": leaving low ping timeout mode");
								}
							}
						}
					}
				} else if (account.getStatus() == Account.State.OFFLINE) {
					reconnectAccount(account, true, interactive);
				} else if (account.getStatus() == Account.State.CONNECTING) {
					long secondsSinceLastConnect = (SystemClock.elapsedRealtime() - account.getXmppConnection().getLastConnect()) / 1000;
					long secondsSinceLastDisco = (SystemClock.elapsedRealtime() - account.getXmppConnection().getLastDiscoStarted()) / 1000;
					long discoTimeout = Config.CONNECT_DISCO_TIMEOUT - secondsSinceLastDisco;
					long timeout = Config.CONNECT_TIMEOUT - secondsSinceLastConnect;
					if (timeout < 0) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": time out during connect reconnecting (secondsSinceLast=" + secondsSinceLastConnect + ")");
						account.getXmppConnection().resetAttemptCount(false);
						reconnectAccount(account, true, interactive);
					} else if (discoTimeout < 0) {
						account.getXmppConnection().sendDiscoTimeout();
						scheduleWakeUpCall((int) Math.min(timeout, discoTimeout), account.getUuid().hashCode());
					} else {
						scheduleWakeUpCall((int) Math.min(timeout, discoTimeout), account.getUuid().hashCode());
					}
				} else {
					if (account.getXmppConnection().getTimeToNextAttempt() <= 0) {
						reconnectAccount(account, true, interactive);
					}
				}
			}
		}
		return pingNow;
	}

	public boolean isDataSaverDisabled() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		return !connectivityManager.isActiveNetworkMetered()
				|| connectivityManager.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED;
	}

	private void directReply(Conversation conversation, String body, final boolean dismissAfterReply) {
		Message message = new Message(conversation, body, conversation.getNextEncryption());
		message.markUnread();
		sendMessage(message);
		if (dismissAfterReply) {
			markRead(conversation, true);
		} else {
			mNotificationService.pushFromDirectReply(message);
		}
	}

	private boolean dndOnSilentMode() {
		return getBooleanPreference(SettingsActivity.DND_ON_SILENT_MODE, R.bool.dnd_on_silent_mode);
	}

	private boolean manuallyChangePresence() {
		return getBooleanPreference(SettingsActivity.MANUALLY_CHANGE_PRESENCE, R.bool.manually_change_presence);
	}

	private boolean treatVibrateAsSilent() {
		return getBooleanPreference(SettingsActivity.TREAT_VIBRATE_AS_SILENT, R.bool.treat_vibrate_as_silent);
	}

	private boolean awayWhenScreenOff() {
		return getBooleanPreference(SettingsActivity.AWAY_WHEN_SCREEN_IS_OFF, R.bool.away_when_screen_off);
	}

	private String getCompressPicturesPreference() {
		return getPreferences().getString("picture_compression", getResources().getString(R.string.picture_compression));
	}

	private Presence.Status getTargetPresence() {
		if (dndOnSilentMode() && isPhoneSilenced()) {
			return Presence.Status.DND;
		} else if (awayWhenScreenOff() && !isInteractive()) {
			return Presence.Status.AWAY;
		} else {
			return Presence.Status.ONLINE;
		}
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public boolean isInteractive() {
		try {
			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			final boolean isScreenOn = pm.isInteractive();
			return isScreenOn;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private boolean isPhoneSilenced() {
		final boolean notificationDnd;
		final NotificationManager notificationManager = getSystemService(NotificationManager.class);
		final int filter = notificationManager == null ? NotificationManager.INTERRUPTION_FILTER_UNKNOWN : notificationManager.getCurrentInterruptionFilter();
		notificationDnd = filter >= NotificationManager.INTERRUPTION_FILTER_PRIORITY;
		final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		final int ringerMode = audioManager == null ? AudioManager.RINGER_MODE_NORMAL : audioManager.getRingerMode();
		try {
			if (treatVibrateAsSilent()) {
				return notificationDnd || ringerMode != AudioManager.RINGER_MODE_NORMAL;
			} else {
				return notificationDnd || ringerMode == AudioManager.RINGER_MODE_SILENT;
			}
		} catch (Throwable throwable) {
			Log.d(Config.LOGTAG, "platform bug in isPhoneSilenced (" + throwable.getMessage() + ")");
			return notificationDnd;
		}
	}

	private void resetAllAttemptCounts(boolean reallyAll, boolean retryImmediately) {
		Log.d(Config.LOGTAG, "resetting all attempt counts");
		for (Account account : accounts) {
			if (account.hasErrorStatus() || reallyAll) {
				final XmppConnection connection = account.getXmppConnection();
				if (connection != null) {
					connection.resetAttemptCount(retryImmediately);
				}
			}
			if (account.setShowErrorNotification(true)) {
				databaseBackend.updateAccount(account);
			}
		}
		mNotificationService.updateErrorNotification();
	}

	private void dismissErrorNotifications() {
		for (final Account account : this.accounts) {
			if (account.hasErrorStatus()) {
				Log.d(Config.LOGTAG, account.getLogJid() + ": dismissing error notification");
				if (account.setShowErrorNotification(false)) {
					databaseBackend.updateAccount(account);
				}
			}
		}
	}

	private void expireOldMessages() {
		expireOldMessages(false);
	}

	public void expireOldMessages(final boolean resetHasMessagesLeftOnServer) {
		mLastExpiryRun.set(SystemClock.elapsedRealtime());
		mDatabaseWriterExecutor.execute(() -> {
			long timestamp = getAutomaticMessageDeletionDate();
			if (timestamp > 0) {
				databaseBackend.expireOldMessages(timestamp);
				synchronized (XmppConnectionService.this.conversations) {
					for (Conversation conversation : XmppConnectionService.this.conversations) {
						conversation.expireOldMessages(timestamp);
						if (resetHasMessagesLeftOnServer) {
							conversation.messagesLoaded.set(true);
							conversation.setHasMessagesLeftOnServer(true);
						}
					}
				}
				updateConversationUi();
			}
		});
	}

	public boolean hasInternetConnection() {
		final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			final NetworkInfo activeNetwork = cm == null ? null : cm.getActiveNetworkInfo();
			return activeNetwork != null && (activeNetwork.isConnected() || activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET);
		} catch (RuntimeException e) {
			Log.d(Config.LOGTAG, "unable to check for internet connection", e);
			return true; //if internet connection can not be checked it is probably best to just try
		}
	}


	@SuppressLint("TrulyRandom")
	@Override
	public void onCreate() {
		if (Compatibility.runsTwentySix()) { //ALF AM-184 and mForce following
			mNotificationService.initializeChannels();
		}
		mForceDuringOnCreate.set(Compatibility.runsAndTargetsTwentySix(this));
		toggleForegroundService();
		this.destroyed = false;
		OmemoSetting.load(this);
		ExceptionHelper.init(getApplicationContext());
		processListener = new ProcessLifecycleListener(this); //AM#14
		try {
			Security.insertProviderAt(Conscrypt.newProvider(), 1);
		} catch (Throwable throwable) {
			Log.e(Config.LOGTAG,"unable to initialize security provider", throwable);
		}
		Resolver.init(this);
		this.mRandom = new SecureRandom();
		updateMemorizingTrustmanager();
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;
		this.mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(final String key, final Bitmap bitmap) {
				return bitmap.getByteCount() / 1024;
			}
		};
		if (mLastActivity == 0) {
			mLastActivity = getPreferences().getLong(SETTING_LAST_ACTIVITY_TS, System.currentTimeMillis());
		}

		//ALF AM-603 test
		this.fileObserver = new ConversationsFileObserver(
				getApplicationContext().getExternalFilesDir(null).getAbsolutePath()
		) {
			@Override
			public void onEvent(int event, String path) {
				markFileDeleted(path);
			}
		};

		Log.d(Config.LOGTAG, "initializing database...");
		this.databaseBackend = DatabaseBackend.getInstance(getApplicationContext());
		Log.d(Config.LOGTAG, "restoring accounts...");
		this.accounts = databaseBackend.getAccounts();
		final SharedPreferences.Editor editor = getPreferences().edit();
		if (this.accounts.size() == 0 && Arrays.asList("Sony", "Sony Ericsson").contains(Build.MANUFACTURER)) {
			editor.putBoolean(SettingsActivity.KEEP_FOREGROUND_SERVICE, true);
			Log.d(Config.LOGTAG, Build.MANUFACTURER + " is on blacklist. enabling foreground service");
		}
		final boolean hasEnabledAccounts = hasEnabledAccounts();
		editor.putBoolean(EventReceiver.SETTING_ENABLED_ACCOUNTS, hasEnabledAccounts()).apply();
		editor.apply();
		toggleSetProfilePictureActivity(hasEnabledAccounts);

		//AM#52, AM#53
		if (accounts.size() > 0 && needsSecurityInfoUpdate) {
			updateSecurityInfo();
		}

		if (accounts.size() > 0) {
			getSmsInfo();
		}

		restoreFromDatabase();

		if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED )) {
			startContactObserver();
		}
		if (Compatibility.hasStoragePermission(this)) {
			Log.d(Config.LOGTAG, "starting file observer");
			mFileAddingExecutor.execute(this.fileObserver::startWatching);
			mFileAddingExecutor.execute(this::checkForDeletedFiles);
		}
		/*if (Config.supportOpenPgp()) {
			this.pgpServiceConnection = new OpenPgpServiceConnection(this, "org.sufficientlysecure.keychain", new OpenPgpServiceConnection.OnBound() {
				@Override
				public void onBound(IOpenPgpService2 service) {
					for (Account account : accounts) {
						final PgpDecryptionService pgp = account.getPgpDecryptionService();
						if (pgp != null) {
							pgp.continueDecryption(true);
						}
					}
				}

				@Override
				public void onError(Exception e) {
				}
			});
			this.pgpServiceConnection.bindToService();
		}*/

		this.pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Glacier:XmppConnectionService");

		//AM-410
		//twilioCallListener = this;
		currentTwilioCall = null;

		//AM-541
		glacierOwnerInForeground = true;
		try {
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_USER_BACKGROUND);
			filter.addAction(Intent.ACTION_USER_FOREGROUND);
			registerReceiver(mUserBroadcastReceiver, filter);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		//ALF AM-344
		try {
			mHandler = new Handler(this);
			Intent icsopenvpnService = new Intent(IOpenVPNAPIService.class.getName());
			icsopenvpnService.setPackage("com.glaciersecurity.glaciercore");
			bindService(icsopenvpnService, this, Context.BIND_AUTO_CREATE);
		} catch (SecurityException e) { //ALF AM-194 added Security
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		toggleForegroundService();
		updateUnreadCountBadge();
		toggleScreenEventReceiver();
		scheduleNextIdlePing();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		intentFilter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
		registerReceiver(this.mInternalEventReceiver, intentFilter);
		mForceDuringOnCreate.set(false);
		toggleForegroundService();
	}

	private void checkForDeletedFiles() {
		if (destroyed) {
			Log.d(Config.LOGTAG, "Do not check for deleted files because service has been destroyed");
			return;
		}
		final long start = SystemClock.elapsedRealtime();
		final List<DatabaseBackend.FilePathInfo> relativeFilePaths = databaseBackend.getFilePathInfo();
		final List<DatabaseBackend.FilePathInfo> changed = new ArrayList<>();
		for(final DatabaseBackend.FilePathInfo filePath : relativeFilePaths) {
			if (destroyed) {
				Log.d(Config.LOGTAG, "Stop checking for deleted files because service has been destroyed");
				return;
			}
			final File file = fileBackend.getFileForPath(filePath.path);
			if (filePath.setDeleted(!file.exists())) {
				changed.add(filePath);
			}
		}
		final long duration = SystemClock.elapsedRealtime() - start;
		Log.d(Config.LOGTAG,"found "+changed.size()+" changed files on start up. total="+relativeFilePaths.size()+". ("+duration+"ms)");
		if (changed.size() > 0) {
			databaseBackend.markFilesAsChanged(changed);
			markChangedFiles(changed);
		}
	}

	public void startContactObserver() {
		getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, new ContentObserver(null) {
			@Override
			public void onChange(boolean selfChange) {
				super.onChange(selfChange);
				if (restoredFromDatabaseLatch.getCount() == 0) {
					loadPhoneContacts();
				}
			}
		});
	}


	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		if (level >= TRIM_MEMORY_COMPLETE) {
			Log.d(Config.LOGTAG, "clear cache due to low memory");
			getBitmapCache().evictAll();
		}
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(this.mInternalEventReceiver);
			unregisterReceiver(this.mInternalScreenEventReceiver);
			unregisterReceiver(this.mUserBroadcastReceiver); //AM-541
		} catch (IllegalArgumentException e) {
			//ignored
		}
		SoundPoolManager.getInstance(XmppConnectionService.this).stopRinging();

		//ALF AM-444
		callManager.stopCallAudio();
		SoundPoolManager.getInstance(XmppConnectionService.this).release();
		currentTwilioCall = null;
		callHandler.removeCallbacksAndMessages(null);

		destroyed = false;
		fileObserver.stopWatching();
		super.onDestroy();
	}

	public void restartFileObserver() {
		Log.d(Config.LOGTAG,"restarting file observer");
		mFileAddingExecutor.execute(this.fileObserver::restartWatching);
		mFileAddingExecutor.execute(this::checkForDeletedFiles);
	}

	public void toggleScreenEventReceiver() {
		if (awayWhenScreenOff()) {   //    && !manuallyChangePresence()) {    // DJF Updated for Advanced Settings 08-27-19
			final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			registerReceiver(this.mInternalScreenEventReceiver, filter);
		} else {
			try {
				unregisterReceiver(this.mInternalScreenEventReceiver);
			} catch (IllegalArgumentException e) {
				//ignored
			}
		}
	}

	//ALF AM-184 next 3 from Conversations, removed old one
	public void toggleForegroundService() {
		toggleForegroundService(false);
	}

	private void toggleForegroundService(boolean force) {
		final boolean status;
		if (force || mForceDuringOnCreate.get() || mForceForegroundService.get() || (Compatibility.keepForegroundService(this) && hasEnabledAccounts())) {
			final Notification notification = this.mNotificationService.createForegroundNotification();
			startForeground(NotificationService.FOREGROUND_NOTIFICATION_ID, notification);
			if (!mForceForegroundService.get()) {
				mNotificationService.notify(NotificationService.FOREGROUND_NOTIFICATION_ID, notification);
			}
			status = true;
		} else {
			stopForeground(true);
			status = false;
		}
		if (!mForceForegroundService.get()) {
			mNotificationService.dismissForcedForegroundNotification(); //if the channel was changed the previous call might fail
		}
		Log.d(Config.LOGTAG,"ForegroundService: "+(status?"on":"off"));
	}

	public boolean foregroundNotificationNeedsUpdatingWhenErrorStateChanges() {
		return !mForceForegroundService.get() && Compatibility.keepForegroundService(this) && hasEnabledAccounts();
	}

	@Override
	public void onTaskRemoved(final Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		if ((Compatibility.keepForegroundService(this) && hasEnabledAccounts()) || mForceForegroundService.get()) {
			Log.d(Config.LOGTAG, "ignoring onTaskRemoved because foreground service is activated");
		} else {
			this.logoutAndSave(false);
		}
	}

	private void logoutAndSave(boolean stop) {
		int activeAccounts = 0;
		for (final Account account : accounts) {
			if (account.getStatus() != Account.State.DISABLED) {
				databaseBackend.writeRoster(account.getRoster());
				activeAccounts++;
			}
			if (account.getXmppConnection() != null) {
				new Thread(() -> disconnect(account, false)).start();
			}
		}
		if (stop || activeAccounts == 0) {
			Log.d(Config.LOGTAG, "good bye");
			stopSelf();
		}
	}

	private void schedulePostConnectivityChange() {
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (alarmManager == null) {
			return;
		}
		final long triggerAtMillis = SystemClock.elapsedRealtime() + (Config.POST_CONNECTIVITY_CHANGE_PING_INTERVAL * 1000);
		final Intent intent = new Intent(this, EventReceiver.class);
		intent.setAction(ACTION_POST_CONNECTIVITY_CHANGE);
		try {
			final PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE);
			alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
		} catch (RuntimeException e) {
			Log.e(Config.LOGTAG, "unable to schedule alarm for post connectivity change", e);
		}
	}

	public void scheduleWakeUpCall(int seconds, int requestCode) {
		final long timeToWake = SystemClock.elapsedRealtime() + (seconds < 0 ? 1 : seconds + 1) * 1000;
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (alarmManager == null) {
			return;
		}
		final Intent intent = new Intent(this, EventReceiver.class);
		intent.setAction("ping");
		try {
			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
			alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToWake, pendingIntent);
		} catch (RuntimeException e) {
			Log.e(Config.LOGTAG, "unable to schedule alarm for ping", e);
		}
	}

	private void scheduleNextIdlePing() {
		//ALF AM-103 changed IDLE to MIN
		final long timeToWake = SystemClock.elapsedRealtime() + (Config.PING_MIN_INTERVAL * 1000);
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (alarmManager == null) {
			return;
		}
		final Intent intent = new Intent(this, EventReceiver.class);
		intent.setAction(ACTION_IDLE_PING);
		try {
			PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
			alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToWake, pendingIntent);
		} catch (RuntimeException e) {
			Log.d(Config.LOGTAG, "unable to schedule alarm for idle ping", e);
		}
	}

	public XmppConnection createConnection(final Account account) {
		final XmppConnection connection = new XmppConnection(account, this);
		connection.setOnMessagePacketReceivedListener(this.mMessageParser);
		connection.setOnStatusChangedListener(this.statusListener);
		connection.setOnPresencePacketReceivedListener(this.mPresenceParser);
		connection.setOnUnregisteredIqPacketReceivedListener(this.mIqParser);
		connection.setOnJinglePacketReceivedListener(this.jingleListener);
		connection.setOnBindListener(this.mOnBindListener);
		connection.setOnMessageAcknowledgeListener(this.mOnMessageAcknowledgedListener);
		connection.addOnAdvancedStreamFeaturesAvailableListener(this.mMessageArchiveService);
		connection.addOnAdvancedStreamFeaturesAvailableListener(this.mAvatarService);
		AxolotlService axolotlService = account.getAxolotlService();
		if (axolotlService != null) {
			connection.addOnAdvancedStreamFeaturesAvailableListener(axolotlService);
		}
		return connection;
	}

	public void sendChatState(Conversation conversation) {
		if (sendChatStates()) {
			MessagePacket packet = mMessageGenerator.generateChatState(conversation);
			sendMessagePacket(conversation.getAccount(), packet);
		}
	}

	private void sendFileMessage(final Message message, final boolean delay) {
		Log.d(Config.LOGTAG, "send file message");
		final Account account = message.getConversation().getAccount();
		if (account.httpUploadAvailable(fileBackend.getFile(message, false).getSize())
				|| message.getConversation().getMode() == Conversation.MODE_MULTI) {
			mHttpConnectionManager.createNewUploadConnection(message, delay);
		} else {
			mJingleConnectionManager.createNewConnection(message);
		}
	}

	public void sendMessage(final Message message) {
		sendMessage(message, false, false);
	}

	private void sendMessage(final Message message, final boolean resend, final boolean delay) {
		final Account account = message.getConversation().getAccount();
		if (account.setShowErrorNotification(true)) {
			databaseBackend.updateAccount(account);
			mNotificationService.updateErrorNotification();
		}
		final Conversation conversation = (Conversation) message.getConversation();
		account.deactivateGracePeriod();

		if (QuickConversationsService.isQuicksy() && conversation.getMode() == Conversation.MODE_SINGLE) {
			final Contact contact = conversation.getContact();
			if (!contact.showInRoster() && contact.getOption(Contact.Options.SYNCED_VIA_OTHER)) {
				Log.d(Config.LOGTAG, account.getLogJid()+": adding "+contact.getJid()+" on sending message");
				createContact(contact, true);
			}
		}

		MessagePacket packet = null;
		final boolean addToConversation = (conversation.getMode() != Conversation.MODE_MULTI
				|| !Patches.BAD_MUC_REFLECTION.contains(account.getServerIdentity()))
				&& !message.edited();
		boolean saveInDb = addToConversation;
		message.setStatus(Message.STATUS_WAITING);

		if (message.getEncryption() != Message.ENCRYPTION_NONE && conversation.getMode() == Conversation.MODE_MULTI && conversation.isPrivateAndNonAnonymous()) {
			if (conversation.setAttribute(Conversation.ATTRIBUTE_FORMERLY_PRIVATE_NON_ANONYMOUS, true)) {
				updateConversation(conversation);
			}
		}

		if (account.isOnlineAndConnected()) {
			switch (message.getEncryption()) {
				case Message.ENCRYPTION_NONE:
					if (message.needsUploading()) {
						if (account.httpUploadAvailable(fileBackend.getFile(message, false).getSize())
								|| conversation.getMode() == Conversation.MODE_MULTI
								|| message.fixCounterpart()) {
							this.sendFileMessage(message, delay);
						} else {
							break;
						}
					} else {
						packet = mMessageGenerator.generateChat(message);
					}
					break;
				case Message.ENCRYPTION_PGP:
				case Message.ENCRYPTION_DECRYPTED:
					/*if (message.needsUploading()) {
						if (account.httpUploadAvailable(fileBackend.getFile(message, false).getSize())
								|| conversation.getMode() == Conversation.MODE_MULTI
								|| message.fixCounterpart()) {
							this.sendFileMessage(message, delay);
						} else {
							break;
						}
					} else {
						packet = mMessageGenerator.generatePgpChat(message);
					}*/
					break;
				case Message.ENCRYPTION_AXOLOTL:
					message.setFingerprint(account.getAxolotlService().getOwnFingerprint());
					if (message.needsUploading()) {
						if (account.httpUploadAvailable(fileBackend.getFile(message, false).getSize())
								|| conversation.getMode() == Conversation.MODE_MULTI
								|| message.fixCounterpart()) {
							this.sendFileMessage(message, delay);
						} else {
							break;
						}
					} else {
						XmppAxolotlMessage axolotlMessage = account.getAxolotlService().fetchAxolotlMessageFromCache(message);
						if (axolotlMessage == null) {
							account.getAxolotlService().preparePayloadMessage(message, delay);
						} else {
							packet = mMessageGenerator.generateAxolotlChat(message, axolotlMessage);
						}
					}
					break;

			}
			if (packet != null) {
				if (account.getXmppConnection().getFeatures().sm()
						|| (conversation.getMode() == Conversation.MODE_MULTI && message.getCounterpart().isBareJid())) {
					message.setStatus(Message.STATUS_UNSEND);
				} else {
					message.setStatus(Message.STATUS_SEND);
				}
			}
		} else {
			switch (message.getEncryption()) {
				case Message.ENCRYPTION_DECRYPTED:
					/*if (!message.needsUploading()) {
						String pgpBody = message.getEncryptedBody();
						String decryptedBody = message.getBody();
						message.setBody(pgpBody); //TODO might throw NPE
						message.setEncryption(Message.ENCRYPTION_PGP);
						if (message.edited()) {
							message.setBody(decryptedBody);
							message.setEncryption(Message.ENCRYPTION_DECRYPTED);
							if (!databaseBackend.updateMessage(message, message.getEditedId())) {
								Log.e(Config.LOGTAG,"error updated message in DB after edit");
							}
							updateConversationUi();
							return;
						} else {
							databaseBackend.createMessage(message);
							saveInDb = false;
							message.setBody(decryptedBody);
							message.setEncryption(Message.ENCRYPTION_DECRYPTED);
						}
					}*/
					break;
				case Message.ENCRYPTION_AXOLOTL:
					message.setFingerprint(account.getAxolotlService().getOwnFingerprint());
					break;
			}
		}


		boolean mucMessage = conversation.getMode() == Conversation.MODE_MULTI && message.getType() != Message.TYPE_PRIVATE;
		if (mucMessage) {
			message.setCounterpart(conversation.getMucOptions().getSelf().getFullJid());
		}

		if (resend) {
			if (packet != null && addToConversation) {
				if (account.getXmppConnection().getFeatures().sm() || mucMessage) {
					markMessage(message, Message.STATUS_UNSEND);
				} else {
					markMessage(message, Message.STATUS_SEND);
				}
			}
		} else {
			if (addToConversation) {
				conversation.add(message);
			}
			if (saveInDb) {
				databaseBackend.createMessage(message);
			} else if (message.edited()) {
				if (!databaseBackend.updateMessage(message, message.getEditedId())) {
					Log.e(Config.LOGTAG,"error updated message in DB after edit");
				}
			}
			updateConversationUi();
		}
		if (packet != null) {
			if (delay) {
				mMessageGenerator.addDelay(packet, message.getTimeSent());
			}
			if (conversation.setOutgoingChatState(Config.DEFAULT_CHATSTATE)) {
				if (this.sendChatStates()) {
					packet.addChild(ChatState.toElement(conversation.getOutgoingChatState()));
				}
			}
			sendMessagePacket(account, packet);
		}
	}

	private void sendUnsentMessages(final Conversation conversation) {
		conversation.findWaitingMessages(message -> resendMessage(message, true));
	}

	public void resendMessage(final Message message, final boolean delay) {
		sendMessage(message, true, delay);
	}

	public void fetchRosterFromServer(final Account account) {
		final IqPacket iqPacket = new IqPacket(IqPacket.TYPE.GET);
		if (!"".equals(account.getRosterVersion())) {
			Log.d(Config.LOGTAG, account.getLogJid()
					+ ": fetching roster version " + account.getRosterVersion());
		} else {
			Log.d(Config.LOGTAG, account.getLogJid() + ": fetching roster");
		}
		iqPacket.query(Namespace.ROSTER).setAttribute("ver", account.getRosterVersion());
		sendIqPacket(account, iqPacket, mIqParser);
	}

	public void fetchBookmarks(final Account account) {
		final IqPacket iqPacket = new IqPacket(IqPacket.TYPE.GET);
		final Element query = iqPacket.query("jabber:iq:private");
		query.addChild("storage", Namespace.BOOKMARKS);
		final OnIqPacketReceived callback = (a, response) -> {
			if (response.getType() == IqPacket.TYPE.RESULT) {
				final Element query1 = response.query();
				final Element storage = query1.findChild("storage", "storage:bookmarks");
				processBookmarks(a, storage, false);
			} else {
				Log.d(Config.LOGTAG, a.getLogJid() + ": could not fetch bookmarks");
			}
		};
		sendIqPacket(account, iqPacket, callback);
	}

	public void processBookmarks(Account account, Element storage, final boolean pep) {
		final Set<Jid> previousBookmarks = account.getBookmarkedJids();
		final HashMap<Jid, Bookmark> bookmarks = new HashMap<>();
		final boolean synchronizeWithBookmarks = synchronizeWithBookmarks();
		if (storage != null) {
			for (final Element item : storage.getChildren()) {
				if (item.getName().equals("conference")) {
					final Bookmark bookmark = Bookmark.parse(item, account);
					Bookmark old = bookmarks.put(bookmark.getJid(), bookmark);
					if (old != null && old.getBookmarkName() != null && bookmark.getBookmarkName() == null) {
						bookmark.setBookmarkName(old.getBookmarkName());
					}
					if (bookmark.getJid() == null) {
						continue;
					}

					previousBookmarks.remove(bookmark.getJid().asBareJid());
					Conversation conversation = find(bookmark);
					if (conversation != null) {
						if (conversation.getMode() != Conversation.MODE_MULTI) {
							continue;
						}
						bookmark.setConversation(conversation);
						if (pep && synchronizeWithBookmarks && !bookmark.autojoin()) {
							Log.d(Config.LOGTAG, account.getLogJid()+": archiving conference ("+conversation.getJid()+") after receiving pep");
							archiveConversation(conversation, false);
						}
					} else if (synchronizeWithBookmarks && bookmark.autojoin()) {
						conversation = findOrCreateConversation(account, bookmark.getFullJid(), true, true, false);
						bookmark.setConversation(conversation);
					}
				}
			}
			if (pep && synchronizeWithBookmarks) {
				Log.d(Config.LOGTAG, account.getLogJid() + ": " + previousBookmarks.size() + " bookmarks have been removed");
				for (Jid jid : previousBookmarks) {
					final Conversation conversation = find(account, jid);
					if (conversation != null && conversation.getMucOptions().getError() == MucOptions.Error.DESTROYED) {
						Log.d(Config.LOGTAG, account.getLogJid()+": archiving destroyed conference ("+conversation.getJid()+") after receiving pep");
						archiveConversation(conversation, false);
					}
				}
			}
		}
		account.setBookmarks(new CopyOnWriteArrayList<>(bookmarks.values()));
	}

	public void pushBookmarks(Account account) {
		if (account.getXmppConnection().getFeatures().bookmarksConversion()) {
			pushBookmarksPep(account);
		} else {
			pushBookmarksPrivateXml(account);
		}
	}

	private void pushBookmarksPrivateXml(Account account) {
		Log.d(Config.LOGTAG, account.getLogJid() + ": pushing bookmarks via private xml");
		IqPacket iqPacket = new IqPacket(IqPacket.TYPE.SET);
		Element query = iqPacket.query("jabber:iq:private");
		Element storage = query.addChild("storage", "storage:bookmarks");
		for (Bookmark bookmark : account.getBookmarks()) {
			storage.addChild(bookmark);
		}
		sendIqPacket(account, iqPacket, mDefaultIqHandler);
	}

	private void pushBookmarksPep(Account account) {
		Log.d(Config.LOGTAG, account.getLogJid() + ": pushing bookmarks via pep");
		Element storage = new Element("storage", "storage:bookmarks");
		for (Bookmark bookmark : account.getBookmarks()) {
			storage.addChild(bookmark);
		}
		pushNodeAndEnforcePublishOptions(account,Namespace.BOOKMARKS,storage, PublishOptions.persistentWhitelistAccess());

	}

	private void pushNodeAndEnforcePublishOptions(final Account account, final String node, final Element element, final Bundle options) {
		pushNodeAndEnforcePublishOptions(account, node, element, options, true);

	}

	private void pushNodeAndEnforcePublishOptions(final Account account, final String node, final Element element, final Bundle options, final boolean retry) {
		final IqPacket packet = mIqGenerator.publishElement(node, element, options);
		sendIqPacket(account, packet, (a, response) -> {
			if (response.getType() == IqPacket.TYPE.RESULT) {
				return;
			}
			if (retry && PublishOptions.preconditionNotMet(response)) {
				pushNodeConfiguration(account, node, options, new OnConfigurationPushed() {
					@Override
					public void onPushSucceeded() {
						pushNodeAndEnforcePublishOptions(account, node, element, options, false);
					}

					@Override
					public void onPushFailed() {
						Log.d(Config.LOGTAG, account.getLogJid()+": unable to push node configuration ("+node+")");
					}
				});
			} else {
				Log.d(Config.LOGTAG, account.getLogJid()+": error publishing bookmarks (retry="+Boolean.toString(retry)+") "+response);
			}
		});
	}

	private void restoreFromDatabase() {
		synchronized (this.conversations) {
			final Map<String, Account> accountLookupTable = new Hashtable<>();
			for (Account account : this.accounts) {
				accountLookupTable.put(account.getUuid(), account);
			}
			Log.d(Config.LOGTAG, "restoring conversations...");
			final long startTimeConversationsRestore = SystemClock.elapsedRealtime();
			this.conversations.addAll(databaseBackend.getConversations(Conversation.STATUS_AVAILABLE));
			for (Iterator<Conversation> iterator = conversations.listIterator(); iterator.hasNext(); ) {
				Conversation conversation = iterator.next();
				Account account = accountLookupTable.get(conversation.getAccountUuid());
				if (account != null) {
					conversation.setAccount(account);
				} else {
					Log.e(Config.LOGTAG, "unable to restore Conversations with " + conversation.getJid());
					iterator.remove();
				}
			}
			long diffConversationsRestore = SystemClock.elapsedRealtime() - startTimeConversationsRestore;
			Log.d(Config.LOGTAG, "finished restoring conversations in " + diffConversationsRestore + "ms");
			Runnable runnable = () -> {
				long deletionDate = getAutomaticMessageDeletionDate();
				mLastExpiryRun.set(SystemClock.elapsedRealtime());
				if (deletionDate > 0) {
					Log.d(Config.LOGTAG, "deleting messages that are older than " + AbstractGenerator.getTimestamp(deletionDate));
					databaseBackend.expireOldMessages(deletionDate);
				}
				Log.d(Config.LOGTAG, "restoring roster...");
				for (Account account : accounts) {
					databaseBackend.readRoster(account.getRoster());
					account.initAccountServices(XmppConnectionService.this); //roster needs to be loaded at this stage
				}
				getBitmapCache().evictAll();
				loadPhoneContacts();
				Log.d(Config.LOGTAG, "restoring messages...");
				final long startMessageRestore = SystemClock.elapsedRealtime();
				final Conversation quickLoad = QuickLoader.get(this.conversations);
				if (quickLoad != null) {
					restoreMessages(quickLoad);
					updateConversationUi();
					final long diffMessageRestore = SystemClock.elapsedRealtime() - startMessageRestore;
					Log.d(Config.LOGTAG,"quickly restored "+quickLoad.getName()+" after " + diffMessageRestore + "ms");
				}
				for (Conversation conversation : this.conversations) {
					//ALF AM-53 startup message deletion code
					List<Message> messages = databaseBackend.getMessages(conversation, Config.PAGE_SIZE);
					ArrayList<String> removedIds = new ArrayList();
					for (int m=messages.size()-1; m>=0; m--){
						Message message = messages.get(m);
						if (message.getTimer() != Message.TIMER_NONE && message.getTimeRemaining() <= 0) {
							removedIds.add(message.getUuid());
							messages.remove(m);
						}
					}
					if (removedIds.size() > 0) {
						databaseBackend.removeDisappearingMessages(removedIds);
						conversation.addAll(0, messages);
					}

					if (quickLoad != conversation) {
						restoreMessages(conversation);
					}
				}
				mNotificationService.finishBacklog(false);
				restoredFromDatabaseLatch.countDown();
				final long diffMessageRestore = SystemClock.elapsedRealtime() - startMessageRestore;
				Log.d(Config.LOGTAG, "finished restoring messages in " + diffMessageRestore + "ms");
				updateConversationUi();
			};
			mDatabaseReaderExecutor.execute(runnable); //will contain one write command (expiry) but that's fine
		}
	}

	private void restoreMessages(Conversation conversation) {
		conversation.addAll(0, databaseBackend.getMessages(conversation, Config.PAGE_SIZE));
		conversation.findUnsentTextMessages(message -> markMessage(message, Message.STATUS_WAITING));
		conversation.findUnreadMessages(message -> mNotificationService.pushFromBacklog(message));
	}

	public void loadPhoneContacts() {
		mContactMergerExecutor.execute(() -> {
			Map<Jid, JabberIdContact> contacts = JabberIdContact.load(this);
			Log.d(Config.LOGTAG, "start merging phone contacts with roster");
			for (Account account : accounts) {
				List<Contact> withSystemAccounts = account.getRoster().getWithSystemAccounts(JabberIdContact.class);
				for (JabberIdContact jidContact : contacts.values()) {
					final Contact contact = account.getRoster().getContact(jidContact.getJid());
					boolean needsCacheClean = contact.setPhoneContact(jidContact);
					if (needsCacheClean) {
						getAvatarService().clear(contact);
					}
					withSystemAccounts.remove(contact);
				}
				for (Contact contact : withSystemAccounts) {
					boolean needsCacheClean = contact.unsetPhoneContact(JabberIdContact.class);
					if (needsCacheClean) {
						getAvatarService().clear(contact);
					}
				}
			}
			Log.d(Config.LOGTAG, "finished merging phone contacts");
			mShortcutService.refresh(mInitialAddressbookSyncCompleted.compareAndSet(false, true));
			updateRosterUi();
			mQuickConversationsService.considerSync();
		});
	}


	public void syncRoster(final Account account) {
		mRosterSyncTaskManager.execute(account, () -> databaseBackend.writeRoster(account.getRoster()));
	}

	public List<Conversation> getConversations() {
		return this.conversations;
	}

	private void markFileDeleted(final String path) {
		final File file = new File(path);
		final boolean isInternalFile = fileBackend.isInternalFile(file);
		final List<String> uuids = databaseBackend.markFileAsDeleted(file, isInternalFile);
		Log.d(Config.LOGTAG, "deleted file " + path+" internal="+isInternalFile+", database hits="+uuids.size());
		markUuidsAsDeletedFiles(uuids);
	}

	private void markUuidsAsDeletedFiles(List<String> uuids) {
		boolean deleted = false;
		for (Conversation conversation : getConversations()) {
			deleted |= conversation.markAsDeleted(uuids);
		}
		if (deleted) {
			updateConversationUi();
		}
	}

	private void markChangedFiles(List<DatabaseBackend.FilePathInfo> infos) {
		boolean changed = false;
		for (Conversation conversation : getConversations()) {
			changed |= conversation.markAsChanged(infos);
		}
		if (changed) {
			updateConversationUi();
		}
	}

	public void populateWithOrderedConversations(final List<Conversation> list) {
		populateWithOrderedConversations(list, true, true);
	}

	public void populateWithOrderedConversations(final List<Conversation> list, final boolean includeNoFileUpload) {
		populateWithOrderedConversations(list, includeNoFileUpload, true);
	}

	public void populateWithOrderedConversations(final List<Conversation> list, final boolean includeNoFileUpload, final boolean sort) {
		final List<String> orderedUuids;
		if (sort) {
			orderedUuids = null;
		} else {
			orderedUuids = new ArrayList<>();
			for(Conversation conversation : list) {
				orderedUuids.add(conversation.getUuid());
			}
		}
		list.clear();
		if (includeNoFileUpload) {
			list.addAll(getConversations());
		} else {
			for (Conversation conversation : getConversations()) {
				if (conversation.getMode() == Conversation.MODE_SINGLE
						|| (conversation.getAccount().httpUploadAvailable() && conversation.getMucOptions().participating())) {
					list.add(conversation);
				}
			}
		}
		try {
			if (orderedUuids != null) {
				Collections.sort(list, (a, b) -> {
					final int indexA = orderedUuids.indexOf(a.getUuid());
					final int indexB = orderedUuids.indexOf(b.getUuid());
					if (indexA == -1 || indexB == -1 || indexA == indexB) {
						return a.compareTo(b);
					}
					return indexA - indexB;
				});
			} else {
				Collections.sort(list);
			}
		} catch (IllegalArgumentException e) {
			//ignore
		}
	}

	public void loadMoreMessages(final Conversation conversation, final long timestamp, final OnMoreMessagesLoaded callback) {
		if (XmppConnectionService.this.getMessageArchiveService().queryInProgress(conversation, callback)) {
			return;
		} else if (timestamp == 0) {
			return;
		}
		Log.d(Config.LOGTAG, "load more messages for " + conversation.getName() + " prior to " + MessageGenerator.getTimestamp(timestamp));
		final Runnable runnable = () -> {
			final Account account = conversation.getAccount();
			List<Message> messages = databaseBackend.getMessages(conversation, 50, timestamp);
			if (messages.size() > 0) {
				conversation.addAll(0, messages);
				callback.onMoreMessagesLoaded(messages.size(), conversation);
			} else if (conversation.hasMessagesLeftOnServer()
					&& account.isOnlineAndConnected()
					&& conversation.getLastClearHistory().getTimestamp() == 0) {
				final boolean mamAvailable;
				if (conversation.getMode() == Conversation.MODE_SINGLE) {
					mamAvailable = account.getXmppConnection().getFeatures().mam() && !conversation.getContact().isBlocked();
				} else {
					mamAvailable = conversation.getMucOptions().mamSupport();
				}
				if (mamAvailable) {
					MessageArchiveService.Query query = getMessageArchiveService().query(conversation, new MamReference(0), timestamp, false);
					if (query != null) {
						query.setCallback(callback);
						callback.informUser(R.string.fetching_history_from_server);
					} else {
						callback.informUser(R.string.not_fetching_history_retention_period);
					}

				}
			}
		};
		mDatabaseReaderExecutor.execute(runnable);
	}

	public List<Account> getAccounts() {
		return this.accounts;
	}

	/**
	 * This will find all conferences with the contact as member and also the conference that is the contact (that 'fake' contact is used to store the avatar)
	 */
	public List<Conversation> findAllConferencesWith(Contact contact) {
		ArrayList<Conversation> results = new ArrayList<>();
		for (final Conversation c : conversations) {
			if (c.getMode() == Conversation.MODE_MULTI && (c.getJid().asBareJid().equals(contact.getJid().asBareJid()) || c.getMucOptions().isContactInRoom(contact))) {
				results.add(c);
			}
		}
		return results;
	}

	public Conversation find(final Iterable<Conversation> haystack, final Contact contact) {
		for (final Conversation conversation : haystack) {
			if (conversation.getContact() == contact) {
				return conversation;
			}
		}
		return null;
	}

	public Conversation find(final Iterable<Conversation> haystack, final Account account, final Jid jid) {
		if (jid == null) {
			return null;
		}
		for (final Conversation conversation : haystack) {
			if ((account == null || conversation.getAccount() == account)
					&& (conversation.getJid().asBareJid().equals(jid.asBareJid()))) {
				return conversation;
			}
		}
		return null;
	}

	public boolean isConversationsListEmpty(final Conversation ignore) {
		synchronized (this.conversations) {
			final int size = this.conversations.size();
			return size == 0 || size == 1 && this.conversations.get(0) == ignore;
		}
	}

	public boolean isConversationStillOpen(final Conversation conversation) {
		synchronized (this.conversations) {
			for (Conversation current : this.conversations) {
				if (current == conversation) {
					return true;
				}
			}
		}
		return false;
	}

	public Conversation findOrCreateConversation(Account account, Jid jid, boolean muc, final boolean async) {
		return this.findOrCreateConversation(account, jid, muc, false, async);
	}

	public Conversation findOrCreateConversation(final Account account, final Jid jid, final boolean muc, final boolean joinAfterCreate, final boolean async) {
		return this.findOrCreateConversation(account, jid, muc, joinAfterCreate, null, async);
	}

	public Conversation findOrCreateConversation(final Account account, final Jid jid, final boolean muc, final boolean joinAfterCreate, final MessageArchiveService.Query query, final boolean async) {
		synchronized (this.conversations) {
			Conversation conversation = find(account, jid);
			if (conversation != null) {
				return conversation;
			}
			conversation = databaseBackend.findConversation(account, jid);
			final boolean loadMessagesFromDb;
			if (conversation != null) {
				conversation.setStatus(Conversation.STATUS_AVAILABLE);
				conversation.setAccount(account);
				if (muc) {
					conversation.setMode(Conversation.MODE_MULTI);
					conversation.setContactJid(jid);
				} else {
					conversation.setMode(Conversation.MODE_SINGLE);
					conversation.setContactJid(jid.asBareJid());
				}
				updateConversation(conversation);
				loadMessagesFromDb = conversation.messagesLoaded.compareAndSet(true, false);
			} else {
				String conversationName;
				Contact contact = account.getRoster().getContact(jid);
				if (contact != null) {
					conversationName = contact.getDisplayName();
				} else {
					conversationName = jid.getLocal();
				}
				if (muc) {
					conversation = new Conversation(conversationName, account, jid,
							Conversation.MODE_MULTI);
				} else {
					conversation = new Conversation(conversationName, account, jid.asBareJid(),
							Conversation.MODE_SINGLE);
				}
				this.databaseBackend.createConversation(conversation);
				loadMessagesFromDb = false;
			}
			final Conversation c = conversation;
			final Runnable runnable = () -> {
				if (loadMessagesFromDb) {
					c.addAll(0, databaseBackend.getMessages(c, Config.PAGE_SIZE));
					updateConversationUi();
					c.messagesLoaded.set(true);
				}
				if (account.getXmppConnection() != null
						&& !c.getContact().isBlocked()
						&& account.getXmppConnection().getFeatures().mam()
						&& !muc) {
					if (query == null) {
						mMessageArchiveService.query(c);
					} else {
						if (query.getConversation() == null) {
							mMessageArchiveService.query(c, query.getStart(), query.isCatchup());
						}
					}
				}
				if (joinAfterCreate) {
					joinMuc(c);
				}
			};
			if (async) {
				mDatabaseReaderExecutor.execute(runnable);
			} else {
				runnable.run();
			}
			this.conversations.add(conversation);
			updateConversationUi();
			return conversation;
		}
	}

	public void archiveConversation(Conversation conversation) {
		archiveConversation(conversation, true);
	}

	private void archiveConversation(Conversation conversation, final boolean maySyncronizeWithBookmarks) {
		getNotificationService().clear(conversation);
		conversation.setStatus(Conversation.STATUS_ARCHIVED);
		conversation.setNextMessage(null);
		synchronized (this.conversations) {
			getMessageArchiveService().kill(conversation);
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				Bookmark bookmark = conversation.getBookmark(); //ALF AM-78 moved out of if
				if (conversation.getAccount().getStatus() == Account.State.ONLINE) {
					if (maySyncronizeWithBookmarks && bookmark != null && synchronizeWithBookmarks()) {
						if (conversation.getMucOptions().getError() == MucOptions.Error.DESTROYED) {
							Account account = bookmark.getAccount();
							bookmark.setConversation(null);
							account.getBookmarks().remove(bookmark);
							pushBookmarks(account);
						} else if (bookmark.autojoin()) {
							bookmark.setAutojoin(false);
							pushBookmarks(bookmark.getAccount());
						}
					}
				}
				conversation.getAccount().getBookmarks().remove(bookmark); //ALF AM-78
				leaveMuc(conversation);
			} else {
				if (conversation.getContact().getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
					stopPresenceUpdatesTo(conversation.getContact());
				}
			}
			updateConversation(conversation);
			this.conversations.remove(conversation);
			updateConversationUi();
		}
	}

	public void stopPresenceUpdatesTo(Contact contact) {
		Log.d(Config.LOGTAG, "Canceling presence request from " + contact.getJid().toString());
		sendPresencePacket(contact.getAccount(), mPresenceGenerator.stopPresenceUpdatesTo(contact));
		contact.resetOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST);
	}

	//ALF AM-228 here to createAccount
	Map<String, Set<PreKeyRecord>> preKeyRecordSet = new Hashtable<>();
	public void setExistingPreKeyRecords(Account account, Set<PreKeyRecord> records) {
		preKeyRecordSet.clear();
		preKeyRecordSet.put(account.getJid().toEscapedString(), records);
	}

	public Set<PreKeyRecord> getExistingPreKeyRecords(final Account account) {
		if (preKeyRecordSet.containsKey(account.getJid().toEscapedString())) {
			return preKeyRecordSet.get(account.getJid().toEscapedString());
		}
		return null;
	}

	/*Account existingAccount = null;
	public void setExistingAccount(Account existingAcct) {
		existingAccount = existingAcct;
	}

	public Account getExistingAccount(String barJidStr) {
		if (existingAccount != null &&
				barJidStr.equalsIgnoreCase(existingAccount.getJid().asBareJid().toEscapedString())) {
			return existingAccount;
		}
		return null;
	}*/

	public void createAccount(final Account account, boolean newAccount) {
		//ALF AM-228 (if...also added newAccount above)
		//if (newAccount) {
		account.initAccountServices(this);
		//}
		//existingAccount = null;

		databaseBackend.createAccount(account);

		//ALF AM-228
		//if (!newAccount && account.getAxolotlService().getExistingKeyPair() != null) {
		//	databaseBackend.storeOwnIdentityKeyPair(account, account.getAxolotlService().getExistingKeyPair());
		//}

		this.accounts.add(account);
		this.reconnectAccountInBackground(account);
		updateAccountUi();
		syncEnabledAccountSetting();
		toggleForegroundService();
	}

	//AM#52, AM#53
	public void createCognitoAccount(CognitoAccount cacct) {
		databaseBackend.createCognitoAccount(cacct);

		if (needsSecurityInfoUpdate) {
			updateSecurityInfo();
		}

		updateSmsInfo();
	}

	private void syncEnabledAccountSetting() {
		final boolean hasEnabledAccounts = hasEnabledAccounts();
		getPreferences().edit().putBoolean(EventReceiver.SETTING_ENABLED_ACCOUNTS, hasEnabledAccounts).apply();
		toggleSetProfilePictureActivity(hasEnabledAccounts);
	}

	private void toggleSetProfilePictureActivity(final boolean enabled) {
		try {
			final ComponentName name = new ComponentName(this, ChooseAccountForProfilePictureActivity.class);
			final int targetState =  enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
			getPackageManager().setComponentEnabledSetting(name, targetState, PackageManager.DONT_KILL_APP);
		} catch (IllegalStateException e) {
			Log.d(Config.LOGTAG,"unable to toggle profile picture actvitiy");
		}
	}

	public void createAccountFromKey(final String alias, final OnAccountCreated callback) {
		new Thread(() -> {
			try {
				final X509Certificate[] chain = KeyChain.getCertificateChain(this, alias);
				final X509Certificate cert = chain != null && chain.length > 0 ? chain[0] : null;
				if (cert == null) {
					callback.informUser(R.string.unable_to_parse_certificate);
					return;
				}
				Pair<Jid, String> info = CryptoHelper.extractJidAndName(cert);
				if (info == null) {
					callback.informUser(R.string.certificate_does_not_contain_jid);
					return;
				}
				if (findAccountByJid(info.first) == null) {
					Account account = new Account(info.first, "");
					account.setPrivateKeyAlias(alias);
					account.setOption(Account.OPTION_DISABLED, true);
					account.setDisplayName(info.second);
					createAccount(account, true);
					callback.onAccountCreated(account);
					if (Config.X509_VERIFICATION) {
						try {
							getMemorizingTrustManager().getNonInteractive(account.getJid().getDomain().toEscapedString()).checkClientTrusted(chain, "RSA");
						} catch (CertificateException e) {
							callback.informUser(R.string.certificate_chain_is_not_trusted);
						}
					}
				} else {
					callback.informUser(R.string.account_already_exists);
				}
			} catch (Exception e) {
				e.printStackTrace();
				callback.informUser(R.string.unable_to_parse_certificate);
			}
		}).start();

	}

	public void updateKeyInAccount(final Account account, final String alias) {
		Log.d(Config.LOGTAG, account.getLogJid() + ": update key in account " + alias);
		try {
			X509Certificate[] chain = KeyChain.getCertificateChain(XmppConnectionService.this, alias);
			Log.d(Config.LOGTAG, account.getLogJid() + " loaded certificate chain");
			Pair<Jid, String> info = CryptoHelper.extractJidAndName(chain[0]);
			if (info == null) {
				showErrorToastInUi(R.string.certificate_does_not_contain_jid);
				return;
			}
			if (account.getJid().asBareJid().equals(info.first)) {
				account.setPrivateKeyAlias(alias);
				account.setDisplayName(info.second);
				databaseBackend.updateAccount(account);
				if (Config.X509_VERIFICATION) {
					try {
						getMemorizingTrustManager().getNonInteractive().checkClientTrusted(chain, "RSA");
					} catch (CertificateException e) {
						showErrorToastInUi(R.string.certificate_chain_is_not_trusted);
					}
					account.getAxolotlService().regenerateKeys(true);
				}
			} else {
				showErrorToastInUi(R.string.jid_does_not_match_certificate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean updateAccount(final Account account) {
		if (databaseBackend.updateAccount(account)) {
			account.setShowErrorNotification(true);
			this.statusListener.onStatusChanged(account);
			databaseBackend.updateAccount(account);
			reconnectAccountInBackground(account);
			updateAccountUi();
			getNotificationService().updateErrorNotification();
			toggleForegroundService();
			syncEnabledAccountSetting();
			return true;
		} else {
			return false;
		}
	}

	public void updateAccountPasswordOnServer(final Account account, final String newPassword, final OnAccountPasswordChanged callback) {
		final IqPacket iq = getIqGenerator().generateSetPassword(account, newPassword);
		sendIqPacket(account, iq, (a, packet) -> {
			if (packet.getType() == IqPacket.TYPE.RESULT) {
				a.setPassword(newPassword);
				a.setOption(Account.OPTION_MAGIC_CREATE, false);
				databaseBackend.updateAccount(a);
				callback.onPasswordChangeSucceeded();
			} else {
				callback.onPasswordChangeFailed();
			}
		});
	}

	public void deleteAccount(final Account account) {
		synchronized (this.conversations) {
			for (final Conversation conversation : conversations) {
				if (conversation.getAccount() == account) {
					if (conversation.getMode() == Conversation.MODE_MULTI) {
						leaveMuc(conversation);
					}
					conversations.remove(conversation);
				}
			}
			if (account.getXmppConnection() != null) {
				new Thread(() -> disconnect(account, true)).start();
			}
			final Runnable runnable = () -> {
				if (!databaseBackend.deleteAccount(account)) {
					Log.d(Config.LOGTAG, account.getLogJid() + ": unable to delete account");
				}
			};
			mDatabaseWriterExecutor.execute(runnable);
			this.accounts.remove(account);
			this.mRosterSyncTaskManager.clear(account);
			updateAccountUi();
			getNotificationService().updateErrorNotification();
			syncEnabledAccountSetting();
			toggleForegroundService();
		}
	}

	public void setOnConversationListChangedListener(OnConversationUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnConversationUpdates.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as ConversationListChangedListener");
			}
			this.mNotificationService.setIsInForeground(this.mOnConversationUpdates.size() > 0);
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnConversationListChangedListener(OnConversationUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnConversationUpdates.remove(listener);
			this.mNotificationService.setIsInForeground(this.mOnConversationUpdates.size() > 0);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnShowErrorToastListener(OnShowErrorToast listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnShowErrorToasts.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnShowErrorToastListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnShowErrorToastListener(OnShowErrorToast onShowErrorToast) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnShowErrorToasts.remove(onShowErrorToast);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnAccountListChangedListener(OnAccountUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnAccountUpdates.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnAccountListChangedtListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnAccountListChangedListener(OnAccountUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnAccountUpdates.remove(listener);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnCaptchaRequestedListener(OnCaptchaRequested listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnCaptchaRequested.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnCaptchaRequestListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnCaptchaRequestedListener(OnCaptchaRequested listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnCaptchaRequested.remove(listener);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnRosterUpdateListener(final OnRosterUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnRosterUpdates.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnRosterUpdateListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnRosterUpdateListener(final OnRosterUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnRosterUpdates.remove(listener);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnUpdateBlocklistListener(final OnUpdateBlocklist listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnUpdateBlocklist.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnUpdateBlocklistListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnUpdateBlocklistListener(final OnUpdateBlocklist listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnUpdateBlocklist.remove(listener);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnKeyStatusUpdatedListener(final OnKeyStatusUpdated listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnKeyStatusUpdated.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnKeyStatusUpdateListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnNewKeysAvailableListener(final OnKeyStatusUpdated listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnKeyStatusUpdated.remove(listener);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	public void setOnMucRosterUpdateListener(OnMucRosterUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			remainingListeners = checkListeners();
			if (!this.mOnMucRosterUpdate.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnMucRosterListener");
			}
		}
		if (remainingListeners) {
			switchToForeground();
		}
	}

	public void removeOnMucRosterUpdateListener(final OnMucRosterUpdate listener) {
		final boolean remainingListeners;
		synchronized (LISTENER_LOCK) {
			this.mOnMucRosterUpdate.remove(listener);
			remainingListeners = checkListeners();
		}
		if (remainingListeners) {
			switchToBackground();
		}
	}

	//AM#14 (next 2)
	public void setOnProcessLifecycleUpdateListener(OnProcessLifecycleUpdate listener) {
		synchronized (LISTENER_LOCK) {
			if (!this.mOnProcessLifecycleUpdates.add(listener)) {
				Log.w(Config.LOGTAG,listener.getClass().getName()+" is already registered as OnProcessLifecycleUpdateListener");
			}
		}

		if (needsProcessLifecycleUpdate) {
			updateProcessLifecycle();
		}
	}

	public void removeOnProcessLifecycleUpdateListener(OnProcessLifecycleUpdate listener) {
		synchronized (LISTENER_LOCK) {
			this.mOnProcessLifecycleUpdates.remove(listener);
		}
	}

	public boolean checkListeners() {
		return (this.mOnAccountUpdates.size() == 0
				&& this.mOnConversationUpdates.size() == 0
				&& this.mOnRosterUpdates.size() == 0
				&& this.mOnCaptchaRequested.size() == 0
				&& this.mOnMucRosterUpdate.size() == 0
				&& this.mOnUpdateBlocklist.size() == 0
				&& this.mOnShowErrorToasts.size() == 0
				&& this.mOnKeyStatusUpdated.size() == 0);
	}

	private void switchToForeground() {
		final boolean broadcastLastActivity = broadcastLastActivity();
		for (Conversation conversation : getConversations()) {
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				conversation.getMucOptions().resetChatState();
			} else {
				conversation.setIncomingChatState(Config.DEFAULT_CHATSTATE);
			}
		}
		for (Account account : getAccounts()) {
			if (account.getStatus() == Account.State.ONLINE) {
				account.deactivateGracePeriod();
				final XmppConnection connection = account.getXmppConnection();
				if (connection != null) {
					if (connection.getFeatures().csi()) {
						connection.sendActive();
					}
					if (broadcastLastActivity) {
						sendPresence(account, false); //send new presence but don't include idle because we are not
					}
				}
			}
		}
		Log.d(Config.LOGTAG, "app switched into foreground");
	}

	private void switchToBackground() {
		final boolean broadcastLastActivity = broadcastLastActivity();
		if (broadcastLastActivity) {
			mLastActivity = System.currentTimeMillis();
			final SharedPreferences.Editor editor = getPreferences().edit();
			editor.putLong(SETTING_LAST_ACTIVITY_TS, mLastActivity);
			editor.apply();
		}
		for (Account account : getAccounts()) {
			if (account.getStatus() == Account.State.ONLINE) {
				XmppConnection connection = account.getXmppConnection();
				if (connection != null) {
					if (broadcastLastActivity) {
						sendPresence(account, true);
					}
					if (connection.getFeatures().csi()) {
						connection.sendInactive();
					}
				}
			}
		}
		this.mNotificationService.setIsInForeground(false);
		Log.d(Config.LOGTAG, "app switched into background");
	}

	private void connectMultiModeConversations(Account account) {
		List<Conversation> conversations = getConversations();
		for (Conversation conversation : conversations) {
			if (conversation.getMode() == Conversation.MODE_MULTI && conversation.getAccount() == account) {
				joinMuc(conversation);
			}
		}
	}

	public void joinMuc(Conversation conversation) {
		joinMuc(conversation, null, false);
	}

	public void joinMuc(Conversation conversation, boolean followedInvite) {
		joinMuc(conversation, null, followedInvite);
	}

	private void joinMuc(Conversation conversation, final OnConferenceJoined onConferenceJoined) {
		joinMuc(conversation, onConferenceJoined, false);
	}

	private void joinMuc(Conversation conversation, final OnConferenceJoined onConferenceJoined, final boolean followedInvite) {
		Account account = conversation.getAccount();
		account.pendingConferenceJoins.remove(conversation);
		account.pendingConferenceLeaves.remove(conversation);
		if (account.getStatus() == Account.State.ONLINE) {
			sendPresencePacket(account, mPresenceGenerator.leave(conversation.getMucOptions()));
			conversation.resetMucOptions();
			if (onConferenceJoined != null) {
				conversation.getMucOptions().flagNoAutoPushConfiguration();
			}
			conversation.setHasMessagesLeftOnServer(false);
			fetchConferenceConfiguration(conversation, new OnConferenceConfigurationFetched() {

				private void join(Conversation conversation) {
					Account account = conversation.getAccount();
					final MucOptions mucOptions = conversation.getMucOptions();

					/*if (mucOptions.nonanonymous() && !mucOptions.membersOnly() && !conversation.getBooleanAttribute("accept_non_anonymous", false)) {
						mucOptions.setError(MucOptions.Error.NON_ANONYMOUS);
						updateConversationUi();
						if (onConferenceJoined != null) {
							onConferenceJoined.onConferenceJoined(conversation);
						}
						return;
					}*/ //ALF AM-270 Conversations update caused this, we don't want it

					final Jid joinJid = mucOptions.getSelf().getFullJid();
					Log.d(Config.LOGTAG, account.getLogJid().toString() + ": joining conversation " + Tools.logJid(joinJid.toString()));
					PresencePacket packet = mPresenceGenerator.selfPresence(account, Presence.Status.ONLINE, mucOptions.nonanonymous() || onConferenceJoined != null);
					packet.setTo(joinJid);
					Element x = packet.addChild("x", "http://jabber.org/protocol/muc");
					if (conversation.getMucOptions().getPassword() != null) {
						x.addChild("password").setContent(mucOptions.getPassword());
					}

					if (mucOptions.mamSupport()) {
						// Use MAM instead of the limited muc history to get history
						x.addChild("history").setAttribute("maxchars", "0");
					} else {
						// Fallback to muc history
						x.addChild("history").setAttribute("since", PresenceGenerator.getTimestamp(conversation.getLastMessageTransmitted().getTimestamp()));
					}
					sendPresencePacket(account, packet);
					if (onConferenceJoined != null) {
						onConferenceJoined.onConferenceJoined(conversation);
					}
					if (!joinJid.equals(conversation.getJid())) {
						conversation.setContactJid(joinJid);
						updateConversation(conversation);
					}

					if (mucOptions.mamSupport()) {
						getMessageArchiveService().catchupMUC(conversation);
					}
					if (mucOptions.isPrivateAndNonAnonymous()) {
						fetchConferenceMembers(conversation);
						if (followedInvite && conversation.getBookmark() == null) {
							saveConversationAsBookmark(conversation, null);
						}
					}
					sendUnsentMessages(conversation);
				}

				@Override
				public void onConferenceConfigurationFetched(Conversation conversation) {
					if (conversation.getStatus() == Conversation.STATUS_ARCHIVED) {
						Log.d(Config.LOGTAG, account.getLogJid()+": conversation ("+conversation.getJid()+") got archived before IQ result");
						return;
					}
					join(conversation);
				}

				@Override
				public void onFetchFailed(final Conversation conversation, Element error) {
					if (conversation.getStatus() == Conversation.STATUS_ARCHIVED) {
						Log.d(Config.LOGTAG, account.getLogJid()+": conversation ("+conversation.getJid()+") got archived before IQ result");
						return;
					}
					if (error != null && "remote-server-not-found".equals(error.getName())) {
						conversation.getMucOptions().setError(MucOptions.Error.SERVER_NOT_FOUND);
						updateConversationUi();
					} else {
						join(conversation);
						fetchConferenceConfiguration(conversation);
					}
				}
			});
			updateConversationUi();
		} else {
			account.pendingConferenceJoins.add(conversation);
			conversation.resetMucOptions();
			conversation.setHasMessagesLeftOnServer(false);
			updateConversationUi();
		}
	}

	private void fetchConferenceMembers(final Conversation conversation) {
		final Account account = conversation.getAccount();
		final AxolotlService axolotlService = account.getAxolotlService();
		final String[] affiliations = {"member", "admin", "owner"};
		OnIqPacketReceived callback = new OnIqPacketReceived() {

			private int i = 0;
			private boolean success = true;

			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				final boolean omemoEnabled = conversation.getNextEncryption() == Message.ENCRYPTION_AXOLOTL;
				Element query = packet.query("http://jabber.org/protocol/muc#admin");
				if (packet.getType() == IqPacket.TYPE.RESULT && query != null) {
					for (Element child : query.getChildren()) {
						if ("item".equals(child.getName())) {
							MucOptions.User user = AbstractParser.parseItem(conversation, child);
							if (!user.realJidMatchesAccount()) {
								boolean isNew = conversation.getMucOptions().updateUser(user);
								Contact contact = user.getContact();
								if (omemoEnabled
										&& isNew
										&& user.getRealJid() != null
										&& (contact == null || !contact.mutualPresenceSubscription())
										&& axolotlService.hasEmptyDeviceList(user.getRealJid())) {
									axolotlService.fetchDeviceIds(user.getRealJid());
								}
							}
						}
					}
				} else {
					success = false;
					Log.d(Config.LOGTAG, account.getLogJid() + ": could not request affiliation " + affiliations[i] + " in " + Tools.logJid(conversation.getJid()));
				}
				++i;
				if (i >= affiliations.length) {
					List<Jid> members = conversation.getMucOptions().getMembers(true);
					if (success) {
						List<Jid> cryptoTargets = conversation.getAcceptedCryptoTargets();
						boolean changed = false;
						for (ListIterator<Jid> iterator = cryptoTargets.listIterator(); iterator.hasNext(); ) {
							Jid jid = iterator.next();
							if (!members.contains(jid) && !members.contains(Jid.ofDomain(jid.getDomain()))) {
								iterator.remove();
								Log.d(Config.LOGTAG, account.getLogJid() + ": removed " + Tools.logJid(jid) + " from crypto targets of " + conversation.getName());
								changed = true;
							}
						}
						if (changed) {
							conversation.setAcceptedCryptoTargets(cryptoTargets);
							updateConversation(conversation);
						}
					}
					getAvatarService().clear(conversation);
					updateMucRosterUi();
					updateConversationUi();

					//ALF AM-228
					AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
					axolotlService.createSessionsIfNeeded(conversation);
					conversation.reloadFingerprints(axolotlService.getCryptoTargets(conversation));
					conversation.commitTrusts();
				}
			}
		};
		for (String affiliation : affiliations) {
			sendIqPacket(account, mIqGenerator.queryAffiliation(conversation, affiliation), callback);
		}
		//Log.d(Config.LOGTAG, account.getLogJid() + ": fetching members for " + Tools.logJid(conversation.getName().toString()));
	}

	public void providePasswordForMuc(Conversation conversation, String password) {
		if (conversation.getMode() == Conversation.MODE_MULTI) {
			conversation.getMucOptions().setPassword(password);
			if (conversation.getBookmark() != null) {
				if (synchronizeWithBookmarks()) {
					conversation.getBookmark().setAutojoin(true);
				}
				pushBookmarks(conversation.getAccount());
			}
			updateConversation(conversation);
			joinMuc(conversation);
		}
	}

	private boolean hasEnabledAccounts() {
		if (this.accounts == null) {
			return false;
		}

		for (Account account : this.accounts) {
			if (account.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	public void getAttachments(final Conversation conversation, int limit, final OnMediaLoaded onMediaLoaded) {
		getAttachments(conversation.getAccount(), conversation.getJid().asBareJid(), limit, onMediaLoaded);
	}

	public void getAttachments(final Account account, final Jid jid, final int limit, final OnMediaLoaded onMediaLoaded) {
		getAttachments(account.getUuid(),jid.asBareJid(),limit, onMediaLoaded);
	}


	public void getAttachments(final String account, final Jid jid, final int limit, final OnMediaLoaded onMediaLoaded) {
		new Thread(() -> onMediaLoaded.onMediaLoaded(fileBackend.convertToAttachments(databaseBackend.getRelativeFilePaths(account, jid, limit)))).start();
	}

	public void persistSelfNick(MucOptions.User self) {
		final Conversation conversation = self.getConversation();
		final boolean tookProposedNickFromBookmark = conversation.getMucOptions().isTookProposedNickFromBookmark();
		Jid full = self.getFullJid();
		if (!full.equals(conversation.getJid())) {
			Log.d(Config.LOGTAG, "nick changed. updating");
			conversation.setContactJid(full);
			updateConversation(conversation);
		}

		final Bookmark bookmark = conversation.getBookmark();
		final String bookmarkedNick = bookmark == null ? null : bookmark.getNick();
		if (bookmark != null && (tookProposedNickFromBookmark || TextUtils.isEmpty(bookmarkedNick)) && !full.getResource().equals(bookmarkedNick)) {
			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid() + ": persist nick '" + full.getResource() + "' into bookmark for " + conversation.getJid().asBareJid());
			bookmark.setNick(full.getResource());
			pushBookmarks(bookmark.getAccount());
		}
	}

	//AM-642
	public void setRoomsNickname(String nickname, boolean push, final UiCallback<Conversation> callback) {
		for (final Conversation conversation : getConversations()) {
			if (conversation.getMode() == Conversation.MODE_MULTI) {
				//if (push) {
					renameInMuc(conversation, nickname, callback);
				//}
				final MucOptions options = conversation.getMucOptions();
				final Jid joinJid = options.createJoinJid(nickname);
				conversation.setContactJid(joinJid);
				updateConversation(conversation);
			}
		}
		for (Account account : getAccounts()) {
			List<Bookmark> bookies = account.getBookmarks();
			for (Bookmark bookie : bookies) {
				bookie.setNick(nickname);
			}
			account.setBookmarks(new CopyOnWriteArrayList<>(bookies));
			if (push) {
				pushBookmarks(account);
			}
		}
	}

	public boolean renameInMuc(final Conversation conversation, final String nick, final UiCallback<Conversation> callback) {
		final MucOptions options = conversation.getMucOptions();
		final Jid joinJid = options.createJoinJid(nick);
		if (joinJid == null) {
			return false;
		}
		if (options.online()) {
			Account account = conversation.getAccount();
			options.setOnRenameListener(new OnRenameListener() {

				@Override
				public void onSuccess() {
					if (callback != null) {
						callback.success(conversation);
					}
				}

				@Override
				public void onFailure() {
					if (callback != null) {
						callback.error(R.string.nick_in_use, conversation);
					}
				}
			});

			PresencePacket packet = new PresencePacket();
			packet.setTo(joinJid);
			packet.setFrom(conversation.getAccount().getJid());

			/*String sig = account.getPgpSignature();
			if (sig != null) {
				packet.addChild("status").setContent("online");
				packet.addChild("x", "jabber:x:signed").setContent(sig);
			}*/
			sendPresencePacket(account, packet);
		} else {
			conversation.setContactJid(joinJid);
			updateConversation(conversation);
			if (conversation.getAccount().getStatus() == Account.State.ONLINE) {
				Bookmark bookmark = conversation.getBookmark();
				if (bookmark != null) {
					bookmark.setNick(nick);
					pushBookmarks(bookmark.getAccount());
				}
				joinMuc(conversation);
			}
		}
		return true;
	}

	public void leaveMuc(Conversation conversation) {
		leaveMuc(conversation, false);
	}

	private void leaveMuc(Conversation conversation, boolean now) {
		Account account = conversation.getAccount();
		account.pendingConferenceJoins.remove(conversation);
		account.pendingConferenceLeaves.remove(conversation);
		if (account.getStatus() == Account.State.ONLINE || now) {
			sendPresencePacket(conversation.getAccount(), mPresenceGenerator.leave(conversation.getMucOptions()));
			conversation.getMucOptions().setOffline();
			Bookmark bookmark = conversation.getBookmark();
			if (bookmark != null) {
				bookmark.setConversation(null);
			}
			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid()+ ": leaving muc " + conversation.getLogJid());
		} else {
			account.pendingConferenceLeaves.add(conversation);
		}
	}

	public String findConferenceServer(final Account account) {
		String server;
		if (account.getXmppConnection() != null) {
			server = account.getXmppConnection().getMucServer();
			if (server != null) {
				return server;
			}
		}
		for (Account other : getAccounts()) {
			if (other != account && other.getXmppConnection() != null) {
				server = other.getXmppConnection().getMucServer();
				if (server != null) {
					return server;
				}
			}
		}
		return null;
	}

	public void createPublicChannel(final Account account, final String name, final Jid address, final UiCallback<Conversation> callback) {
		joinMuc(findOrCreateConversation(account, address, true, false, true), conversation -> {
			final Bundle configuration = IqGenerator.defaultChannelConfiguration();
			if (!TextUtils.isEmpty(name)) {
				configuration.putString("muc#roomconfig_roomname", name);
			}
			pushConferenceConfiguration(conversation, configuration, new OnConfigurationPushed() {
				@Override
				public void onPushSucceeded() {
					saveConversationAsBookmark(conversation, name);
					callback.success(conversation);
				}

				@Override
				public void onPushFailed() {
					if (conversation.getMucOptions().getSelf().getAffiliation().ranks(MucOptions.Affiliation.OWNER)) {
						callback.error(R.string.unable_to_set_channel_configuration, conversation);
					} else {
						callback.error(R.string.joined_an_existing_channel, conversation);
					}
				}
			});
		});
	}

	public boolean createAdhocConference(final Account account,
										 final String name,
										 final Iterable<Jid> jids,
										 final boolean publicgroup, //ALF AM-88
										 final UiCallback<Conversation> callback) {
		Log.d(Config.LOGTAG, account.getLogJid().toString() + ": creating adhoc conference");
		if (account.getStatus() == Account.State.ONLINE) {
			try {
				String server = findConferenceServer(account);
				if (server == null) {
					if (callback != null) {
						callback.error(R.string.no_conference_server_found, null);
					}
					return false;
				}

				//ALF AM-78 if
				final Jid jid;
				if (name != null) {
					jid = Jid.of(name + "@" + server); //ALF AM-111
				} else {
					jid = Jid.of(CryptoHelper.pronounceable(getRNG()), server, null);
				}

				final Conversation conversation = findOrCreateConversation(account, jid, true, false, true);
				joinMuc(conversation, new OnConferenceJoined() {
					@Override
					public void onConferenceJoined(final Conversation conversation) {
						//ALF AM-88
						final Bundle configuration;
						if (publicgroup) {
							configuration = IqGenerator.defaultChannelConfiguration();
						} else {
							configuration = IqGenerator.defaultGroupChatConfiguration();
						}

						if (!TextUtils.isEmpty(name)) {
							configuration.putString("muc#roomconfig_roomname", name);
						}
						pushConferenceConfiguration(conversation, configuration, new OnConfigurationPushed() {
							@Override
							public void onPushSucceeded() {
								ArrayList<Jid> joiners = new ArrayList<>(); //ALF AM-51
								for (Jid invite : jids) {
									invite(conversation, invite);
									joiners.add(invite); //ALF AM-51
								}
								if (account.countPresences() > 1) {
									directInvite(conversation, account.getJid().asBareJid());
								}
								saveConversationAsBookmark(conversation, name);
								if (callback != null) {
									callback.success(conversation);
								}

								sendJoiningGroupMessage(conversation, joiners, true);//ALF AM-51
							}

							@Override
							public void onPushFailed() {
								archiveConversation(conversation);
								if (callback != null) {
									callback.error(R.string.conference_creation_failed, conversation);
									Log.d(Config.LOGTAG, "PROBLEM - The group chat creation failed!");
								}
							}
						});
					}
				});
				return true;
			} catch (IllegalArgumentException e) {
				if (callback != null) {
					callback.error(R.string.conference_creation_failed, null);
				}
				return false;
			}
		} else {
			if (callback != null) {
				callback.error(R.string.not_connected_try_again, null);
			}
			return false;
		}
	}

	/**
	 * //ALF AM-51
	 */
	public void sendJoiningGroupMessage(final Conversation conversation, List joiners, boolean includeAccount) {
		// sleep required so message goes out before conversation thread stopped
		// maybe show a spinner?
		//AM-535
		try { Thread.sleep(2000); } catch (InterruptedException ie) {}
		final Account account = conversation.getAccount();
		String dname = account.getDisplayName();
		if (dname == null) { dname = account.getUsername(); }
		if (!includeAccount) { dname = ""; }
		String bod = dname;
		for (int j=0; j<joiners.size(); j++) {
			Contact contact = account.getRoster().getContact((Jid)joiners.get(j));
			if (bod.length() > 0) {
				bod = bod + ", ";
			}
			if (contact != null) {
				bod = bod + contact.getDisplayName();
			} else {
				Jid cjid = (Jid)joiners.get(j);
				String[] splitter =  cjid.asBareJid().toString().split("@");
				bod = bod + splitter[0];
			}
		}

		bod = bod + " " + getString(R.string.added_to_group);
		Message message = new Message(conversation, bod, Message.ENCRYPTION_NONE);
		this.sendMessage(message);
	}

	public void fetchConferenceConfiguration(final Conversation conversation) {
		fetchConferenceConfiguration(conversation, null);
	}

	public void fetchConferenceConfiguration(final Conversation conversation, final OnConferenceConfigurationFetched callback) {
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.setTo(conversation.getJid().asBareJid());
		request.query("http://jabber.org/protocol/disco#info");
		sendIqPacket(conversation.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {

					final MucOptions mucOptions = conversation.getMucOptions();
					final Bookmark bookmark = conversation.getBookmark();
					final boolean sameBefore = StringUtils.equals(bookmark == null ? null : bookmark.getBookmarkName(), mucOptions.getName());

					if (mucOptions.updateConfiguration(new ServiceDiscoveryResult(packet))) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": muc configuration changed for " + conversation.getLogJid());
						updateConversation(conversation);
					}

					if (bookmark != null && (sameBefore || bookmark.getBookmarkName() == null)) {
						if (bookmark.setBookmarkName(StringUtils.nullOnEmpty(mucOptions.getName()))) {
							pushBookmarks(account);
						}
					}


					if (callback != null) {
						callback.onConferenceConfigurationFetched(conversation);
					}



					updateConversationUi();
				} else if (packet.getType() == IqPacket.TYPE.ERROR) {
					if (callback != null) {
						callback.onFetchFailed(conversation, packet.getError());
					}
				}
			}
		});
	}

	public void pushNodeConfiguration(Account account, final String node, final Bundle options, final OnConfigurationPushed callback) {
		pushNodeConfiguration(account, account.getJid().asBareJid(), node, options, callback);
	}

	public void pushNodeConfiguration(Account account, final Jid jid, final String node, final Bundle options, final OnConfigurationPushed callback) {
		sendIqPacket(account, mIqGenerator.requestPubsubConfiguration(jid, node), new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Element pubsub = packet.findChild("pubsub", "http://jabber.org/protocol/pubsub#owner");
					Element configuration = pubsub == null ? null : pubsub.findChild("configure");
					Element x = configuration == null ? null : configuration.findChild("x", Namespace.DATA);
					if (x != null) {
						Data data = Data.parse(x);
						data.submit(options);
						sendIqPacket(account, mIqGenerator.publishPubsubConfiguration(jid, node, data), new OnIqPacketReceived() {
							@Override
							public void onIqPacketReceived(Account account, IqPacket packet) {
								if (packet.getType() == IqPacket.TYPE.RESULT && callback != null) {
									Log.d(Config.LOGTAG, account.getLogJid()+": successfully changed node configuration for node "+node);
									callback.onPushSucceeded();
								} else if (packet.getType() == IqPacket.TYPE.ERROR && callback != null) {
									callback.onPushFailed();
								}
							}
						});
					} else if (callback != null) {
						callback.onPushFailed();
					}
				} else if (packet.getType() == IqPacket.TYPE.ERROR && callback != null) {
					callback.onPushFailed();
				}
			}
		});
	}

	public void pushConferenceConfiguration(final Conversation conversation, final Bundle options, final OnConfigurationPushed callback) {
		if (options.getString("muc#roomconfig_whois","moderators").equals("anyone")) {
			conversation.setAttribute("accept_non_anonymous",true);
			updateConversation(conversation);
		}
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.setTo(conversation.getJid().asBareJid());
		request.query("http://jabber.org/protocol/muc#owner");
		sendIqPacket(conversation.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Data data = Data.parse(packet.query().findChild("x", Namespace.DATA));
					data.submit(options);
					Log.d(Config.LOGTAG,data.toString());
					IqPacket set = new IqPacket(IqPacket.TYPE.SET);
					set.setTo(conversation.getJid().asBareJid());
					set.query("http://jabber.org/protocol/muc#owner").addChild(data);
					sendIqPacket(account, set, new OnIqPacketReceived() {
						@Override
						public void onIqPacketReceived(Account account, IqPacket packet) {
							if (callback != null) {
								if (packet.getType() == IqPacket.TYPE.RESULT) {
									callback.onPushSucceeded();
								} else {
									callback.onPushFailed();
								}
							}
						}
					});
				} else {
					if (callback != null) {
						callback.onPushFailed();
					}
				}
			}
		});
	}

	public void pushSubjectToConference(final Conversation conference, final String subject) {
		MessagePacket packet = this.getMessageGenerator().conferenceSubject(conference, StringUtils.nullOnEmpty(subject));
		this.sendMessagePacket(conference.getAccount(), packet);
	}

	public void changeAffiliationInConference(final Conversation conference, Jid user, final MucOptions.Affiliation affiliation, final OnAffiliationChanged callback) {
		final Jid jid = user.asBareJid();
		IqPacket request = this.mIqGenerator.changeAffiliation(conference, jid, affiliation.toString());
		sendIqPacket(conference.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					conference.getMucOptions().changeAffiliation(jid, affiliation);
					getAvatarService().clear(conference);
					callback.onAffiliationChangedSuccessful(jid);
				} else {
					callback.onAffiliationChangeFailed(jid, R.string.could_not_change_affiliation);
				}
			}
		});
	}

	public void changeAffiliationsInConference(final Conversation conference, MucOptions.Affiliation before, MucOptions.Affiliation after) {
		List<Jid> jids = new ArrayList<>();
		for (MucOptions.User user : conference.getMucOptions().getUsers()) {
			if (user.getAffiliation() == before && user.getRealJid() != null) {
				jids.add(user.getRealJid());
			}
		}
		IqPacket request = this.mIqGenerator.changeAffiliation(conference, jids, after.toString());
		sendIqPacket(conference.getAccount(), request, mDefaultIqHandler);
	}

	public void changeRoleInConference(final Conversation conference, final String nick, MucOptions.Role role) {
		IqPacket request = this.mIqGenerator.changeRole(conference, nick, role.toString());
		Log.d(Config.LOGTAG, request.toString());
		sendIqPacket(conference.getAccount(), request, (account, packet) -> {
			if (packet.getType() != IqPacket.TYPE.RESULT) {
				Log.d(Config.LOGTAG, account.getLogJid()+" unable to change role of "+nick);
			}
		});
	}

	public void destroyRoom(final Conversation conversation, final OnRoomDestroy callback) {
		IqPacket request = new IqPacket(IqPacket.TYPE.SET);
		request.setTo(conversation.getJid().asBareJid());
		request.query("http://jabber.org/protocol/muc#owner").addChild("destroy");
		sendIqPacket(conversation.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					if (callback != null) {
						callback.onRoomDestroySucceeded();
					}
				} else if (packet.getType() == IqPacket.TYPE.ERROR) {
					if (callback != null) {
						callback.onRoomDestroyFailed();
					}
				}
			}
		});
	}

	private void disconnect(Account account, boolean force) {
		if ((account.getStatus() == Account.State.ONLINE)
				|| (account.getStatus() == Account.State.DISABLED)) {
			final XmppConnection connection = account.getXmppConnection();
			if (!force) {
				List<Conversation> conversations = getConversations();
				for (Conversation conversation : conversations) {
					if (conversation.getAccount() == account) {
						if (conversation.getMode() == Conversation.MODE_MULTI) {
							leaveMuc(conversation, true);
						}
					}
				}
				//sendOfflinePresence(account);
			}
			//ALF AM-258 (and commented out above)
			sendOfflinePresence(account);
			connection.disconnect(false);
			//connection.disconnect(force);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void updateMessage(Message message) {
		updateMessage(message, true);
	}

	public void updateMessage(Message message, boolean includeBody) {
		databaseBackend.updateMessage(message, includeBody);
		updateConversationUi();
	}

	public void updateMessage(Message message, String uuid) {
		if (!databaseBackend.updateMessage(message, uuid)) {
			Log.e(Config.LOGTAG,"error updated message in DB after edit");
		}
		updateConversationUi();
	}

	protected void syncDirtyContacts(Account account) {
		for (Contact contact : account.getRoster().getContacts()) {
			if (contact.getOption(Contact.Options.DIRTY_PUSH)) {
				pushContactToServer(contact);
			}
			if (contact.getOption(Contact.Options.DIRTY_DELETE)) {
				deleteContactOnServer(contact);
			}
		}
	}

	public void createContact(Contact contact, boolean autoGrant) {
		if (autoGrant) {
			contact.setOption(Contact.Options.PREEMPTIVE_GRANT);
			contact.setOption(Contact.Options.ASKING);
		}
		pushContactToServer(contact);
	}

	public void pushContactToServer(final Contact contact) {
		contact.resetOption(Contact.Options.DIRTY_DELETE);
		contact.setOption(Contact.Options.DIRTY_PUSH);
		final Account account = contact.getAccount();
		if (account.getStatus() == Account.State.ONLINE) {
			final boolean ask = contact.getOption(Contact.Options.ASKING);
			final boolean sendUpdates = contact
					.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)
					&& contact.getOption(Contact.Options.PREEMPTIVE_GRANT);
			final IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
			iq.query(Namespace.ROSTER).addChild(contact.asElement());
			account.getXmppConnection().sendIqPacket(iq, mDefaultIqHandler);
			if (sendUpdates) {
				sendPresencePacket(account, mPresenceGenerator.sendPresenceUpdatesTo(contact));
			}
			if (ask) {
				sendPresencePacket(account, mPresenceGenerator.requestPresenceUpdatesFrom(contact));
			}
		} else {
			syncRoster(contact.getAccount());
		}
	}

	public void publishMucAvatar(final Conversation conversation, final Uri image, final OnAvatarPublication callback) {
		new Thread(() -> {
			final Bitmap.CompressFormat format = Config.AVATAR_FORMAT;
			final int size = Config.AVATAR_SIZE;
			final Avatar avatar = getFileBackend().getPepAvatar(image, size, format);
			if (avatar != null) {
				if (!getFileBackend().save(avatar)) {
					callback.onAvatarPublicationFailed(R.string.error_saving_avatar);
					return;
				}
				avatar.owner = conversation.getJid().asBareJid();
				publishMucAvatar(conversation, avatar, callback);
			} else {
				callback.onAvatarPublicationFailed(R.string.error_publish_avatar_converting);
			}
		}).start();
	}

	public void publishAvatar(final Account account, final Uri image, final OnAvatarPublication callback) {
		new Thread(() -> {
			final Bitmap.CompressFormat format = Config.AVATAR_FORMAT;
			final int size = Config.AVATAR_SIZE;
			final Avatar avatar = getFileBackend().getPepAvatar(image, size, format);
			if (avatar != null) {
				if (!getFileBackend().save(avatar)) {
					Log.d(Config.LOGTAG,"unable to save vcard");
					callback.onAvatarPublicationFailed(R.string.error_saving_avatar);
					return;
				}
				publishAvatar(account, avatar, callback);
			} else {
				callback.onAvatarPublicationFailed(R.string.error_publish_avatar_converting);
			}
		}).start();

	}

	private void publishMucAvatar(Conversation conversation, Avatar avatar, OnAvatarPublication callback) {
		final IqPacket retrieve = mIqGenerator.retrieveVcardAvatar(avatar);
		sendIqPacket(conversation.getAccount(), retrieve, (account, response) -> {
			boolean itemNotFound = response.getType() == IqPacket.TYPE.ERROR && response.hasChild("error") && response.findChild("error").hasChild("item-not-found");
			if (response.getType() == IqPacket.TYPE.RESULT || itemNotFound) {
				Element vcard = response.findChild("vCard", "vcard-temp");
				if (vcard == null) {
					vcard = new Element("vCard", "vcard-temp");
				}
				Element photo = vcard.findChild("PHOTO");
				if (photo == null) {
					photo = vcard.addChild("PHOTO");
				}
				photo.clearChildren();
				photo.addChild("TYPE").setContent(avatar.type);
				photo.addChild("BINVAL").setContent(avatar.image);
				IqPacket publication = new IqPacket(IqPacket.TYPE.SET);
				publication.setTo(conversation.getJid().asBareJid());
				publication.addChild(vcard);
				sendIqPacket(account, publication, (a1, publicationResponse) -> {
					if (publicationResponse.getType() == IqPacket.TYPE.RESULT) {
						callback.onAvatarPublicationSucceeded();
					} else {
						Log.d(Config.LOGTAG, "failed to publish vcard " + publicationResponse.getError());
						callback.onAvatarPublicationFailed(R.string.error_publish_avatar_server_reject);
					}
				});
			} else {
				Log.d(Config.LOGTAG, "failed to request vcard " + response.toString());
				callback.onAvatarPublicationFailed(R.string.error_publish_avatar_no_server_support);
			}
		});
	}

	public void publishAvatar(Account account, final Avatar avatar, final OnAvatarPublication callback) {
		final Bundle options;
		if (account.getXmppConnection().getFeatures().pepPublishOptions()) {
			options = PublishOptions.openAccess();
		} else {
			options = null;
		}
		publishAvatar(account, avatar, options, true, callback);
	}

	public void publishAvatar(Account account, final Avatar avatar, final Bundle options, final boolean retry, final OnAvatarPublication callback) {
		Log.d(Config.LOGTAG, account.getLogJid()+": publishing avatar. options="+options);
		IqPacket packet = this.mIqGenerator.publishAvatar(avatar, options);
		this.sendIqPacket(account, packet, new OnIqPacketReceived() {

			@Override
			public void onIqPacketReceived(Account account, IqPacket result) {
				if (result.getType() == IqPacket.TYPE.RESULT) {
					publishAvatarMetadata(account, avatar, options,true, callback);
				} else if (retry && PublishOptions.preconditionNotMet(result)) {
					pushNodeConfiguration(account, "urn:xmpp:avatar:data", options, new OnConfigurationPushed() {
						@Override
						public void onPushSucceeded() {
							Log.d(Config.LOGTAG, account.getLogJid()+": changed node configuration for avatar node");
							publishAvatar(account, avatar, options, false, callback);
						}

						@Override
						public void onPushFailed() {
							Log.d(Config.LOGTAG, account.getLogJid()+": unable to change node configuration for avatar node");
							publishAvatar(account, avatar, null, false, callback);
						}
					});
				} else {
					Element error = result.findChild("error");
					Log.d(Config.LOGTAG, account.getLogJid() + ": server rejected avatar " + (avatar.size / 1024) + "KiB " + (error != null ? error.toString() : ""));
					if (callback != null) {
						callback.onAvatarPublicationFailed(R.string.error_publish_avatar_server_reject);
					}
				}
			}
		});
	}

	public void publishAvatarMetadata(Account account, final Avatar avatar, final Bundle options, final boolean retry, final OnAvatarPublication callback) {
		final IqPacket packet = XmppConnectionService.this.mIqGenerator.publishAvatarMetadata(avatar, options);
		sendIqPacket(account, packet, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket result) {
				if (result.getType() == IqPacket.TYPE.RESULT) {
					if (account.setAvatar(avatar.getFilename())) {
						getAvatarService().clear(account);
						databaseBackend.updateAccount(account);
						notifyAccountAvatarHasChanged(account);
					}
					Log.d(Config.LOGTAG, account.getLogJid() + ": published avatar " + (avatar.size / 1024) + "KiB");
					if (callback != null) {
						callback.onAvatarPublicationSucceeded();
					}
				} else if (retry && PublishOptions.preconditionNotMet(result)) {
					pushNodeConfiguration(account, "urn:xmpp:avatar:metadata", options, new OnConfigurationPushed() {
						@Override
						public void onPushSucceeded() {
							Log.d(Config.LOGTAG, account.getLogJid()+": changed node configuration for avatar meta data node");
							publishAvatarMetadata(account, avatar, options,false, callback);
						}

						@Override
						public void onPushFailed() {
							Log.d(Config.LOGTAG, account.getLogJid()+": unable to change node configuration for avatar meta data node");
							publishAvatarMetadata(account, avatar,  null,false, callback);
						}
					});
				} else {
					if (callback != null) {
						callback.onAvatarPublicationFailed(R.string.error_publish_avatar_server_reject);
					}
				}
			}
		});
	}

	public void republishAvatarIfNeeded(Account account) {
		if (account.getAxolotlService().isPepBroken()) {
			Log.d(Config.LOGTAG, account.getLogJid() + ": skipping republication of avatar because pep is broken");
			return;
		}
		IqPacket packet = this.mIqGenerator.retrieveAvatarMetaData(null);
		this.sendIqPacket(account, packet, new OnIqPacketReceived() {

			private Avatar parseAvatar(IqPacket packet) {
				Element pubsub = packet.findChild("pubsub", "http://jabber.org/protocol/pubsub");
				if (pubsub != null) {
					Element items = pubsub.findChild("items");
					if (items != null) {
						return Avatar.parseMetadata(items);
					}
				}
				return null;
			}

			private boolean errorIsItemNotFound(IqPacket packet) {
				Element error = packet.findChild("error");
				return packet.getType() == IqPacket.TYPE.ERROR
						&& error != null
						&& error.hasChild("item-not-found");
			}

			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT || errorIsItemNotFound(packet)) {
					Avatar serverAvatar = parseAvatar(packet);
					if (serverAvatar == null && account.getAvatar() != null) {
						Avatar avatar = fileBackend.getStoredPepAvatar(account.getAvatar());
						if (avatar != null) {
							Log.d(Config.LOGTAG, account.getLogJid() + ": avatar on server was null. republishing");
							publishAvatar(account, fileBackend.getStoredPepAvatar(account.getAvatar()), null);
						} else {
							Log.e(Config.LOGTAG, account.getLogJid() + ": error rereading avatar");
						}
					}
				}
			}
		});
	}

	public void fetchAvatar(Account account, Avatar avatar) {
		fetchAvatar(account, avatar, null);
	}

	public void fetchAvatar(Account account, final Avatar avatar, final UiCallback<Avatar> callback) {
		final String KEY = generateFetchKey(account, avatar);
		synchronized (this.mInProgressAvatarFetches) {
			if (mInProgressAvatarFetches.add(KEY)) {
				switch (avatar.origin) {
					case PEP:
						this.mInProgressAvatarFetches.add(KEY);
						fetchAvatarPep(account, avatar, callback);
						break;
					case VCARD:
						this.mInProgressAvatarFetches.add(KEY);
						fetchAvatarVcard(account, avatar, callback);
						break;
				}
			} else if (avatar.origin == Avatar.Origin.PEP) {
				mOmittedPepAvatarFetches.add(KEY);
			} else {
				Log.d(Config.LOGTAG, account.getLogJid()+": already fetching "+avatar.origin+" avatar for "+avatar.owner);
			}
		}
	}

	private void fetchAvatarPep(Account account, final Avatar avatar, final UiCallback<Avatar> callback) {
		IqPacket packet = this.mIqGenerator.retrievePepAvatar(avatar);
		sendIqPacket(account, packet, (a, result) -> {
			synchronized (mInProgressAvatarFetches) {
				mInProgressAvatarFetches.remove(generateFetchKey(a, avatar));
			}
			final String ERROR = a.getJid().asBareJid() + ": fetching avatar for " + avatar.owner + " failed ";
			if (result.getType() == IqPacket.TYPE.RESULT) {
				avatar.image = mIqParser.avatarData(result);
				if (avatar.image != null) {
					if (getFileBackend().save(avatar)) {
						if (a.getJid().asBareJid().equals(avatar.owner)) {
							if (a.setAvatar(avatar.getFilename())) {
								databaseBackend.updateAccount(a);
							}
							getAvatarService().clear(a);
							updateConversationUi();
							updateAccountUi();
						} else {
							Contact contact = a.getRoster().getContact(avatar.owner);
							if (contact.setAvatar(avatar)) {
								syncRoster(account);
								getAvatarService().clear(contact);
								updateConversationUi();
								updateRosterUi();
							}
						}
						if (callback != null) {
							callback.success(avatar);
						}
						Log.d(Config.LOGTAG, a.getJid().asBareJid()
								+ ": successfully fetched pep avatar for " + avatar.owner);
						return;
					}
				} else {

					Log.d(Config.LOGTAG, ERROR + "(parsing error)");
				}
			} else {
				Element error = result.findChild("error");
				if (error == null) {
					Log.d(Config.LOGTAG, ERROR + "(server error)");
				} else {
					Log.d(Config.LOGTAG, ERROR + error.toString());
				}
			}
			if (callback != null) {
				callback.error(0, null);
			}

		});
	}

	private void fetchAvatarVcard(final Account account, final Avatar avatar, final UiCallback<Avatar> callback) {
		IqPacket packet = this.mIqGenerator.retrieveVcardAvatar(avatar);
		this.sendIqPacket(account, packet, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				final boolean previouslyOmittedPepFetch;
				synchronized (mInProgressAvatarFetches) {
					final String KEY = generateFetchKey(account, avatar);
					mInProgressAvatarFetches.remove(KEY);
					previouslyOmittedPepFetch = mOmittedPepAvatarFetches.remove(KEY);
				}
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Element vCard = packet.findChild("vCard", "vcard-temp");
					Element photo = vCard != null ? vCard.findChild("PHOTO") : null;
					String image = photo != null ? photo.findChildContent("BINVAL") : null;
					if (image != null) {
						avatar.image = image;
						if (getFileBackend().save(avatar)) {
							Log.d(Config.LOGTAG, account.getLogJid()
									+ ": successfully fetched vCard avatar for " + avatar.owner+" omittedPep="+previouslyOmittedPepFetch);
							if (avatar.owner.isBareJid()) {
								if (account.getJid().asBareJid().equals(avatar.owner) && account.getAvatar() == null) {
									Log.d(Config.LOGTAG, account.getLogJid() + ": had no avatar. replacing with vcard");
									account.setAvatar(avatar.getFilename());
									databaseBackend.updateAccount(account);
									getAvatarService().clear(account);
									updateAccountUi();
								} else {
									Contact contact = account.getRoster().getContact(avatar.owner);
									if (contact.setAvatar(avatar, previouslyOmittedPepFetch)) {
										syncRoster(account);
										getAvatarService().clear(contact);
										updateRosterUi();
									}
								}
								updateConversationUi();
							} else {
								Conversation conversation = find(account, avatar.owner.asBareJid());
								if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI) {
									MucOptions.User user = conversation.getMucOptions().findUserByFullJid(avatar.owner);
									if (user != null) {
										if (user.setAvatar(avatar)) {
											getAvatarService().clear(user);
											updateConversationUi();
											updateMucRosterUi();
										}
										if (user.getRealJid() != null) {
											Contact contact = account.getRoster().getContact(user.getRealJid());
											if (contact.setAvatar(avatar)) {
												syncRoster(account);
												getAvatarService().clear(contact);
												updateRosterUi();
											}
										}
									}
								}
							}
						}
					}
					//AM-642
					Element displayel = vCard != null ? vCard.findChild("NICKNAME") : null;
					if (displayel != null) {
						String displayname = displayel.getContent();
						Jid avatarJid = avatar.owner;
						if (account.getDisplayName() == null && packet.getFrom() != null && account.getJid().asBareJid().equals(packet.getFrom().asBareJid())) {
							account.setDisplayName(displayname);
							setRoomsNickname(displayname, false, null);
							databaseBackend.updateAccount(account);
							updateConversationUi();
							updateAccountUi();
						} else if (displayname != null && avatarJid != null && account.getJid().asBareJid().equals(avatarJid.asBareJid()) && !account.getDisplayName().equals(displayname)) {
							account.setDisplayName(displayname);
							setRoomsNickname(displayname, false, null);
							databaseBackend.updateAccount(account);
							updateConversationUi();
							updateAccountUi();
						} else if (displayname != null && avatarJid != null && !account.getJid().asBareJid().equals(avatarJid.asBareJid())) {
							if (avatarJid.isBareJid()) {
								Contact contact = account.getRoster().getContact(avatarJid);
								if (contact.getDisplayName() == null || !contact.getDisplayName().equals(displayname)) {
									syncRoster(account);
									updateRosterUi();
								}
							} else {
								Conversation conversation = find(account, avatarJid.asBareJid());
								if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI) {
									MucOptions.User user = conversation.getMucOptions().findUserByFullJid(avatar.owner);
									if (user != null && user.getRealJid() != null) {
										Contact contact = account.getRoster().getContact(user.getRealJid());
										if (contact.getDisplayName() == null || !contact.getDisplayName().equals(displayname)) {
											syncRoster(account);
											updateRosterUi();
										}
									}
								}
							}
						}
					}
				}
			}
		});
	}

	//AM-642
	public void getVCardForName(Account account) {
		IqPacket packet = this.mIqGenerator.retrieveVcardAccount(account);
		this.sendIqPacket(account, packet, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Element vCard = packet.findChild("vCard", "vcard-temp");
					Element displayel = vCard != null ? vCard.findChild("NICKNAME") : null;
					if (displayel != null && packet.getFrom() != null && account.getJid().asBareJid().equals(packet.getFrom().asBareJid())) {
						String displayname = displayel.getContent();
						if (displayname != null &&
								(account.getDisplayName() == null || !account.getDisplayName().equals(displayname))) {
							account.setDisplayName(displayname);
							setRoomsNickname(displayname, false, null);
							databaseBackend.updateAccount(account);
							updateConversationUi();
							updateAccountUi();
						}
					}
				}
			}
		});
	}

	public void checkForAvatar(Account account, final UiCallback<Avatar> callback) {
		IqPacket packet = this.mIqGenerator.retrieveAvatarMetaData(null);
		this.sendIqPacket(account, packet, new OnIqPacketReceived() {

			@Override
			public void onIqPacketReceived(Account account, IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					Element pubsub = packet.findChild("pubsub", "http://jabber.org/protocol/pubsub");
					if (pubsub != null) {
						Element items = pubsub.findChild("items");
						if (items != null) {
							Avatar avatar = Avatar.parseMetadata(items);
							if (avatar != null) {
								avatar.owner = account.getJid().asBareJid();
								if (fileBackend.isAvatarCached(avatar)) {
									if (account.setAvatar(avatar.getFilename())) {
										databaseBackend.updateAccount(account);
									}
									getAvatarService().clear(account);
									callback.success(avatar);
								} else {
									fetchAvatarPep(account, avatar, callback);
								}
								return;
							}
						}
					}
				}
				callback.error(0, null);
			}
		});
	}

	public void notifyAccountAvatarHasChanged(final Account account) {
		final XmppConnection connection = account.getXmppConnection();
		if (connection != null && connection.getFeatures().bookmarksConversion()) {
			Log.d(Config.LOGTAG, account.getLogJid()+": avatar changed. resending presence to online group chats");
			for(Conversation conversation : conversations) {
				if (conversation.getAccount() == account && conversation.getMode() == Conversational.MODE_MULTI) {
					final MucOptions mucOptions = conversation.getMucOptions();
					if (mucOptions.online()) {
						PresencePacket packet = mPresenceGenerator.selfPresence(account, Presence.Status.ONLINE, mucOptions.nonanonymous());
						packet.setTo(mucOptions.getSelf().getFullJid());
						connection.sendPresencePacket(packet);
					}
				}
			}
		}
	}

	public void deleteContactOnServer(Contact contact) {
		contact.resetOption(Contact.Options.PREEMPTIVE_GRANT);
		contact.resetOption(Contact.Options.DIRTY_PUSH);
		contact.setOption(Contact.Options.DIRTY_DELETE);
		Account account = contact.getAccount();
		if (account.getStatus() == Account.State.ONLINE) {
			IqPacket iq = new IqPacket(IqPacket.TYPE.SET);
			Element item = iq.query(Namespace.ROSTER).addChild("item");
			item.setAttribute("jid", contact.getJid().toString());
			item.setAttribute("subscription", "remove");
			account.getXmppConnection().sendIqPacket(iq, mDefaultIqHandler);
		}
	}

	public void updateConversation(final Conversation conversation) {
		mDatabaseWriterExecutor.execute(() -> databaseBackend.updateConversation(conversation));
	}

	private void reconnectAccount(final Account account, final boolean force, final boolean interactive) {
		synchronized (account) {
			XmppConnection connection = account.getXmppConnection();
			if (connection == null) {
				connection = createConnection(account);
				account.setXmppConnection(connection);
			}
			boolean hasInternet = hasInternetConnection();
			if (account.isEnabled() && hasInternet) {
				if (!force) {
					disconnect(account, false);
				}
				Thread thread = new Thread(connection);
				connection.setInteractive(interactive);
				connection.prepareNewConnection();
				connection.interrupt();
				thread.start();
				scheduleWakeUpCall(Config.CONNECT_DISCO_TIMEOUT, account.getUuid().hashCode());
			} else {
				disconnect(account, force || account.getTrueStatus().isError() || !hasInternet);
				account.getRoster().clearPresences();
				connection.resetEverything();
				final AxolotlService axolotlService = account.getAxolotlService();
				if (axolotlService != null) {
					axolotlService.resetBrokenness();
				}
				if (!hasInternet) {
					account.setStatus(Account.State.NO_INTERNET);
				}
			}
		}
	}

	public void reconnectAccountInBackground(final Account account) {
		new Thread(() -> reconnectAccount(account, false, true)).start();
	}

	public void invite(Conversation conversation, Jid contact) {
		Log.d(Config.LOGTAG, conversation.getAccount().getLogJid() + ": inviting " + Tools.logJid(contact) + " to " + conversation.getLogJid());
		MessagePacket packet = mMessageGenerator.invite(conversation, contact);
		sendMessagePacket(conversation.getAccount(), packet);
	}

	public void directInvite(Conversation conversation, Jid jid) {
		MessagePacket packet = mMessageGenerator.directInvite(conversation, jid);
		sendMessagePacket(conversation.getAccount(), packet);
	}

	public void resetSendingToWaiting(Account account) {
		for (Conversation conversation : getConversations()) {
			if (conversation.getAccount() == account) {
				conversation.findUnsentTextMessages(message -> markMessage(message, Message.STATUS_WAITING));
			}
		}
	}

	public Message markMessage(final Account account, final Jid recipient, final String uuid, final int status) {
		return markMessage(account, recipient, uuid, status, null);
	}

	public Message markMessage(final Account account, final Jid recipient, final String uuid, final int status, String errorMessage) {
		if (uuid == null) {
			return null;
		}
		for (Conversation conversation : getConversations()) {
			if (conversation.getJid().asBareJid().equals(recipient) && conversation.getAccount() == account) {
				final Message message = conversation.findSentMessageWithUuidOrRemoteId(uuid);
				if (message != null) {
					markMessage(message, status, errorMessage);
				}
				return message;
			}
		}
		return null;
	}

	public boolean markMessage(Conversation conversation, String uuid, int status, String serverMessageId) {
		if (uuid == null) {
			return false;
		} else {
			Message message = conversation.findSentMessageWithUuid(uuid);
			if (message != null) {
				if (message.getServerMsgId() == null) {
					message.setServerMsgId(serverMessageId);
				}
				markMessage(message, status);
				return true;
			} else {
				return false;
			}
		}
	}

	public void markMessage(Message message, int status) {
		markMessage(message, status, null);
	}


	public void markMessage(Message message, int status, String errorMessage) {
		final int c = message.getStatus();
		if (status == Message.STATUS_SEND_FAILED && (c == Message.STATUS_SEND_RECEIVED || c == Message.STATUS_SEND_DISPLAYED)) {
			return;
		}
		if (status == Message.STATUS_SEND_RECEIVED && c == Message.STATUS_SEND_DISPLAYED) {
			return;
		}
		message.setErrorMessage(errorMessage);
		message.setStatus(status);
		databaseBackend.updateMessage(message, false);
		updateConversationUi();
	}

	//ALF AM-75
	public void handleFailedEncryption(Message message, boolean delay) {
		// recall session stuff if possible?
		message.setEncryption(Message.ENCRYPTION_NONE);
		this.resendMessage(message, delay);

		Conversation conv = (Conversation)message.getConversation();
		if (conv != null && conv.getMode() == Conversation.MODE_MULTI && conv.getMucOptions().membersOnly())
		{
			AxolotlService axolotlService = conv.getAccount().getAxolotlService();
			axolotlService.createSessionsIfNeeded(conv);
			conv.reloadFingerprints(axolotlService.getCryptoTargets(conv));
			conv.commitTrusts();
			conv.setNextEncryption(Message.ENCRYPTION_AXOLOTL);
			updateConversation(conv);
		}
	}

	public SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	}

	public long getAutomaticMessageDeletionDate() {
		final long timeout = getLongPreference(SettingsActivity.AUTOMATIC_MESSAGE_DELETION, R.integer.automatic_message_deletion);
		return timeout == 0 ? timeout : (System.currentTimeMillis() - (timeout * 1000));
	}

	public long getLongPreference(String name, @IntegerRes int res) {
		long defaultValue = getResources().getInteger(res);
		try {
			return Long.parseLong(getPreferences().getString(name, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public boolean getBooleanPreference(String name, @BoolRes int res) {
		return getPreferences().getBoolean(name, getResources().getBoolean(res));
	}

	public boolean confirmMessages() {
		return getBooleanPreference("confirm_messages", R.bool.confirm_messages);
	}

	public boolean allowMessageCorrection() {
		return getBooleanPreference("allow_message_correction", R.bool.allow_message_correction);
	}

	public boolean sendChatStates() {
		return getBooleanPreference("chat_states", R.bool.chat_states);
	}

	private boolean synchronizeWithBookmarks() {
		return getBooleanPreference("autojoin", R.bool.autojoin);
	}

	public boolean indicateReceived() {
		return getBooleanPreference("indicate_received", R.bool.indicate_received);
	}

	public boolean useTorToConnect() {
		return QuickConversationsService.isConversations() && getBooleanPreference("use_tor", R.bool.use_tor);
	}

	public boolean showExtendedConnectionOptions() {
		return QuickConversationsService.isConversations() && getBooleanPreference("show_connection_options", R.bool.show_connection_options);
	}

	public boolean broadcastLastActivity() {
		return getBooleanPreference(SettingsActivity.BROADCAST_LAST_ACTIVITY, R.bool.last_activity);
	}

	public int unreadCount() {
		int count = 0;
		for (Conversation conversation : getConversations()) {
			count += conversation.unreadCount();
		}
		return count;
	}


	private <T> List<T> threadSafeList(Set<T> set) {
		synchronized (LISTENER_LOCK) {
			return set.size() == 0 ? Collections.emptyList() : new ArrayList<>(set);
		}
	}

	public void showErrorToastInUi(int resId) {
		for (OnShowErrorToast listener : threadSafeList(this.mOnShowErrorToasts)) {
			listener.onShowErrorToast(resId);
		}
	}

	public void updateConversationUi() {
		for (OnConversationUpdate listener : threadSafeList(this.mOnConversationUpdates)) {
			listener.onConversationUpdate();
		}
	}

	public void updateAccountUi() {
		for (OnAccountUpdate listener : threadSafeList(this.mOnAccountUpdates)) {
			listener.onAccountUpdate();
		}
	}

	public void updateRosterUi() {
		for (OnRosterUpdate listener : threadSafeList(this.mOnRosterUpdates)) {
			listener.onRosterUpdate();
		}
	}

	public boolean displayCaptchaRequest(Account account, String id, Data data, Bitmap captcha) {
		if (mOnCaptchaRequested.size() > 0) {
			DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
			Bitmap scaled = Bitmap.createScaledBitmap(captcha, (int) (captcha.getWidth() * metrics.scaledDensity),
					(int) (captcha.getHeight() * metrics.scaledDensity), false);
			for (OnCaptchaRequested listener : threadSafeList(this.mOnCaptchaRequested)) {
				listener.onCaptchaRequested(account, id, data, scaled);
			}
			return true;
		}
		return false;
	}

	public void updateBlocklistUi(final OnUpdateBlocklist.Status status) {
		for (OnUpdateBlocklist listener : threadSafeList(this.mOnUpdateBlocklist)) {
			listener.OnUpdateBlocklist(status);
		}
	}

	public void updateMucRosterUi() {
		for (OnMucRosterUpdate listener : threadSafeList(this.mOnMucRosterUpdate)) {
			listener.onMucRosterUpdate();
		}
	}

	public void keyStatusUpdated(AxolotlService.FetchStatus report) {
		for (OnKeyStatusUpdated listener : threadSafeList(this.mOnKeyStatusUpdated)) {
			listener.onKeyStatusUpdated(report);
		}
	}

	//AM#14
	public void updateProcessLifecycle() {
		if (this.mOnProcessLifecycleUpdates.size() == 0) {
			needsProcessLifecycleUpdate = true;
			return;
		}
		needsProcessLifecycleUpdate = false;

		//get time stored in shared preferences
		String locktimeStr = getPreferences().getString("automatic_locktime", null);
		long locktime = BIOAUTH_INTERVAL_DEFAULT;
		if (locktimeStr != null && !locktimeStr.equalsIgnoreCase("immediate"))
		{
			try {
				locktime = Long.parseLong(locktimeStr);
				locktime = locktime * 1000L;
			} catch(NumberFormatException nfe) {
				locktime = BIOAUTH_INTERVAL_DEFAULT;
			}
		}

		if (lastBioauthFailed || SystemClock.elapsedRealtime() - mLastGlacierUsage.get() >= locktime) {
			for (OnProcessLifecycleUpdate listener : threadSafeList(this.mOnProcessLifecycleUpdates)) {
				listener.onProcessLifecycleUpdate();
			}
		}
	}

	//AM#52, AM#53 (next 2)
	public void updateSecurityInfo() {
		if (accounts == null || accounts.size() == 0) {
			needsSecurityInfoUpdate = true;
			return;
		}
		needsSecurityInfoUpdate = false;

		long lastupdate = mLastSecInfoUpdate.get();
		if (lastupdate == 0L || SystemClock.elapsedRealtime() - lastupdate >= (SECHUB_INTERVAL*1000)) {
			mLastSecInfoUpdate.set(SystemClock.elapsedRealtime());
			getSecurityInfo().checkCurrentSecurityInfo();
		}
	}

	public SystemSecurityInfo getSecurityInfo() {
		if (secInfo == null) {
			secInfo = new SystemSecurityInfo(this);
		}
		return secInfo;
	}

	public SMSdbInfo getSmsInfo() {
		if (smsInfo == null){
			smsInfo = new SMSdbInfo(this);
		}
		return smsInfo;
	}

	public void updateSmsInfo(){
		if (accounts != null) {
			try {
				getSmsInfo().trySmsInfoUpload();
			}
			catch (Exception e){

			}
		}
	}

	public void setSmsInfo(SMSdbInfo smsInfo){
		this.smsInfo = smsInfo;
	}


	public Account findAccountByJid(final Jid accountJid) {
		for (Account account : this.accounts) {
			if (account.getJid().asBareJid().equals(accountJid.asBareJid())) {
				return account;
			}
		}
		return null;
	}

	public Account findAccountByUuid(final String uuid) {
		for(Account account : this.accounts) {
			if (account.getUuid().equals(uuid)) {
				return account;
			}
		}
		return null;
	}

	public Conversation findConversationByUuid(String uuid) {
		for (Conversation conversation : getConversations()) {
			if (conversation.getUuid().equals(uuid)) {
				return conversation;
			}
		}
		return null;
	}

	public Conversation findUniqueConversationByJid(XmppUri xmppUri) {
		List<Conversation> findings = new ArrayList<>();
		for (Conversation c : getConversations()) {
			if (c.getAccount().isEnabled() && c.getJid().asBareJid().equals(xmppUri.getJid()) && ((c.getMode() == Conversational.MODE_MULTI) == xmppUri.isAction(XmppUri.ACTION_JOIN))) {
				findings.add(c);
			}
		}
		return findings.size() == 1 ? findings.get(0) : null;
	}

	public boolean markRead(final Conversation conversation, boolean dismiss) {
		return markRead(conversation, null, dismiss).size() > 0;
	}

	public void markRead(final Conversation conversation) {
		markRead(conversation, null, true);
	}

	public List<Message> markRead(final Conversation conversation, String upToUuid, boolean dismiss) {
		if (dismiss) {
			mNotificationService.clear(conversation);
		}
		final List<Message> readMessages = conversation.markRead(upToUuid);
		if (readMessages.size() > 0) {
			Runnable runnable = () -> {
				for (Message message : readMessages) {
					databaseBackend.updateMessage(message, false);
				}
			};
			mDatabaseWriterExecutor.execute(runnable);
			updateUnreadCountBadge();
			return readMessages;
		} else {
			return readMessages;
		}
	}

	public synchronized void updateUnreadCountBadge() {
		int count = unreadCount();
		if (unreadCount != count) {
			Log.d(Config.LOGTAG, "update unread count to " + count);
			if (count > 0) {
				ShortcutBadger.applyCount(getApplicationContext(), count);
			} else {
				ShortcutBadger.removeCount(getApplicationContext());
			}
			unreadCount = count;
		}
	}

	public void sendReadMarker(final Conversation conversation, String upToUuid) {
		final boolean isPrivateAndNonAnonymousMuc = conversation.getMode() == Conversation.MODE_MULTI && conversation.isPrivateAndNonAnonymous();
		final List<Message> readMessages = this.markRead(conversation, upToUuid, true);
		if (readMessages.size() > 0) {
			updateConversationUi();
		}
		final Message markable = Conversation.getLatestMarkableMessage(readMessages, isPrivateAndNonAnonymousMuc);
		if (confirmMessages()
				&& markable != null
				&& (markable.trusted() || isPrivateAndNonAnonymousMuc)
				&& markable.getRemoteMsgId() != null) {
//			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid()+ ": sending read marker to " + markable.getCounterpart().toString());
			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid()+ ": sending read marker to " + Tools.logJid(markable.getCounterpart().toString()));
			Account account = conversation.getAccount();
			final Jid to = markable.getCounterpart();
			final boolean groupChat = conversation.getMode() == Conversation.MODE_MULTI;
			MessagePacket packet = mMessageGenerator.confirm(account, to, markable.getRemoteMsgId(), markable.getCounterpart(), groupChat);
			this.sendMessagePacket(conversation.getAccount(), packet);
		}
	}

	public SecureRandom getRNG() {
		return this.mRandom;
	}

	public MemorizingTrustManager getMemorizingTrustManager() {
		return this.mMemorizingTrustManager;
	}

	public void setMemorizingTrustManager(MemorizingTrustManager trustManager) {
		this.mMemorizingTrustManager = trustManager;
	}

	public void updateMemorizingTrustmanager() {
		final MemorizingTrustManager tm;
		final boolean dontTrustSystemCAs = getBooleanPreference("dont_trust_system_cas", R.bool.dont_trust_system_cas);
		if (dontTrustSystemCAs) {
			tm = new MemorizingTrustManager(getApplicationContext(), null);
		} else {
			tm = new MemorizingTrustManager(getApplicationContext());
		}
		setMemorizingTrustManager(tm);
	}

	public LruCache<String, Bitmap> getBitmapCache() {
		return this.mBitmapCache;
	}

	public Collection<String> getKnownHosts() {
		final Set<String> hosts = new HashSet<>();
		for (final Account account : getAccounts()) {
			hosts.add(account.getServer());
			for (final Contact contact : account.getRoster().getContacts()) {
				if (contact.showInRoster()) {
					final String server = contact.getServer();
					if (server != null) {
						hosts.add(server);
					}
				}
			}
		}
		if (Config.QUICKSY_DOMAIN != null) {
			hosts.remove(Config.QUICKSY_DOMAIN); //we only want to show this when we type a e164 number
		}
		if (Config.DOMAIN_LOCK != null) {
			hosts.add(Config.DOMAIN_LOCK);
		}
		if (Config.MAGIC_CREATE_DOMAIN != null) {
			hosts.add(Config.MAGIC_CREATE_DOMAIN);
		}
		return hosts;
	}

	public Collection<String> getKnownConferenceHosts() {
		final Set<String> mucServers = new HashSet<>();
		for (final Account account : accounts) {
			if (account.getXmppConnection() != null) {
				mucServers.addAll(account.getXmppConnection().getMucServers());
				for (Bookmark bookmark : account.getBookmarks()) {
					final Jid jid = bookmark.getJid();
					final String s = jid == null ? null : jid.getDomain().toEscapedString();
					if (s != null) {
						mucServers.add(s);
					}
				}
			}
		}
		return mucServers;
	}

	public void sendMessagePacket(Account account, MessagePacket packet) {
		XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.sendMessagePacket(packet);
		}
	}

	public void sendPresencePacket(Account account, PresencePacket packet) {
		XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.sendPresencePacket(packet);
		}
	}

	public void sendCreateAccountWithCaptchaPacket(Account account, String id, Data data) {
		final XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			IqPacket request = mIqGenerator.generateCreateAccountWithCaptcha(account, id, data);
			connection.sendUnmodifiedIqPacket(request, connection.registrationResponseListener, true);
		}
	}

	public void sendIqPacket(final Account account, final IqPacket packet, final OnIqPacketReceived callback) {
		final XmppConnection connection = account.getXmppConnection();
		if (connection != null) {
			connection.sendIqPacket(packet, callback);
		} else if (callback != null) {
			callback.onIqPacketReceived(account,new IqPacket(IqPacket.TYPE.TIMEOUT));
		}
	}

	public void sendPresence(final Account account) {
		sendPresence(account, checkListeners() && broadcastLastActivity());
	}

	private void sendPresence(final Account account, final boolean includeIdleTimestamp) {
		Presence.Status status;
		boolean getVcard = false; //AM-642 and usage below
		/*if (manuallyChangePresence()) {
			status = account.getPresenceStatus();
		} else {
			status = getTargetPresence();
		}*/
		// DJF Updated for Advanced Settings 08-27-19
		if (dndOnSilentMode() && isPhoneSilenced()) {
			status =  Presence.Status.DND;
		} else if (awayWhenScreenOff() && !isInteractive()) {
			status =  Presence.Status.AWAY;
		} else if (manuallyChangePresence()) {
			status = account.getPresenceStatus();
			if (status == Presence.Status.ONLINE && lastStatus != Presence.Status.ONLINE && account.getStatus() == Account.State.ONLINE) { //AM-642
				getVcard = true;
			}
		} else {
			status =  Presence.Status.ONLINE;
			if (lastStatus != Presence.Status.ONLINE && account.getStatus() == Account.State.ONLINE) { //AM-642
				getVcard = true;
			}
		}
		lastStatus = status;

		PresencePacket packet = mPresenceGenerator.selfPresence(account, status);
		String message = account.getPresenceStatusMessage();
		if (message != null && !message.isEmpty()) {
			packet.addChild(new Element("status").setContent(message));
		}
		if (mLastActivity > 0 && includeIdleTimestamp) {
			long since = Math.min(mLastActivity, System.currentTimeMillis()); //don't send future dates
			packet.addChild("idle", Namespace.IDLE).setAttribute("since", AbstractGenerator.getTimestamp(since));
		}
		sendPresencePacket(account, packet);

		if (getVcard) { //AM-642
			getVCardForName(account);
		}
	}

	private void deactivateGracePeriod() {
		for (Account account : getAccounts()) {
			account.deactivateGracePeriod();
		}
	}

	public void refreshAllPresences() {
		boolean includeIdleTimestamp = checkListeners() && broadcastLastActivity();
		for (Account account : getAccounts()) {
			if (account.isEnabled()) {
				sendPresence(account, includeIdleTimestamp);
			}
		}
	}

	private void refreshAllFcmTokens() {
		for (Account account : getAccounts()) {
			if (account.isOnlineAndConnected() && mPushManagementService.available(account)) {
				mPushManagementService.registerPushTokenOnServer(account);
			}
		}
	}

	private void sendOfflinePresence(final Account account) {
		Log.d(Config.LOGTAG, account.getLogJid() + ": sending offline presence");
		sendPresencePacket(account, mPresenceGenerator.sendOfflinePresence(account));
	}

	public MessageGenerator getMessageGenerator() {
		return this.mMessageGenerator;
	}

	public PresenceGenerator getPresenceGenerator() {
		return this.mPresenceGenerator;
	}

	public IqGenerator getIqGenerator() {
		return this.mIqGenerator;
	}

	public IqParser getIqParser() {
		return this.mIqParser;
	}

	public JingleConnectionManager getJingleConnectionManager() {
		return this.mJingleConnectionManager;
	}

	public MessageArchiveService getMessageArchiveService() {
		return this.mMessageArchiveService;
	}

	public QuickConversationsService getQuickConversationsService() {
		return this.mQuickConversationsService;
	}

	public List<Contact> findContacts(Jid jid, String accountJid) {
		ArrayList<Contact> contacts = new ArrayList<>();
		for (Account account : getAccounts()) {
			if ((account.isEnabled() || accountJid != null)
					&& (accountJid == null || accountJid.equals(account.getJid().asBareJid().toString()))) {
				Contact contact = account.getRoster().getContactFromContactList(jid);
				if (contact != null) {
					contacts.add(contact);
				}
			}
		}
		return contacts;
	}

	public Conversation findFirstMuc(Jid jid) {
		for (Conversation conversation : getConversations()) {
			if (conversation.getAccount().isEnabled() && conversation.getJid().asBareJid().equals(jid.asBareJid()) && conversation.getMode() == Conversation.MODE_MULTI) {
				return conversation;
			}
		}
		return null;
	}

	public NotificationService getNotificationService() {
		return this.mNotificationService;
	}

	public HttpConnectionManager getHttpConnectionManager() {
		return this.mHttpConnectionManager;
	}

	public void resendFailedMessages(final Message message) {
		final Collection<Message> messages = new ArrayList<>();
		Message current = message;
		while (current.getStatus() == Message.STATUS_SEND_FAILED) {
			messages.add(current);
			if (current.mergeable(current.next())) {
				current = current.next();
			} else {
				break;
			}
		}
		for (final Message msg : messages) {
			msg.setTime(System.currentTimeMillis());
			markMessage(msg, Message.STATUS_WAITING);
			this.resendMessage(msg, false);
		}
		if (message.getConversation() instanceof Conversation) {
			((Conversation) message.getConversation()).sort();
		}
		updateConversationUi();
	}

	public void clearConversationHistory(final Conversation conversation) {
		final long clearDate;
		final String reference;
		if (conversation.countMessages() > 0) {
			Message latestMessage = conversation.getLatestMessage();
			clearDate = latestMessage.getTimeSent() + 1000;
			reference = latestMessage.getServerMsgId();
		} else {
			clearDate = System.currentTimeMillis();
			reference = null;
		}
		conversation.clearMessages();
		conversation.setHasMessagesLeftOnServer(false); //avoid messages getting loaded through mam
		conversation.setLastClearHistory(clearDate, reference);
		Runnable runnable = () -> {
			databaseBackend.deleteMessagesInConversation(conversation);
			databaseBackend.updateConversation(conversation);
		};
		mDatabaseWriterExecutor.execute(runnable);
	}

	public boolean sendBlockRequest(final Blockable blockable, boolean reportSpam) {
		if (blockable != null && blockable.getBlockedJid() != null) {
			final Jid jid = blockable.getBlockedJid();
			this.sendIqPacket(blockable.getAccount(), getIqGenerator().generateSetBlockRequest(jid, reportSpam), new OnIqPacketReceived() {

				@Override
				public void onIqPacketReceived(final Account account, final IqPacket packet) {
					if (packet.getType() == IqPacket.TYPE.RESULT) {
						account.getBlocklist().add(jid);
						updateBlocklistUi(OnUpdateBlocklist.Status.BLOCKED);
					}
				}
			});
			if (removeBlockedConversations(blockable.getAccount(), jid)) {
				updateConversationUi();
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean removeBlockedConversations(final Account account, final Jid blockedJid) {
		boolean removed = false;
		synchronized (this.conversations) {
			boolean domainJid = blockedJid.getLocal() == null;
			for (Conversation conversation : this.conversations) {
				boolean jidMatches = (domainJid && blockedJid.getDomain().equals(conversation.getJid().getDomain()))
						|| blockedJid.equals(conversation.getJid().asBareJid());
				if (conversation.getAccount() == account
						&& conversation.getMode() == Conversation.MODE_SINGLE
						&& jidMatches) {
					this.conversations.remove(conversation);
					markRead(conversation);
					conversation.setStatus(Conversation.STATUS_ARCHIVED);
					Log.d(Config.LOGTAG, account.getLogJid() + ": archiving conversation " + conversation.getJid().asBareJid() + " because jid was blocked");
					updateConversation(conversation);
					removed = true;
				}
			}
		}
		return removed;
	}

	public void sendUnblockRequest(final Blockable blockable) {
		if (blockable != null && blockable.getJid() != null) {
			final Jid jid = blockable.getBlockedJid();
			this.sendIqPacket(blockable.getAccount(), getIqGenerator().generateSetUnblockRequest(jid), new OnIqPacketReceived() {
				@Override
				public void onIqPacketReceived(final Account account, final IqPacket packet) {
					if (packet.getType() == IqPacket.TYPE.RESULT) {
						account.getBlocklist().remove(jid);
						updateBlocklistUi(OnUpdateBlocklist.Status.UNBLOCKED);
					}
				}
			});
		}
	}

	//ALF AM-569
	//buddyUniqueIds, callid: callid)
	public void addContactsToCall(List<Jid> receivers) {
		final String deviceId = PhoneHelper.getAndroidId(this);
		if (currentTwilioCall == null) {
			return;
		}

		final IqPacket request = new IqPacket(IqPacket.TYPE.SET);
		request.setTo(Jid.of("p2.glaciersec.cc"));
		request.setAttribute("from",currentTwilioCall.getAccount().getJid().toString());

		final Element command = request.addChild("command", "http://jabber.org/protocol/commands");
		command.setAttribute("action", "execute");
		command.setAttribute("node", "call-user-fcm");
		final Element x = command.addChild("x", "jabber:x:data");
		x.setAttribute("type", "submit");

		final Element callerfield = x.addChild("field");
		callerfield.setAttribute("var", "caller");
		Element callerval = new Element("value");
		String name = currentTwilioCall.getAccount().getDisplayName();
		if (name == null) {
			name = currentTwilioCall.getAccount().getUsername();
		}
		callerval.setContent(name);
		callerfield.addChild(callerval);

		final Element callerdevice = x.addChild("field");
		callerdevice.setAttribute("var", "callerdevice");
		Element deviceval = new Element("value");
		deviceval.setContent(deviceId);
		callerdevice.addChild(deviceval);

		final Element receiver = x.addChild("field");
		receiver.setAttribute("var", "receiver");
		for (Jid receiverjid : receivers){
			Element receiverval = new Element("value");
			receiverval.setContent(receiverjid.toString());
			receiver.addChild(receiverval);
		}

		if (currentTwilioCall.getRoomTitle() != null) {
			final Element calltitle = x.addChild("field");
			calltitle.setAttribute("var", "title");
			Element titleval = new Element("value");
			titleval.setContent(currentTwilioCall.getRoomTitle());
			calltitle.addChild(titleval);
		}

		final Element roomname = x.addChild("field");
		roomname.setAttribute("var", "roomname");
		Element roomnameval = new Element("value");
		roomnameval.setContent(currentTwilioCall.getRoomName());
		roomname.addChild(deviceval);

		final Element callid = x.addChild("field");
		callid.setAttribute("var", "callid");
		Element callidval = new Element("value");
		callidval.setContent(Integer.toString(currentTwilioCall.getCallId()));
		callid.addChild(callidval);

		final Element inprogress = x.addChild("field");
		inprogress.setAttribute("var", "inprogress");
		Element inprogressval = new Element("value");
		inprogressval.setContent("true");
		inprogress.addChild(inprogressval);

		sendIqPacket(currentTwilioCall.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(final Account account, final IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					final Element command = packet.findChild("command", "http://jabber.org/protocol/commands");
					if (command == null) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
						return;
					}

					final Element x = command.findChild("x", "jabber:x:data");
					if (x == null) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
						return;
					}
				} else {
					//callback.informUser("Something bad");
					Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
				}
			}
		});
	}

	//ALF AM-410 (next 3) //receiver is bare jid of receiver
	public void sendCallRequest(TwilioCall call) {
		final String deviceId = PhoneHelper.getAndroidId(this);
		currentTwilioCall = null;

		final IqPacket request = new IqPacket(IqPacket.TYPE.SET);
		request.setTo(Jid.of("p2.glaciersec.cc"));
		request.setAttribute("from",call.getAccount().getJid().toString());

		final Element command = request.addChild("command", "http://jabber.org/protocol/commands");
		command.setAttribute("action", "execute");
		command.setAttribute("node", "call-user-fcm");
		final Element x = command.addChild("x", "jabber:x:data");
		x.setAttribute("type", "submit");

		final Element callerfield = x.addChild("field");
		callerfield.setAttribute("var", "caller");
		Element callerval = new Element("value");
		String name = call.getAccount().getDisplayName();
		if (name == null) {
			name = call.getAccount().getUsername();
		}
		callerval.setContent(name);
		callerfield.addChild(callerval);

		final Element callerdevice = x.addChild("field");
		callerdevice.setAttribute("var", "callerdevice");
		Element deviceval = new Element("value");
		deviceval.setContent(deviceId);
		callerdevice.addChild(deviceval);

		final Element receiver = x.addChild("field");
		receiver.setAttribute("var", "receiver");
		//AM-558 (and room title below)
		final String[] receivers = call.getReceiver().split(",");
		for (String receiverstr : receivers){
			Element receiverval = new Element("value");
			receiverval.setContent(receiverstr);
			receiver.addChild(receiverval);
		}
		//Element receiverval = new Element("value");
		//receiverval.setContent(call.getReceiver());
		//receiver.addChild(receiverval);
		if (call.getRoomTitle() != null) {
			final Element calltitle = x.addChild("field");
			calltitle.setAttribute("var", "title");
			Element titleval = new Element("value");
			titleval.setContent(call.getRoomTitle());
			calltitle.addChild(titleval);
		}

		Log.d(Config.LOGTAG, call.getAccount().getLogJid() + ": making call request to " + Tools.logJid(call.getReceiver()));
		sendIqPacket(call.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(final Account account, final IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					final Element command = packet.findChild("command", "http://jabber.org/protocol/commands");
					if (command == null) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
						return;
					}

					final Element x = command.findChild("x", "jabber:x:data");
					if (x == null) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
						return;
					}

					for (final Element item : x.getChildren()) {
						//<field var="callid"><value>237</value></field>
						if (item.getName().equals("field")) {
							if (item.getAttribute("var").equals("caller")) {
								call.setCaller(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("receiver")) {
								call.setReceiver(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("room_name")) {
								call.setRoomName(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("title")) { //ALF AM-558
								call.setRoomTitle(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("token")) {
								call.setToken(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("call_id")) {
								String call_id = item.findChild("value").getContent();
								try {
									int callid = Integer.parseInt(call_id);
									call.setCallId(callid);
								} catch (NumberFormatException nfe) {}
							}
						}
					}

					call.setStatus("waiting"); //ALF AM-558
					currentTwilioCall = call;
				} else {
					//callback.informUser("Something bad");
					Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
				}
			}
		});
	}

	public void acceptCall(TwilioCall call) {
		//CMG AM-541
		if (call == null ){
			Toast.makeText(this, R.string.call_nolonger_exisits, Toast.LENGTH_LONG).show();
			Intent intent1 = new Intent("callActivityFinish");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
			getNotificationService().dismissCallNotification();
			return ;
		}
		final String deviceId = PhoneHelper.getAndroidId(this);
		int callid = call.getCallId();

		final IqPacket request = new IqPacket(IqPacket.TYPE.SET);
		request.setTo(Jid.of("p2.glaciersec.cc"));
		request.setAttribute("from",call.getAccount().getJid().toString());

		final Element command = request.addChild("command", "http://jabber.org/protocol/commands");
		command.setAttribute("action", "execute");
		command.setAttribute("node", "accept-call-fcm");
		final Element x = command.addChild("x", "jabber:x:data");
		x.setAttribute("type", "submit");

		final Element callidfield = x.addChild("field");
		callidfield.setAttribute("var", "callid");
		Element callidval = new Element("value");
		callidval.setContent(Integer.toString(callid));
		callidfield.addChild(callidval);

		final Element receiverdevice = x.addChild("field");
		receiverdevice.setAttribute("var", "receiverdevice");
		Element deviceval = new Element("value");
		deviceval.setContent(deviceId);
		receiverdevice.addChild(deviceval);

		/*final Element migratedfield = x.addChild("field");
		migratedfield.setAttribute("var", "migrated");
		Element migratedval = new Element("value");
		migratedval.setContent("true");
		migratedfield.addChild(migratedval);*/ //ALF AM-558

		Log.d(Config.LOGTAG, call.getAccount().getLogJid() + ": accepting call from " + call.getCaller());
		sendIqPacket(call.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(final Account account, final IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					final Element command = packet.findChild("command", "http://jabber.org/protocol/commands");
					if (command == null) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
						return;
					}

					final Element x = command.findChild("x", "jabber:x:data");
					if (x == null) {
						Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
						return;
					}
					for (final Element item : x.getChildren()) {
						if (item.getName().equals("field")) {
							if (item.getAttribute("var").equals("call_id")) {
								String call_id = item.findChild("value").getContent();
								try {
									int callid = Integer.parseInt(call_id);
									call.setCallId(callid);
								} catch (NumberFormatException nfe) {}
							} else if (item.getAttribute("var").equals("token")) {
								call.setToken(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("roomname")) {
								call.setRoomName(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("caller")) {
								call.setCaller(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("receiver")) {
								call.setReceiver(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("title")) { //ALF AM-558
								call.setRoomTitle(item.findChild("value").getContent());
							}
						}
					}

					//ALF AM-431 send message
					call.setStatus("accept");
					MessagePacket messagePacket = mMessageGenerator.callUpdate(call, Jid.of(call.getCaller()));
					sendMessagePacket(call.getAccount(), messagePacket);

					Intent intent1 = new Intent("callActivityFinish");
					LocalBroadcastManager.getInstance(XmppConnectionService.this).sendBroadcast(intent1);
					getNotificationService().dismissCallNotification();

					//ALF AM-421
					//Conversation c = find(getConversations(), account, Jid.of(call.getCaller())); //DJF DJF AM-438

					//AM-558 accept message already sent...this should be the group conversation if group
					Conversation c = null;
					if (call.getRoomTitle() == null){
						c = findOrCreateConversation(account, Jid.of(call.getCaller()), false, true);
					}
					else if (call.getRoomTitle().startsWith("#")) {
						String server = findConferenceServer(account);
						String name = call.getRoomTitle().substring(1);
						//AM-599
						try {
							Jid roomjid = findRoomJidfromRoomTitle(name, account);
							c = findOrCreateConversation(account, roomjid, true, true);
						} catch (Exception re) {}
					} else {
						c = findOrCreateConversation(account, Jid.of(call.getCaller()), false, true);
					}
					//Conversation c = findOrCreateConversation(account, Jid.of(call.getCaller()), false, true);

					if (c != null) {
						Message msg = Message.createCallStatusMessage(c, Message.STATUS_CALL_RECEIVED);
						c.add(msg);
						databaseBackend.createMessage(msg);
					}

					//open RoomActivity with callToken/info
					/*Intent callIntent = new Intent(getApplicationContext(), VideoActivity.class);
					callIntent.setAction(CallActivity.ACTION_ACCEPTED_CALL);
					callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					callIntent.putExtra("call_id", call.getCallId());
					callIntent.putExtra("token", call.getToken());
					callIntent.putExtra("roomname", call.getRoomName());
					callIntent.putExtra("caller", call.getCaller());
					callIntent.putExtra("receiver", call.getReceiver());

					startActivity(callIntent);*/
					if (currentTwilioCall != null) {
						currentTwilioCall.setStatus("inprogress"); //ALF AM-558
					}
					callManager.initCall(call); //AM-478
				} else {
					//callback.informUser("Something bad"); //TODO ALERT USER
					Log.d(Config.LOGTAG, account.getLogJid() + ": could not create call");
				}
			}
		});
	}

	public void rejectCall(TwilioCall call, boolean isBusy) {
		final String deviceId = PhoneHelper.getAndroidId(this);
		callManager.stopCallAudio(); //AM-581

		if (call == null) {
			//Close CallActivity
			Intent intent1 = new Intent("callActivityFinish");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
			getNotificationService().dismissCallNotification();
			return;
		}


		final IqPacket request = new IqPacket(IqPacket.TYPE.SET);
		request.setTo(Jid.of("p2.glaciersec.cc"));
		request.setAttribute("from",call.getAccount().getJid().toString());

		final Element command = request.addChild("command", "http://jabber.org/protocol/commands");
		command.setAttribute("action", "execute");
		if (isBusy) {
			command.setAttribute("node", "busy-call-fcm");
		} else {
			command.setAttribute("node", "reject-call-fcm");
		}
		final Element x = command.addChild("x", "jabber:x:data");
		x.setAttribute("type", "submit");

		final Element callidfield = x.addChild("field");
		callidfield.setAttribute("var", "callid");
		Element callidval = new Element("value");
		callidval.setContent(Integer.toString(call.getCallId()));
		callidfield.addChild(callidval);

		//AM-612, IOSM-569
		final Element receiverdevicefield = x.addChild("field");
		receiverdevicefield.setAttribute("var", "receiverdevice");
		Element receiverdeviceval = new Element("value");
		receiverdeviceval.setContent(deviceId);
		receiverdevicefield.addChild(receiverdeviceval);

		final Element migratedfield = x.addChild("field");
		migratedfield.setAttribute("var", "migrated");
		Element migratedval = new Element("value");
		migratedval.setContent("true");
		migratedfield.addChild(migratedval);

		Log.d(Config.LOGTAG, call.getAccount().getLogJid() + ": rejecting call from " + call.getCaller());

		//ALF AM-431 handle response
		sendIqPacket(call.getAccount(), request, new OnIqPacketReceived() {
			@Override
			public void onIqPacketReceived(final Account account, final IqPacket packet) {
				if (packet.getType() == IqPacket.TYPE.RESULT) {
					final Element command = packet.findChild("command", "http://jabber.org/protocol/commands");
					if (command == null) {
						return;
					}
					final Element x = command.findChild("x", "jabber:x:data");
					if (x == null) {
						return;
					}
					for (final Element item : x.getChildren()) {
						if (item.getName().equals("field")) {
							if (item.getAttribute("var").equals("call_id")) {
								String call_id = item.findChild("value").getContent();
								try {
									int callid = Integer.parseInt(call_id);
									call.setCallId(callid);
								} catch (NumberFormatException nfe) {
								}
							} else if (item.getAttribute("var").equals("caller")) {
								call.setCaller(item.findChild("value").getContent());
							} else if (item.getAttribute("var").equals("title")) { //ALF AM-558
								call.setRoomTitle(item.findChild("value").getContent());
							}
						}
					}

					if (isBusy) {
						call.setStatus("busy");

						//ALF AM-421
						//Conversation c = find(getConversations(), account, Jid.of(call.getCaller())); //DJF DJF AM-438

						//AM-558 accept message already sent...this should be the group conversation if group
						Conversation c = null;
						if (call.getRoomTitle() != null && call.getRoomTitle().startsWith("#")) {

							String name = call.getRoomTitle().substring(1);

							try {
								Jid roomjid = findRoomJidfromRoomTitle(name, account);
								c = findOrCreateConversation(account, roomjid, true, true);
							} catch (Exception re) {}
						} else {
							c = findOrCreateConversation(account, Jid.of(call.getCaller()), false, true);
						}
						//Conversation c = findOrCreateConversation(account, Jid.of(call.getCaller()), false, true);

						if (c != null) {
							Message msg = Message.createCallStatusMessage(c, Message.STATUS_CALL_MISSED);
							msg.markUnread(); //AM#10
							getNotificationService().notifyMissedCall(c); //ALF AM-468
							c.add(msg);
							databaseBackend.createMessage(msg);
						}
					} else {
						call.setStatus("reject");
					}
					MessagePacket messagePacket = mMessageGenerator.callUpdate(call, Jid.of(call.getCaller()));
					sendMessagePacket(call.getAccount(), messagePacket);
				}
			}
		});

		//Close CallActivity
		Intent intent1 = new Intent("callActivityFinish");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
		getNotificationService().dismissCallNotification();
	}

	//AM-599
	public Jid findRoomJidfromRoomTitle(String roomTitle, Account account){
		for (Jid jid: account.getAvailableGroups()){
			Conversation c = find(account, jid);
			if (c != null) {
				String cRoomTitle = c.getName().toString();
				if (roomTitle.equals(cRoomTitle)) {
					return jid;
				}
			}
		}
		String server = findConferenceServer(account);
		return Jid.of(roomTitle + "@" + server);
	}

	public void cancelCall(TwilioCall call) {

		if (call == null) {
			Intent intent1 = new Intent("callActivityFinish");
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
			return;
		}

		final IqPacket request = new IqPacket(IqPacket.TYPE.SET);
		request.setTo(Jid.of("p2.glaciersec.cc"));
		request.setAttribute("from",call.getAccount().getJid().toString());

		final Element command = request.addChild("command", "http://jabber.org/protocol/commands");
		command.setAttribute("action", "execute");
		command.setAttribute("node", "cancel-call-fcm");
		final Element x = command.addChild("x", "jabber:x:data");
		x.setAttribute("type", "submit");

		final Element callidfield = x.addChild("field");
		callidfield.setAttribute("var", "callid");
		Element callidval = new Element("value");
		callidval.setContent(Integer.toString(call.getCallId()));
		callidfield.addChild(callidval);

		Log.d(Config.LOGTAG, call.getAccount().getLogJid() + ": cancelling call"); // from " + call.getCaller());
		sendIqPacket(call.getAccount(), request, null);

		//ALF AM-431 send message
		call.setStatus("cancel");
		MessagePacket packet = mMessageGenerator.callUpdate(call, Jid.of(call.getReceiver()));
		sendMessagePacket(call.getAccount(), packet);

		//Close CallActivity
		Intent intent1 = new Intent("callActivityFinish");
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
	}
	//ALF AM-410 end TwilioCall stuff


	public void publishDisplayName(Account account) {
		String displayName = account.getDisplayName();
		final IqPacket request;
		if (TextUtils.isEmpty(displayName)) {
			request = mIqGenerator.deleteNode(Namespace.NICK);
		} else {
			request = mIqGenerator.publishNick(displayName);
		}
		mAvatarService.clear(account);
		sendIqPacket(account, request, (account1, packet) -> {
			if (packet.getType() == IqPacket.TYPE.ERROR) {
				Log.d(Config.LOGTAG, account1.getJid().asBareJid() + ": unable to modify nick name "); //+packet.toString());
			}
		});

		//ALF AM-48, 03/19 might need to rethink this
		if (displayName != null && !displayName.isEmpty()) {

			IqPacket publishv = mIqGenerator.publishVcardWithNick(displayName);
			sendIqPacket(account, publishv, (account2, packet2) -> {
				if (packet2.getType() == IqPacket.TYPE.ERROR) {
					Log.d(Config.LOGTAG, account2.getJid().asBareJid() + ": could not publish vcardnick");
				}
			});
			sendPresencePacket(account, mPresenceGenerator.sendPresenceWithvCard(account, displayName));
		}
	}

	public ServiceDiscoveryResult getCachedServiceDiscoveryResult(Pair<String, String> key) {
		ServiceDiscoveryResult result = discoCache.get(key);
		if (result != null) {
			return result;
		} else {
			result = databaseBackend.findDiscoveryResult(key.first, key.second);
			if (result != null) {
				discoCache.put(key, result);
			}
			return result;
		}
	}

	public void fetchCaps(Account account, final Jid jid, final Presence presence) {
		final Pair<String, String> key = new Pair<>(presence.getHash(), presence.getVer());
		ServiceDiscoveryResult disco = getCachedServiceDiscoveryResult(key);
		if (disco != null) {
			presence.setServiceDiscoveryResult(disco);
		} else {
			if (!account.inProgressDiscoFetches.contains(key)) {
				account.inProgressDiscoFetches.add(key);
				IqPacket request = new IqPacket(IqPacket.TYPE.GET);
				request.setTo(jid);
				final String node = presence.getNode();
				final String ver = presence.getVer();
				final Element query = request.query("http://jabber.org/protocol/disco#info");
				if (node != null && ver != null) {
					query.setAttribute("node",node+"#"+ver);
				}
				Log.d(Config.LOGTAG, account.getLogJid() + ": making disco request to " + Tools.logJid(jid));
				sendIqPacket(account, request, (a, response) -> {
					if (response.getType() == IqPacket.TYPE.RESULT) {
						ServiceDiscoveryResult discoveryResult = new ServiceDiscoveryResult(response);
						if (presence.getVer().equals(discoveryResult.getVer())) {
							databaseBackend.insertDiscoveryResult(discoveryResult);
							injectServiceDiscoveryResult(a.getRoster(), presence.getHash(), presence.getVer(), discoveryResult);
						} else {
							Log.d(Config.LOGTAG, a.getLogJid() + ": mismatch in caps for contact " + Tools.logJid(jid) + " " + presence.getVer() + " vs " + discoveryResult.getVer());
						}
					}
					a.inProgressDiscoFetches.remove(key);
				});
			}
		}
	}

	private void injectServiceDiscoveryResult(Roster roster, String hash, String ver, ServiceDiscoveryResult disco) {
		for (Contact contact : roster.getContacts()) {
			for (Presence presence : contact.getPresences().getPresences().values()) {
				if (hash.equals(presence.getHash()) && ver.equals(presence.getVer())) {
					presence.setServiceDiscoveryResult(disco);
				}
			}
		}
	}

	public void fetchMamPreferences(Account account, final OnMamPreferencesFetched callback) {
		final MessageArchiveService.Version version = MessageArchiveService.Version.get(account);
		IqPacket request = new IqPacket(IqPacket.TYPE.GET);
		request.addChild("prefs", version.namespace);
		sendIqPacket(account, request, (account1, packet) -> {
			Element prefs = packet.findChild("prefs", version.namespace);
			if (packet.getType() == IqPacket.TYPE.RESULT && prefs != null) {
				callback.onPreferencesFetched(prefs);
			} else {
				callback.onPreferencesFetchFailed();
			}
		});
	}

	public PushManagementService getPushManagementService() {
		return mPushManagementService;
	}

	public void changeStatus(Account account, PresenceTemplate template, String signature) {
		if (!template.getStatusMessage().isEmpty()) {
			databaseBackend.insertPresenceTemplate(template);
		}
		//account.setPgpSignature(signature);
		account.setPresenceStatus(template.getStatus());
		account.setPresenceStatusMessage(template.getStatusMessage());
		databaseBackend.updateAccount(account);
		sendPresence(account);
	}

	public List<PresenceTemplate> getPresenceTemplates(Account account) {
		List<PresenceTemplate> templates = databaseBackend.getPresenceTemplates();
		for (PresenceTemplate template : account.getSelfContact().getPresences().asTemplates()) {
			if (!templates.contains(template)) {
				templates.add(0, template);
			}
		}
		return templates;
	}

	public void saveConversationAsBookmark(Conversation conversation, String name) {
		Account account = conversation.getAccount();
		Bookmark bookmark = new Bookmark(account, conversation.getJid().asBareJid());
		if (!conversation.getJid().isBareJid()) {
			bookmark.setNick(conversation.getJid().getResource());
		}
		if (!TextUtils.isEmpty(name)) {
			bookmark.setBookmarkName(name);
		}
		bookmark.setAutojoin(getPreferences().getBoolean("autojoin", getResources().getBoolean(R.bool.autojoin)));
		account.getBookmarks().add(bookmark);
		pushBookmarks(account);
		bookmark.setConversation(conversation);
	}

	public boolean verifyFingerprints(Contact contact, List<XmppUri.Fingerprint> fingerprints) {
		boolean performedVerification = false;
		final AxolotlService axolotlService = contact.getAccount().getAxolotlService();
		for (XmppUri.Fingerprint fp : fingerprints) {
			if (fp.type == XmppUri.FingerprintType.GLACIER) {
				String fingerprint = "05" + fp.fingerprint.replaceAll("\\s", "");
				FingerprintStatus fingerprintStatus = axolotlService.getFingerprintTrust(fingerprint);
				if (fingerprintStatus != null) {
					if (!fingerprintStatus.isVerified()) {
						performedVerification = true;
						axolotlService.setFingerprintTrust(fingerprint, fingerprintStatus.toVerified());
					}
				} else {
					axolotlService.preVerifyFingerprint(contact, fingerprint);
				}
			}
		}
		return performedVerification;
	}

	public boolean verifyFingerprints(Account account, List<XmppUri.Fingerprint> fingerprints) {
		final AxolotlService axolotlService = account.getAxolotlService();
		boolean verifiedSomething = false;
		for (XmppUri.Fingerprint fp : fingerprints) {
			if (fp.type == XmppUri.FingerprintType.GLACIER) {
				String fingerprint = "05" + fp.fingerprint.replaceAll("\\s", "");
				Log.d(Config.LOGTAG, "trying to verify own fp=" + fingerprint);
				FingerprintStatus fingerprintStatus = axolotlService.getFingerprintTrust(fingerprint);
				if (fingerprintStatus != null) {
					if (!fingerprintStatus.isVerified()) {
						axolotlService.setFingerprintTrust(fingerprint, fingerprintStatus.toVerified());
						verifiedSomething = true;
					}
				} else {
					axolotlService.preVerifyFingerprint(account, fingerprint);
					verifiedSomething = true;
				}
			}
		}
		return verifiedSomething;
	}

	public boolean blindTrustBeforeVerification() {
		return getBooleanPreference(SettingsActivity.BLIND_TRUST_BEFORE_VERIFICATION, R.bool.btbv);
	}

	public ShortcutService getShortcutService() {
		return mShortcutService;
	}

	//ALF AM-344 start
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mService = IOpenVPNAPIService.Stub.asInterface(service);

		try {
			mService.registerStatusCallback(mCallback);
		} catch (RemoteException | SecurityException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onServiceDisconnected(ComponentName className) {
		// This is called when the connection with the service has been
		// unexpectedly disconnected -- that is, its process crashed.
		mService = null;
	}

	private IOpenVPNStatusCallback mCallback = new IOpenVPNStatusCallback.Stub() {
		/**
		 * This is called by the remote service regularly to tell us about
		 * new values.  Note that IPC calls are dispatched through a thread
		 * pool running in each process, so the code executing here will
		 * NOT be running in our main thread like most other things -- so,
		 * to update the UI, we need to use a Handler to hop over there.
		 */

		@Override
		public void newStatus(String uuid, String state, String message, String level)
				throws RemoteException {
			android.os.Message msg = android.os.Message.obtain(mHandler, MSG_UPDATE_STATE, state + "|" + message);
			msg.sendToTarget();
		}
	};

	@Override
	public boolean handleMessage(android.os.Message msg) {
		if(msg.what == MSG_UPDATE_STATE) {
			if (msg.obj.toString().startsWith("CONNECTED")) {
				for (Account account : accounts) {
					Log.d(Config.LOGTAG, account.getLogJid() + ": Core just connected. Account status: " + account.getStatus());
					if (account.getStatus() == Account.State.OFFLINE || account.getStatus() == Account.State.NO_INTERNET ||
							account.getStatus() == Account.State.SERVER_NOT_FOUND) {
						resetSendingToWaiting(account);
						if (account.isEnabled() && !account.isOnlineAndConnected()) {
							Log.d(Config.LOGTAG, account.getLogJid() + ": Core just connected. Reconnecting account now");
							reconnectAccount(account, true, false);
						}
					}
				}
			}
		}
		return true;
	}
	//ALF AM-344 end

	public void pushMamPreferences(Account account, Element prefs) {
		IqPacket set = new IqPacket(IqPacket.TYPE.SET);
		set.addChild(prefs);
		sendIqPacket(account, set, null);
	}

	public interface OnMamPreferencesFetched {
		void onPreferencesFetched(Element prefs);

		void onPreferencesFetchFailed();
	}

	public interface OnAccountCreated {
		void onAccountCreated(Account account);

		void informUser(int r);
	}

	public interface OnMoreMessagesLoaded {
		void onMoreMessagesLoaded(int count, Conversation conversation);

		void informUser(int r);
	}

	public interface OnAccountPasswordChanged {
		void onPasswordChangeSucceeded();

		void onPasswordChangeFailed();
	}

	public interface OnRoomDestroy {
		void onRoomDestroySucceeded();

		void onRoomDestroyFailed();
	}

	public interface OnAffiliationChanged {
		void onAffiliationChangedSuccessful(Jid jid);

		void onAffiliationChangeFailed(Jid jid, int resId);
	}

	public interface OnConversationUpdate {
		void onConversationUpdate();
	}

	public interface OnAccountUpdate {
		void onAccountUpdate();
	}

	public interface OnCaptchaRequested {
		void onCaptchaRequested(Account account, String id, Data data, Bitmap captcha);
	}

	public interface OnRosterUpdate {
		void onRosterUpdate();
	}

	public interface OnMucRosterUpdate {
		void onMucRosterUpdate();
	}

	public interface OnConferenceConfigurationFetched {
		void onConferenceConfigurationFetched(Conversation conversation);

		void onFetchFailed(Conversation conversation, Element error);
	}

	public interface OnConferenceJoined {
		void onConferenceJoined(Conversation conversation);
	}

	public interface OnConfigurationPushed {
		void onPushSucceeded();

		void onPushFailed();
	}

	public interface OnCallStateChanged {
		void onCallConnected();

		void onCallEnded();
	}

	public interface OnShowErrorToast {
		void onShowErrorToast(int resId);
	}

	public interface OnProcessLifecycleUpdate { //AM#14
		void onProcessLifecycleUpdate();
	}

	public class XmppConnectionBinder extends Binder {
		public XmppConnectionService getService() {
			return XmppConnectionService.this;
		}
	}

	private class InternalEventReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			onStartCommand(intent,0,0);
		}
	}
}
