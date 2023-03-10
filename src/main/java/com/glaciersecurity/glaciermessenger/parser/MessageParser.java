package com.glaciersecurity.glaciermessenger.parser;

import com.glaciersecurity.glaciermessenger.utils.Log;
import android.util.Pair;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.AxolotlService;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.NotEncryptedForThisDeviceException;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.XmppAxolotlMessage;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.BrokenSessionException;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.entities.ReadByMarker;
import com.glaciersecurity.glaciermessenger.entities.ReceiptRequest;
import com.glaciersecurity.glaciermessenger.entities.TwilioCall;
import com.glaciersecurity.glaciermessenger.http.HttpConnectionManager;
import com.glaciersecurity.glaciermessenger.http.P1S3UrlStreamHandler;
import com.glaciersecurity.glaciermessenger.services.MessageArchiveService;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.services.QuickConversationsService;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.CryptoHelper;
import com.glaciersecurity.glaciermessenger.xml.Namespace;
import com.glaciersecurity.glaciermessenger.xml.Element;
import com.glaciersecurity.glaciermessenger.xmpp.InvalidJid;
import com.glaciersecurity.glaciermessenger.xmpp.OnMessagePacketReceived;
import com.glaciersecurity.glaciermessenger.xmpp.chatstate.ChatState;
import com.glaciersecurity.glaciermessenger.xmpp.pep.Avatar;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.MessagePacket;
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class MessageParser extends AbstractParser implements OnMessagePacketReceived {

	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

	public MessageParser(XmppConnectionService service) {
		super(service);
	}

	private static String extractStanzaId(Element packet, boolean isTypeGroupChat, Conversation conversation) {
		final Jid by;
		final boolean safeToExtract;
		if (isTypeGroupChat) {
			by = conversation.getJid().asBareJid();
			safeToExtract = conversation.getMucOptions().hasFeature(Namespace.STANZA_IDS);
		} else {
			Account account = conversation.getAccount();
			by = account.getJid().asBareJid();
			safeToExtract = account.getXmppConnection().getFeatures().stanzaIds();
		}
		return safeToExtract ? extractStanzaId(packet, by) : null;
	}

	private static String extractStanzaId(Element packet, Jid by) {
		for (Element child : packet.getChildren()) {
			if (child.getName().equals("stanza-id")
					&& Namespace.STANZA_IDS.equals(child.getNamespace())
					&& by.equals(InvalidJid.getNullForInvalid(child.getAttributeAsJid("by")))) {
				return child.getAttribute("id");
			}
		}
		return null;
	}

	private static Jid getTrueCounterpart(Element mucUserElement, Jid fallback) {
		final Element item = mucUserElement == null ? null : mucUserElement.findChild("item");
		Jid result = item == null ? null : InvalidJid.getNullForInvalid(item.getAttributeAsJid("jid"));
		return result != null ? result : fallback;
	}

	private boolean extractChatState(Conversation c, final boolean isTypeGroupChat, final MessagePacket packet) {
		ChatState state = ChatState.parse(packet);
		if (state != null && c != null) {
			final Account account = c.getAccount();
			Jid from = packet.getFrom();
			if (from.asBareJid().equals(account.getJid().asBareJid())) {
				c.setOutgoingChatState(state);
				if (state == ChatState.ACTIVE || state == ChatState.COMPOSING) {
					mXmppConnectionService.markRead(c);
					activateGracePeriod(account);
				}
				return false;
			} else {
				if (isTypeGroupChat) {
					MucOptions.User user = c.getMucOptions().findUserByFullJid(from);
					if (user != null) {
						return user.setChatState(state);
					} else {
						return false;
					}
				} else {
					return c.setIncomingChatState(state);
				}
			}
		}
		return false;
	}

	private Message parseAxolotlChat(Element axolotlMessage, Jid from, Conversation conversation, int status, boolean checkedForDuplicates, boolean postpone) {
		final AxolotlService service = conversation.getAccount().getAxolotlService();
		final XmppAxolotlMessage xmppAxolotlMessage;
		try {
			xmppAxolotlMessage = XmppAxolotlMessage.fromElement(axolotlMessage, from.asBareJid());
		} catch (Exception e) {
			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid() + ": invalid omemo message received " + e.getMessage());
			return null;
		}
		if (xmppAxolotlMessage.hasPayload()) {
			final XmppAxolotlMessage.XmppAxolotlPlaintextMessage plaintextMessage;
			try {
				plaintextMessage = service.processReceivingPayloadMessage(xmppAxolotlMessage, postpone);
			} catch (BrokenSessionException e) {
				if (checkedForDuplicates) {
					service.reportBrokenSessionException(e, postpone);
					return new Message(conversation, "", Message.ENCRYPTION_AXOLOTL_FAILED, status);
				} else {
					//ALF AM-228
					Log.d(Config.LOGTAG,"ignoring broken session exception because checkedForDuplicates failed");
					return null;
					//service.handleBrokenSession(e);
					//return new Message(conversation, "", Message.ENCRYPTION_AXOLOTL_FAILED, status);
				}
			} catch (NotEncryptedForThisDeviceException e) {
				//service.handleNotEncryptedForDevice(); //ALF AM-228
				//ALF AM-228b added if/else
				if (conversation.returnNotForDevice()) {
					return new Message(conversation, "", Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE, status);
				} else {
					return null;
				}
			}
			if (plaintextMessage != null) {
				Message finishedMessage = new Message(conversation, plaintextMessage.getPlaintext(), Message.ENCRYPTION_AXOLOTL, status);
				finishedMessage.setFingerprint(plaintextMessage.getFingerprint());
				//Log.d(Config.LOGTAG, AxolotlService.getLogprefix(finishedMessage.getConversation().getAccount()) + "Received Message with session fingerprint: " + plaintextMessage.getFingerprint());
				Log.d(Config.LOGTAG, AxolotlService.getLogprefix(finishedMessage.getConversation().getAccount()) + "Received Message with session fingerprint ");
				return finishedMessage;
			} else if (!service.isReflected(xmppAxolotlMessage)){ //ALF AM-228   //ALF AM-287 if !reflected
				//service.verifySessions(conversation, from);
				//ALF AM-228b added if/else
				if (conversation.returnNotForDevice()) {
					return new Message(conversation, "", Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE, status);
				} else {
					return null;
				}
			}
		} else {
			Log.d(Config.LOGTAG, conversation.getAccount().getLogJid()+ ": received OMEMO key transport message");
			service.processReceivingKeyTransportMessage(xmppAxolotlMessage, postpone);
		}
		return null;
	}

	private Invite extractInvite(Account account, Element message) {
		Element x = message.findChild("x", "http://jabber.org/protocol/muc#user");
		if (x != null) {
			Element invite = x.findChild("invite");
			if (invite != null) {
				String password = x.findChildContent("password");
				Jid from = InvalidJid.getNullForInvalid(invite.getAttributeAsJid("from"));
				Contact contact = from == null ? null : account.getRoster().getContact(from);
				Jid room = InvalidJid.getNullForInvalid(message.getAttributeAsJid("from"));
				if (room == null) {
					return null;
				}
				return new Invite(room, password, contact);
			}
		} else {
			x = message.findChild("x", "jabber:x:conference");
			if (x != null) {
				Jid from = InvalidJid.getNullForInvalid(message.getAttributeAsJid("from"));
				Contact contact = from == null ? null : account.getRoster().getContact(from);
				Jid room = InvalidJid.getNullForInvalid(x.getAttributeAsJid("jid"));
				if (room == null) {
					return null;
				}
				return new Invite(room, x.getAttribute("password"), contact);
			}
		}
		return null;
	}

	private void parseEvent(final Element event, final Jid from, final Account account) {
		Element items = event.findChild("items");
		String node = items == null ? null : items.getAttribute("node");
		if ("urn:xmpp:avatar:metadata".equals(node)) {
			Avatar avatar = Avatar.parseMetadata(items);
			if (avatar != null) {
				avatar.owner = from.asBareJid();
				if (mXmppConnectionService.getFileBackend().isAvatarCached(avatar)) {
					if (account.getJid().asBareJid().equals(from)) {
						if (account.setAvatar(avatar.getFilename())) {
							mXmppConnectionService.databaseBackend.updateAccount(account);
							mXmppConnectionService.notifyAccountAvatarHasChanged(account);
						}
						mXmppConnectionService.getAvatarService().clear(account);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateAccountUi();
					} else {
						Contact contact = account.getRoster().getContact(from);
						if (contact.setAvatar(avatar)) {
							mXmppConnectionService.syncRoster(account);
							mXmppConnectionService.getAvatarService().clear(contact);
							mXmppConnectionService.updateConversationUi();
							mXmppConnectionService.updateRosterUi();
						}
					}
				} else if (mXmppConnectionService.isDataSaverDisabled()) {
					mXmppConnectionService.fetchAvatar(account, avatar);
				}
			}
		} else if (Namespace.NICK.equals(node)) {
			final Element i = items.findChild("item");
			final String nick = i == null ? null : i.findChildContent("nick", Namespace.NICK);
			if (nick != null) {
				setNick(account, from, nick);
			}
		} else if (AxolotlService.PEP_DEVICE_LIST.equals(node)) {
			Element item = items.findChild("item");
			Set<Integer> deviceIds = mXmppConnectionService.getIqParser().deviceIds(item);
			Log.d(Config.LOGTAG, AxolotlService.getLogprefix(account) + "Received PEP device list " + deviceIds + " update from " + getLogJid(from) + ", processing... ");
			AxolotlService axolotlService = account.getAxolotlService();
			axolotlService.registerDevices(from, deviceIds);
		} else if (Namespace.BOOKMARKS.equals(node) && account.getJid().asBareJid().equals(from)) {
			if (account.getXmppConnection().getFeatures().bookmarksConversion()) {
				final Element i = items.findChild("item");
				final Element storage = i == null ? null : i.findChild("storage", Namespace.BOOKMARKS);
				mXmppConnectionService.processBookmarks(account, storage, true);
				Log.d(Config.LOGTAG, account.getLogJid()+": processing bookmark PEP event");
			} else {
				Log.d(Config.LOGTAG, account.getLogJid()+": ignoring bookmark PEP event because bookmark conversion was not detected");
			}
		}
	}

	//CMG AM-
	public String getLogJid(Jid jid) {
		StringBuilder logJidBuilder = new StringBuilder();
		logJidBuilder.append(jid.getLocal().charAt(0));
		for (int i = 1; i < jid.getLocal().length(); i++) {
			logJidBuilder.append("*");
		}
		logJidBuilder.append("@");
		logJidBuilder.append(jid.getDomain().charAt(0));
		for (int i = 1; i < jid.getDomain().length(); i++) {
			logJidBuilder.append("*");
		}
		return logJidBuilder.toString();
	}

    private void parseDeleteEvent(final Element event, final Jid from, final Account account) {
        final Element delete = event.findChild("delete");
        if (delete == null) {
            return;
        }
        String node = delete.getAttribute("node");
        if (Namespace.NICK.equals(node)) {
            Log.d(Config.LOGTAG, "parsing nick delete event from " + from);
            setNick(account, from, null);
        }
    }

    private void setNick(Account account, Jid user, String nick) {
        if (user.asBareJid().equals(account.getJid().asBareJid())) {
            account.setDisplayName(nick);
            if (QuickConversationsService.isQuicksy()) {
                mXmppConnectionService.getAvatarService().clear(account);
            }
        } else {
            Contact contact = account.getRoster().getContact(user);
            if (contact.setPresenceName(nick)) {
                mXmppConnectionService.getAvatarService().clear(contact);
            }
        }
        mXmppConnectionService.updateConversationUi();
        mXmppConnectionService.updateAccountUi();
    }

	private boolean handleErrorMessage(Account account, MessagePacket packet) {
		if (packet.getType() == MessagePacket.TYPE_ERROR) {
			Jid from = packet.getFrom();
			if (from != null) {
				mXmppConnectionService.markMessage(account,
						from.asBareJid(),
						packet.getId(),
						Message.STATUS_SEND_FAILED,
						extractErrorMessage(packet));
			}
			return true;
		}
		return false;
	}

	@Override
	public void onMessagePacketReceived(Account account, MessagePacket original) {
		if (handleErrorMessage(account, original)) {
			return;
		}

		final MessagePacket packet;
		Long timestamp = null;
		boolean isCarbon = false;
		String serverMsgId = null;
		final Element fin = original.findChild("fin", MessageArchiveService.Version.MAM_0.namespace);
		if (fin != null) {
			mXmppConnectionService.getMessageArchiveService().processFinLegacy(fin, original.getFrom());
			return;
		}
		final Element result = MessageArchiveService.Version.findResult(original);
		final MessageArchiveService.Query query = result == null ? null : mXmppConnectionService.getMessageArchiveService().findQuery(result.getAttribute("queryid"));
		if (query != null && query.validFrom(original.getFrom())) {
			Pair<MessagePacket, Long> f = original.getForwardedMessagePacket("result", query.version.namespace);
			if (f == null) {
				return;
			}
			timestamp = f.second;
			packet = f.first;
			serverMsgId = result.getAttribute("id");
			query.incrementMessageCount();
		} else if (query != null) {
			Log.d(Config.LOGTAG, account.getLogJid() + ": received mam result from invalid sender");
			return;
		} else if (original.fromServer(account)) {
			Pair<MessagePacket, Long> f;
			f = original.getForwardedMessagePacket("received", "urn:xmpp:carbons:2");
			f = f == null ? original.getForwardedMessagePacket("sent", "urn:xmpp:carbons:2") : f;
			packet = f != null ? f.first : original;
			if (handleErrorMessage(account, packet)) {
				return;
			}
			timestamp = f != null ? f.second : null;
			isCarbon = f != null;
		} else {
			packet = original;

			//ALF AM-431
			final Element callElement = packet.findChild("x", "jabber:x:callupdate");
			if (callElement != null && callElement.getAttribute("callstatus") != null) {

				TwilioCall call = new TwilioCall(account);
				try {
					int callid = Integer.parseInt(callElement.getAttribute("callid"));
					call.setCallId(callid);
				} catch (NumberFormatException nfe) {
				}
				call.setStatus(callElement.getAttribute("callstatus"));

				//ALF AM-421
				if (call.getStatus().equalsIgnoreCase("cancel") && original.getFrom() != null) {
					call.setCaller(original.getFrom().asBareJid().toString());
					//AM-558 not sure if this value would be correct for groups
				}

				mXmppConnectionService.handleCallSetupMessage(account, call);
				return;
			}
		}

		if (timestamp == null) {
			timestamp = AbstractParser.parseTimestamp(original, AbstractParser.parseTimestamp(packet));
		}
		final String body = packet.getBody();
		final Element mucUserElement = packet.findChild("x", "http://jabber.org/protocol/muc#user");
		final String pgpEncrypted = packet.findChildContent("x", "jabber:x:encrypted");
		final Element replaceElement = packet.findChild("replace", "urn:xmpp:message-correct:0");
		final Element timerElement = packet.findChild("x", "jabber:x:msgexpire"); //ALF AM-53
		final Element oob = packet.findChild("x", Namespace.OOB);
		final Element xP1S3 = packet.findChild("x", Namespace.P1_S3_FILE_TRANSFER);
		final URL xP1S3url = xP1S3 == null ? null : P1S3UrlStreamHandler.of(xP1S3);
		final String oobUrl = oob != null ? oob.findChildContent("url") : null;
		final String replacementId = replaceElement == null ? null : replaceElement.getAttribute("id");
		final Element axolotlEncrypted = packet.findChild(XmppAxolotlMessage.CONTAINERTAG, AxolotlService.PEP_PREFIX);
		int status;
		final Jid counterpart;
		final Jid to = packet.getTo();
		final Jid from = packet.getFrom();
		final Element originId = packet.findChild("origin-id", Namespace.STANZA_IDS);
		final String remoteMsgId;
		if (originId != null && originId.getAttribute("id") != null) {
			remoteMsgId = originId.getAttribute("id");
		} else {
			remoteMsgId = packet.getId();
		}
		boolean notify = false;

		if (from == null || !InvalidJid.isValid(from) || !InvalidJid.isValid(to)) {
			Log.e(Config.LOGTAG, "encountered invalid message from='" + from + "' to='" + to + "'");
			return;
		}

		boolean isTypeGroupChat = packet.getType() == MessagePacket.TYPE_GROUPCHAT;
		if (query != null && !query.muc() && isTypeGroupChat) {
			Log.e(Config.LOGTAG, account.getLogJid() + ": received groupchat (" + from + ") message on regular MAM request. skipping");
			return;
		}
		boolean isMucStatusMessage = InvalidJid.hasValidFrom(packet) && from.isBareJid() && mucUserElement != null && mucUserElement.hasChild("status");
		boolean selfAddressed;
		if (packet.fromAccount(account)) {
			status = Message.STATUS_SEND;
			selfAddressed = to == null || account.getJid().asBareJid().equals(to.asBareJid());
			if (selfAddressed) {
				counterpart = from;
			} else {
				counterpart = to != null ? to : account.getJid();
			}
		} else {
			status = Message.STATUS_RECEIVED;
			counterpart = from;
			selfAddressed = false;
		}

		Invite invite = extractInvite(account, packet);
		if (invite != null && invite.execute(account)) {
			return;
		}

		if ((body != null || pgpEncrypted != null || (axolotlEncrypted != null && axolotlEncrypted.hasChild("payload")) || oobUrl != null || xP1S3 != null) && !isMucStatusMessage) {
			final boolean conversationIsProbablyMuc = isTypeGroupChat || mucUserElement != null || account.getXmppConnection().getMucServersWithholdAccount().contains(counterpart.getDomain());
			final Conversation conversation = mXmppConnectionService.findOrCreateConversation(account, counterpart.asBareJid(), conversationIsProbablyMuc, false, query, false);
			final boolean conversationMultiMode = conversation.getMode() == Conversation.MODE_MULTI;

			if (serverMsgId == null) {
				serverMsgId = extractStanzaId(packet, isTypeGroupChat, conversation);
			}


			if (selfAddressed) {
				if (mXmppConnectionService.markMessage(conversation, remoteMsgId, Message.STATUS_SEND_RECEIVED, serverMsgId)) {
					return;
				}
				status = Message.STATUS_RECEIVED;
				if (remoteMsgId != null && conversation.findMessageWithRemoteId(remoteMsgId, counterpart) != null) {
					return;
				}
			}

			if (isTypeGroupChat) {
				if (conversation.getMucOptions().isSelf(counterpart)) {
					status = Message.STATUS_SEND_RECEIVED;
					isCarbon = true; //not really carbon but received from another resource
					if (mXmppConnectionService.markMessage(conversation, remoteMsgId, status, serverMsgId)) {
						return;
					} else if (remoteMsgId == null || Config.IGNORE_ID_REWRITE_IN_MUC) {
						Message message = conversation.findSentMessageWithBody(packet.getBody());
						if (message != null) {
							mXmppConnectionService.markMessage(message, status);
							return;
						}
					}
				} else {
					status = Message.STATUS_RECEIVED;
				}
			}
			final Message message;
			if (xP1S3url != null) {
				message = new Message(conversation, xP1S3url.toString(), Message.ENCRYPTION_NONE, status);
				message.setOob(true);
				//if (CryptoHelper.isPgpEncryptedUrl(xP1S3url.toString())) {
				//	message.setEncryption(Message.ENCRYPTION_DECRYPTED);
				//}
			//} else if (pgpEncrypted != null && Config.supportOpenPgp()) {
			//	message = new Message(conversation, pgpEncrypted, Message.ENCRYPTION_PGP, status);
			} else if (axolotlEncrypted != null && Config.supportOmemo()) {
				Jid origin;
				Set<Jid> fallbacksBySourceId = Collections.emptySet();
				if (conversationMultiMode) {
					final Jid fallback = conversation.getMucOptions().getTrueCounterpart(counterpart);
					origin = getTrueCounterpart(query != null ? mucUserElement : null, fallback);
					if (origin == null) {
						try {
							fallbacksBySourceId = account.getAxolotlService().findCounterpartsBySourceId(XmppAxolotlMessage.parseSourceId(axolotlEncrypted));
						} catch (IllegalArgumentException e) {
							//ignoring
						}
					}
					if (origin == null && fallbacksBySourceId.size() == 0) {
						Log.d(Config.LOGTAG, "axolotl message in anonymous conference received and no possible fallbacks");
						return;
					}
				} else {
					fallbacksBySourceId = Collections.emptySet();
					origin = from;
				}
				final boolean checkedForDuplicates = serverMsgId != null && remoteMsgId != null && !conversation.possibleDuplicate(serverMsgId, remoteMsgId);
				if (origin != null) {
					message = parseAxolotlChat(axolotlEncrypted, origin, conversation, status,  checkedForDuplicates,query != null);
				} else {
					Message trial = null;
					for (Jid fallback : fallbacksBySourceId) {
						trial = parseAxolotlChat(axolotlEncrypted, fallback, conversation, status, checkedForDuplicates && fallbacksBySourceId.size() == 1, query != null);
						if (trial != null) {
							Log.d(Config.LOGTAG, account.getLogJid() + ": decoded muc message using fallback");
							origin = fallback;
							break;
						}
					}
					message = trial;
				}
				if (message == null) {
					if (query == null && extractChatState(mXmppConnectionService.find(account, counterpart.asBareJid()), isTypeGroupChat, packet)) {
						mXmppConnectionService.updateConversationUi();
					}
					if (query != null && status == Message.STATUS_SEND && remoteMsgId != null) {
						Message previouslySent = conversation.findSentMessageWithUuid(remoteMsgId);
						if (previouslySent != null && previouslySent.getServerMsgId() == null && serverMsgId != null) {
							previouslySent.setServerMsgId(serverMsgId);
							mXmppConnectionService.databaseBackend.updateMessage(previouslySent, false);
							Log.d(Config.LOGTAG, account.getLogJid() + ": encountered previously sent OMEMO message without serverId. updating...");
						}
					}
					return;
				}
				if (conversationMultiMode) {
					message.setTrueCounterpart(origin);
				}
			} else if (body == null && oobUrl != null) {
				message = new Message(conversation, oobUrl, Message.ENCRYPTION_NONE, status);
				message.setOob(true);
				if (CryptoHelper.isPgpEncryptedUrl(oobUrl)) {
					message.setEncryption(Message.ENCRYPTION_DECRYPTED);
				}
			} else {
				message = new Message(conversation, body, Message.ENCRYPTION_NONE, status);
			}

			message.setCounterpart(counterpart);
			message.setRemoteMsgId(remoteMsgId);

			//ALF AM-53
			if (timerElement != null && timerElement.getAttribute("seconds") != null) {
				String timerStr = timerElement.getAttribute("seconds");
				try {
					int timer = Integer.parseInt(timerStr);
					message.setTimer(timer);
					//conversation.setTimer(timer); //AM#9
					if (conversation.getTimer() != timer) {
						conversation.setTimer(timer);
						setTimerStatus(conversation, timerStr);
					}
				} catch(NumberFormatException nfe) {
					message.setTimer(Message.TIMER_NONE);
				}
			} else {
				if (conversation.getTimer() != Message.TIMER_NONE) { //AM#9
					message.setTimer(Message.TIMER_NONE);
					conversation.setTimer(Message.TIMER_NONE);
					setTimerStatus(conversation, null);
				}
			}

			message.setServerMsgId(serverMsgId);
			message.setCarbon(isCarbon);
			message.setTime(timestamp);
			if (body != null && body.equals(oobUrl)) {
				message.setOob(true);
				if (CryptoHelper.isPgpEncryptedUrl(oobUrl)) {
					message.setEncryption(Message.ENCRYPTION_DECRYPTED);
				}
			}
			message.markable = packet.hasChild("markable", "urn:xmpp:chat-markers:0");
			if (conversationMultiMode) {
				message.setMucUser(conversation.getMucOptions().findUserByFullJid(counterpart));
				final Jid fallback = conversation.getMucOptions().getTrueCounterpart(counterpart);
				Jid trueCounterpart;
				if (message.getEncryption() == Message.ENCRYPTION_AXOLOTL) {
					trueCounterpart = message.getTrueCounterpart();
				} else if (query != null && query.safeToExtractTrueCounterpart()) {
					trueCounterpart = getTrueCounterpart(mucUserElement, fallback);
				} else {
					trueCounterpart = fallback;
				}
				if (trueCounterpart != null && isTypeGroupChat) {
					if (trueCounterpart.asBareJid().equals(account.getJid().asBareJid())) {
						status = isTypeGroupChat ? Message.STATUS_SEND_RECEIVED : Message.STATUS_SEND;
					} else {
						status = Message.STATUS_RECEIVED;
						message.setCarbon(false);
					}
				}
				message.setStatus(status);
				message.setTrueCounterpart(trueCounterpart);
				if (!isTypeGroupChat) {
					message.setType(Message.TYPE_PRIVATE);
				}
			} else {
				updateLastseen(account, from);
			}

			if (replacementId != null && mXmppConnectionService.allowMessageCorrection()) {
				final Message replacedMessage = conversation.findMessageWithRemoteIdAndCounterpart(replacementId,
						counterpart,
						message.getStatus() == Message.STATUS_RECEIVED,
						message.isCarbon());
				if (replacedMessage != null) {
					final boolean fingerprintsMatch = replacedMessage.getFingerprint() == null
							|| replacedMessage.getFingerprint().equals(message.getFingerprint());
					final boolean trueCountersMatch = replacedMessage.getTrueCounterpart() != null
							&& replacedMessage.getTrueCounterpart().equals(message.getTrueCounterpart());
					final boolean mucUserMatches = query == null && replacedMessage.sameMucUser(message); //can not be checked when using mam
					final boolean duplicate = conversation.hasDuplicateMessage(message);
					if (fingerprintsMatch && (trueCountersMatch || !conversationMultiMode || mucUserMatches) && !duplicate) {
						//Log.d(Config.LOGTAG, "replaced message '" + replacedMessage.getBody() + "' with '" + message.getBody() + "'");
						synchronized (replacedMessage) {
							final String uuid = replacedMessage.getUuid();
							replacedMessage.setUuid(UUID.randomUUID().toString());
							replacedMessage.setBody(message.getBody());
							replacedMessage.putEdited(replacedMessage.getRemoteMsgId(), replacedMessage.getServerMsgId());
							replacedMessage.setRemoteMsgId(remoteMsgId);
							if (replacedMessage.getServerMsgId() == null || message.getServerMsgId() != null) {
								replacedMessage.setServerMsgId(message.getServerMsgId());
							}
							replacedMessage.setEncryption(message.getEncryption());

							//ALF AM-53
							replacedMessage.setTimer(message.getTimer());
							replacedMessage.setEndTime(message.getEndTime());

							if (replacedMessage.getStatus() == Message.STATUS_RECEIVED) {
								replacedMessage.markUnread();
							}
							extractChatState(mXmppConnectionService.find(account, counterpart.asBareJid()), isTypeGroupChat, packet);
							mXmppConnectionService.updateMessage(replacedMessage, uuid);
							if (mXmppConnectionService.confirmMessages()
									&& replacedMessage.getStatus() == Message.STATUS_RECEIVED
									&& (replacedMessage.trusted() || replacedMessage.getType() == Message.TYPE_PRIVATE)
									&& remoteMsgId != null
									&& !selfAddressed
									&& !isTypeGroupChat) {
								processMessageReceipts(account, packet, query);
							}
							/*if (replacedMessage.getEncryption() == Message.ENCRYPTION_PGP) {
								conversation.getAccount().getPgpDecryptionService().discard(replacedMessage);
								conversation.getAccount().getPgpDecryptionService().decrypt(replacedMessage, false);
							}*/
						}
						mXmppConnectionService.getNotificationService().updateNotification();
						return;
					} else {
						Log.d(Config.LOGTAG, account.getLogJid() + ": received message correction but verification didn't check out");
					}
				}
			}

			long deletionDate = mXmppConnectionService.getAutomaticMessageDeletionDate();
			if (deletionDate != 0 && message.getTimeSent() < deletionDate) {
				Log.d(Config.LOGTAG, account.getLogJid() + ": skipping message from " + message.getCounterpart().toString() + " because it was sent prior to our deletion date");
				return;
			}

			boolean checkForDuplicates = (isTypeGroupChat && packet.hasChild("delay", "urn:xmpp:delay"))
					|| message.getType() == Message.TYPE_PRIVATE
					|| message.getServerMsgId() != null
					|| (query == null && mXmppConnectionService.getMessageArchiveService().isCatchupInProgress(conversation));
			if (checkForDuplicates) {
				final Message duplicate = conversation.findDuplicateMessage(message);
				if (duplicate != null) {
					final boolean serverMsgIdUpdated;
					if (duplicate.getStatus() != Message.STATUS_RECEIVED
							&& duplicate.getUuid().equals(message.getRemoteMsgId())
							&& duplicate.getServerMsgId() == null
							&& message.getServerMsgId() != null) {
						duplicate.setServerMsgId(message.getServerMsgId());
						if (mXmppConnectionService.databaseBackend.updateMessage(duplicate, false)) {
							serverMsgIdUpdated = true;
						} else {
							serverMsgIdUpdated = false;
							Log.e(Config.LOGTAG,"failed to update message");
						}
					} else {
						serverMsgIdUpdated = false;
					}
					Log.d(Config.LOGTAG, "skipping duplicate message with " + message.getCounterpart() + ". serverMsgIdUpdated=" + Boolean.toString(serverMsgIdUpdated));
					return;
				}
			}

			if (query != null && query.getPagingOrder() == MessageArchiveService.PagingOrder.REVERSE) {
				conversation.prepend(query.getActualInThisQuery(), message);
			} else {
				conversation.add(message);
			}
			if (query != null) {
				query.incrementActualMessageCount();
			}

			if (query == null || query.isCatchup()) { //either no mam or catchup
				if (status == Message.STATUS_SEND || status == Message.STATUS_SEND_RECEIVED) {
					mXmppConnectionService.markRead(conversation);
					if (query == null) {
						activateGracePeriod(account);
					}
				} else {
					message.markUnread();
					notify = true;
				}
			}

			if (message.getEncryption() == Message.ENCRYPTION_PGP) {
				//notify = conversation.getAccount().getPgpDecryptionService().decrypt(message, notify);
			} else if (message.getEncryption() == Message.ENCRYPTION_AXOLOTL_NOT_FOR_THIS_DEVICE || message.getEncryption() == Message.ENCRYPTION_AXOLOTL_FAILED) {
				notify = false;
			}

			if (query == null) {
				extractChatState(mXmppConnectionService.find(account, counterpart.asBareJid()), isTypeGroupChat, packet);
				mXmppConnectionService.updateConversationUi();
			}

			if (mXmppConnectionService.confirmMessages()
					&& message.getStatus() == Message.STATUS_RECEIVED
					&& (message.trusted() || message.getType() == Message.TYPE_PRIVATE)
					&& remoteMsgId != null
					&& !selfAddressed
					&& !isTypeGroupChat) {
				processMessageReceipts(account, packet, query);
			}

			mXmppConnectionService.databaseBackend.createMessage(message);
			final HttpConnectionManager manager = this.mXmppConnectionService.getHttpConnectionManager();
			if (message.trusted() && message.treatAsDownloadable() && manager.getAutoAcceptFileSize() > 0) {
				manager.createNewDownloadConnection(message);
			} else if (notify) {
				if (query != null && query.isCatchup()) {
					mXmppConnectionService.getNotificationService().pushFromBacklog(message);
				} else {
					mXmppConnectionService.getNotificationService().push(message);
				}
			}
		} else if (!packet.hasChild("body")) { //no body

			final Conversation conversation = mXmppConnectionService.find(account, from.asBareJid());
			if (axolotlEncrypted != null) {
				Jid origin;
				if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI) {
					final Jid fallback = conversation.getMucOptions().getTrueCounterpart(counterpart);
					origin = getTrueCounterpart(query != null ? mucUserElement : null, fallback);
					if (origin == null) {
						Log.d(Config.LOGTAG, "omemo key transport message in anonymous conference received");
						return;
					}
				} else if (isTypeGroupChat) {
					return;
				} else {
					origin = from;
				}
				try {
					final XmppAxolotlMessage xmppAxolotlMessage = XmppAxolotlMessage.fromElement(axolotlEncrypted, origin.asBareJid());
					account.getAxolotlService().processReceivingKeyTransportMessage(xmppAxolotlMessage, query != null);
					Log.d(Config.LOGTAG, account.getLogJid() + ": omemo key transport message received"); // from " + origin);
				} catch (Exception e) {
					Log.d(Config.LOGTAG, account.getLogJid() + ": invalid omemo key transport message received "); // + e.getMessage());
					return;
				}
			}

			if (query == null && extractChatState(mXmppConnectionService.find(account, counterpart.asBareJid()), isTypeGroupChat, packet)) {
				mXmppConnectionService.updateConversationUi();
			}

			if (isTypeGroupChat) {
				if (packet.hasChild("subject")) {
					if (conversation != null && conversation.getMode() == Conversation.MODE_MULTI) {
						conversation.setHasMessagesLeftOnServer(conversation.countMessages() > 0);
						String subject = packet.findInternationalizedChildContent("subject");
						if (conversation.getMucOptions().setSubject(subject)) {
							mXmppConnectionService.updateConversation(conversation);
						}
						mXmppConnectionService.updateConversationUi();
						return;
					}
				}
			}
			if (conversation != null && mucUserElement != null && InvalidJid.hasValidFrom(packet) && from.isBareJid()) {
				for (Element child : mucUserElement.getChildren()) {
					if ("status".equals(child.getName())) {
						try {
							int code = Integer.parseInt(child.getAttribute("code"));
							if ((code >= 170 && code <= 174) || (code >= 102 && code <= 104)) {
								mXmppConnectionService.fetchConferenceConfiguration(conversation);
								break;
							}
						} catch (Exception e) {
							//ignored
						}
					} else if ("item".equals(child.getName())) {
						MucOptions.User user = AbstractParser.parseItem(conversation, child);
						Log.d(Config.LOGTAG, account.getLogJid() + ": changing affiliation for "
								+ Tools.logJid(user.getRealJid()) + " to " + user.getAffiliation() + " in "
								+ conversation.getLogJid());
						if (!user.realJidMatchesAccount()) {
							boolean isNew = conversation.getMucOptions().updateUser(user);
							mXmppConnectionService.getAvatarService().clear(conversation);
							mXmppConnectionService.updateMucRosterUi();
							mXmppConnectionService.updateConversationUi();
							Contact contact = user.getContact();
							if (!user.getAffiliation().ranks(MucOptions.Affiliation.MEMBER)) {
								Jid jid = user.getRealJid();
								List<Jid> cryptoTargets = conversation.getAcceptedCryptoTargets();
								if (cryptoTargets.remove(user.getRealJid())) {
									Log.d(Config.LOGTAG, account.getLogJid() + ": removed " + Tools.logJid(jid) + " from crypto targets " ); //+ conversation.getName()
									conversation.setAcceptedCryptoTargets(cryptoTargets);
									mXmppConnectionService.updateConversation(conversation);
								}
							} else if (isNew
									&& user.getRealJid() != null
									&& conversation.getMucOptions().isPrivateAndNonAnonymous()
									&& (contact == null || !contact.mutualPresenceSubscription())
									&& account.getAxolotlService().hasEmptyDeviceList(user.getRealJid())) {
								account.getAxolotlService().fetchDeviceIds(user.getRealJid());
							}
						}
					}
				}
			}
		}

		Element received = packet.findChild("received", "urn:xmpp:chat-markers:0");
		if (received == null) {
			received = packet.findChild("received", "urn:xmpp:receipts");
		}
		if (received != null) {
			String id = received.getAttribute("id");
			if (packet.fromAccount(account)) {
				if (query != null && id != null && packet.getTo() != null) {
					query.removePendingReceiptRequest(new ReceiptRequest(packet.getTo(), id));
				}
			} else {
				mXmppConnectionService.markMessage(account, from.asBareJid(), received.getAttribute("id"), Message.STATUS_SEND_RECEIVED);
			}
		}
		Element displayed = packet.findChild("displayed", "urn:xmpp:chat-markers:0");
		if (displayed != null) {
			final String id = displayed.getAttribute("id");
			final Jid sender = InvalidJid.getNullForInvalid(displayed.getAttributeAsJid("sender"));
			if (packet.fromAccount(account) && !selfAddressed) {
				dismissNotification(account, counterpart, query);
			} else if (isTypeGroupChat) {
				Conversation conversation = mXmppConnectionService.find(account, counterpart.asBareJid());
				if (conversation != null && id != null && sender != null) {
					Message message = conversation.findMessageWithRemoteId(id, sender);
					if (message != null) {
						final Jid fallback = conversation.getMucOptions().getTrueCounterpart(counterpart);
						final Jid trueJid = getTrueCounterpart((query != null && query.safeToExtractTrueCounterpart()) ? mucUserElement : null, fallback);
						final boolean trueJidMatchesAccount = account.getJid().asBareJid().equals(trueJid == null ? null : trueJid.asBareJid());
						if (trueJidMatchesAccount || conversation.getMucOptions().isSelf(counterpart)) {
							if (!message.isRead() && (query == null || query.isCatchup())) { //checking if message is unread fixes race conditions with reflections
								mXmppConnectionService.markRead(conversation);
							}
						} else if (!counterpart.isBareJid() && trueJid != null) {
							ReadByMarker readByMarker = ReadByMarker.from(counterpart, trueJid);
							if (message.addReadByMarker(readByMarker)) {
								Log.d(Config.LOGTAG, account.getLogJid() + ": added read by (" + Tools.logJid(readByMarker.getRealJid()) + ") to message");
								mXmppConnectionService.updateMessage(message, false);
							}
						}
					}
				}
			} else {
				final Message displayedMessage = mXmppConnectionService.markMessage(account, from.asBareJid(), id, Message.STATUS_SEND_DISPLAYED);
				Message message = displayedMessage == null ? null : displayedMessage.prev();
				while (message != null
						&& message.getStatus() == Message.STATUS_SEND_RECEIVED
						&& message.getTimeSent() < displayedMessage.getTimeSent()) {
					mXmppConnectionService.markMessage(message, Message.STATUS_SEND_DISPLAYED);
					message = message.prev();
				}
				if (displayedMessage != null && selfAddressed) {
					dismissNotification(account, counterpart, query);
				}
			}
		}

		Element event = original.findChild("event", "http://jabber.org/protocol/pubsub#event");
		if (event != null && InvalidJid.hasValidFrom(original)) {
			parseEvent(event, original.getFrom(), account);
		}

		final String nick = packet.findChildContent("nick", Namespace.NICK);
		if (nick != null && InvalidJid.hasValidFrom(original)) {
			Contact contact = account.getRoster().getContact(from);
			if (contact.setPresenceName(nick)) {
				mXmppConnectionService.getAvatarService().clear(contact);
			}
		}
	}

	//AM#9
	protected void setTimerStatus(Conversation conversation, String timerstring) {
		final String[] ctimers = mXmppConnectionService.getResources().getStringArray(R.array.timer_options_durations);
		final String[] ctimersStrs = mXmppConnectionService.getResources().getStringArray(R.array.timer_options_descriptions);
		int idx = -1;
		if (timerstring == null) {
			idx = 0;
		} else {
			for (int i = 0; i < ctimers.length; i++) {
				if (ctimers[i].equals(timerstring)) {
					idx = i;
				}
			}
		}

		String tstatus = "";
		if (idx >= 0) {
			tstatus = ctimersStrs[idx];
		} else { return;}

		String timerStatus = "Disappearing message time set to " + tstatus;
		Message disMessageStatus = Message.createStatusMessage(conversation, timerStatus);
		disMessageStatus.setTime(System.currentTimeMillis());
		disMessageStatus.setEndTime(Long.MAX_VALUE);
		disMessageStatus.setTimer(Message.TIMER_NONE);
		mXmppConnectionService.databaseBackend.createMessage(disMessageStatus);
		conversation.add(disMessageStatus);
	}

	private void dismissNotification(Account account, Jid counterpart, MessageArchiveService.Query query) {
		Conversation conversation = mXmppConnectionService.find(account, counterpart.asBareJid());
		if (conversation != null && (query == null || query.isCatchup())) {
			mXmppConnectionService.markRead(conversation); //TODO only mark messages read that are older than timestamp
		}
	}

	private void processMessageReceipts(Account account, MessagePacket packet, MessageArchiveService.Query query) {
		final boolean markable = packet.hasChild("markable", "urn:xmpp:chat-markers:0");
		final boolean request = packet.hasChild("request", "urn:xmpp:receipts");
		if (query == null) {
			final ArrayList<String> receiptsNamespaces = new ArrayList<>();
			if (markable) {
				receiptsNamespaces.add("urn:xmpp:chat-markers:0");
			}
			if (request) {
				receiptsNamespaces.add("urn:xmpp:receipts");
			}
			if (receiptsNamespaces.size() > 0) {
				MessagePacket receipt = mXmppConnectionService.getMessageGenerator().received(account,
						packet,
						receiptsNamespaces,
						packet.getType());
				mXmppConnectionService.sendMessagePacket(account, receipt);
			}
		} else if (query.isCatchup()) {
			if (request) {
				query.addPendingReceiptRequest(new ReceiptRequest(packet.getFrom(), packet.getId()));
			}
		}
	}

	private void activateGracePeriod(Account account) {
		long duration = mXmppConnectionService.getLongPreference("grace_period_length", R.integer.grace_period) * 1000;
		Log.d(Config.LOGTAG, account.getLogJid() + ": activating grace period till " + TIME_FORMAT.format(new Date(System.currentTimeMillis() + duration)));
		account.activateGracePeriod(duration);
	}

	private class Invite {
		final Jid jid;
		final String password;
		final Contact inviter;

		Invite(Jid jid, String password, Contact inviter) {
			this.jid = jid;
			this.password = password;
			this.inviter = inviter;
		}

		public boolean execute(Account account) {
			if (jid != null) {
				Conversation conversation = mXmppConnectionService.findOrCreateConversation(account, jid, true, false);
				if (!conversation.getMucOptions().online()) {
					conversation.getMucOptions().setPassword(password);
					mXmppConnectionService.databaseBackend.updateConversation(conversation);
					mXmppConnectionService.joinMuc(conversation, inviter != null && inviter.mutualPresenceSubscription());
					mXmppConnectionService.updateConversationUi();
				}
				return true;
			}
			return false;
		}
	}
}
