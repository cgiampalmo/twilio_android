package com.glaciersecurity.glaciermessenger.parser;

import com.glaciersecurity.glaciermessenger.utils.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


import com.glaciersecurity.glaciermessenger.Config;
//import com.glaciersecurity.glaciermessenger.crypto.PgpEngine;
import com.glaciersecurity.glaciermessenger.crypto.axolotl.AxolotlService;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Bookmark;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.Message;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.generator.IqGenerator;
import com.glaciersecurity.glaciermessenger.generator.PresenceGenerator;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.utils.XmppUri;
import com.glaciersecurity.glaciermessenger.xml.Element;
import com.glaciersecurity.glaciermessenger.xml.Namespace;
import com.glaciersecurity.glaciermessenger.xmpp.InvalidJid;
import com.glaciersecurity.glaciermessenger.xmpp.OnPresencePacketReceived;
import com.glaciersecurity.glaciermessenger.xmpp.pep.Avatar;
import com.glaciersecurity.glaciermessenger.xmpp.stanzas.PresencePacket;
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class PresenceParser extends AbstractParser implements
		OnPresencePacketReceived {

	public PresenceParser(XmppConnectionService service) {
		super(service);
	}

	public void parseConferencePresence(PresencePacket packet, Account account) {
		final Conversation conversation = packet.getFrom() == null ? null : mXmppConnectionService.find(account, packet.getFrom().asBareJid());
		if (conversation != null) {
			final MucOptions mucOptions = conversation.getMucOptions();
			boolean before = mucOptions.online();
			int count = mucOptions.getUserCount();
			final List<MucOptions.User> tileUserBefore = mucOptions.getUsers(5);
			processConferencePresence(packet, conversation);
			final List<MucOptions.User> tileUserAfter = mucOptions.getUsers(5);
			if (!tileUserAfter.equals(tileUserBefore)) {
				mXmppConnectionService.getAvatarService().clear(mucOptions);
			}
			if (before != mucOptions.online() || (mucOptions.online() && count != mucOptions.getUserCount())) {
				mXmppConnectionService.updateConversationUi();
			} else if (mucOptions.online()) {
				mXmppConnectionService.updateMucRosterUi();
			}
		}
	}

	private void processConferencePresence(PresencePacket packet, Conversation conversation) {
		MucOptions mucOptions = conversation.getMucOptions();
		final Jid jid = conversation.getAccount().getJid();
		final Jid from = packet.getFrom();
		if (!from.isBareJid()) {
			final String type = packet.getAttribute("type");
			final Element x = packet.findChild("x", "http://jabber.org/protocol/muc#user");
			Avatar avatar = Avatar.parsePresence(packet.findChild("x", "vcard-temp:x:update"));
			final List<String> codes = getStatusCodes(x);
			if (type == null) {
				if (x != null) {
					Element item = x.findChild("item");
					if (item != null && !from.isBareJid()) {
						mucOptions.setError(MucOptions.Error.NONE);
						MucOptions.User user = parseItem(conversation, item, from);
						if (codes.contains(MucOptions.STATUS_CODE_SELF_PRESENCE) || (codes.contains(MucOptions.STATUS_CODE_ROOM_CREATED) && jid.equals(InvalidJid.getNullForInvalid(item.getAttributeAsJid("jid"))))) {
							if (mucOptions.setOnline()) {
								mXmppConnectionService.getAvatarService().clear(mucOptions);
							}
							if (mucOptions.setSelf(user)) {
								Log.d(Config.LOGTAG,"role or affiliation changed");
								mXmppConnectionService.databaseBackend.updateConversation(conversation);
							}

							//mXmppConnectionService.persistSelfNick(user); //AM-642
							invokeRenameListener(mucOptions, true);
						}
						boolean isNew = mucOptions.updateUser(user);
						final AxolotlService axolotlService = conversation.getAccount().getAxolotlService();
						Contact contact = user.getContact();
						if (isNew
								&& user.getRealJid() != null
								&& mucOptions.isPrivateAndNonAnonymous()
								&& (contact == null || !contact.mutualPresenceSubscription())
								&& axolotlService.hasEmptyDeviceList(user.getRealJid())) {
							axolotlService.fetchDeviceIds(user.getRealJid());
						}
						if (codes.contains(MucOptions.STATUS_CODE_ROOM_CREATED) && mucOptions.autoPushConfiguration()) {
							Log.d(Config.LOGTAG,mucOptions.getAccount().getLogJid()
									+": room '"
									+mucOptions.getConversation().getJid().asBareJid()
									+"' created. pushing default configuration");
							mXmppConnectionService.pushConferenceConfiguration(mucOptions.getConversation(),
									IqGenerator.defaultGroupChatConfiguration(),
									null);
						}
						/*if (mXmppConnectionService.getPgpEngine() != null) {
							Element signed = packet.findChild("x", "jabber:x:signed");
							if (signed != null) {
								Element status = packet.findChild("status");
								String msg = status == null ? "" : status.getContent();
								long keyId = mXmppConnectionService.getPgpEngine().fetchKeyId(mucOptions.getAccount(), msg, signed.getContent());
								if (keyId != 0) {
									user.setPgpKeyId(keyId);
								}
							}
						}*/
						if (avatar != null) {
							avatar.owner = from;
							if (mXmppConnectionService.getFileBackend().isAvatarCached(avatar)) {
								if (user.setAvatar(avatar)) {
									mXmppConnectionService.getAvatarService().clear(user);
								}
								if (user.getRealJid() != null) {
									Contact c = conversation.getAccount().getRoster().getContact(user.getRealJid());
									if (c.setAvatar(avatar)) {
										mXmppConnectionService.syncRoster(conversation.getAccount());
										mXmppConnectionService.getAvatarService().clear(c);
										mXmppConnectionService.updateRosterUi();
									}
								}
							} else if (mXmppConnectionService.isDataSaverDisabled()) {
								mXmppConnectionService.fetchAvatar(mucOptions.getAccount(), avatar);
							}
						}
					}
				}
			} else if (type.equals("unavailable")) {
				final boolean fullJidMatches = from.equals(mucOptions.getSelf().getFullJid());
				if (x.hasChild("destroy") && fullJidMatches) {
					Element destroy = x.findChild("destroy");
					final Jid alternate = destroy == null ? null : InvalidJid.getNullForInvalid(destroy.getAttributeAsJid("jid"));
					mucOptions.setError(MucOptions.Error.DESTROYED);
					if (alternate != null) {
						Log.d(Config.LOGTAG, conversation.getAccount().getLogJid() + ": muc destroyed. alternate location " + alternate);
					}
				} else if (codes.contains(MucOptions.STATUS_CODE_SHUTDOWN) && fullJidMatches) {
					mucOptions.setError(MucOptions.Error.SHUTDOWN);
				} else if (codes.contains(MucOptions.STATUS_CODE_SELF_PRESENCE)) {
					if (codes.contains(MucOptions.STATUS_CODE_KICKED)) {
						mucOptions.setError(MucOptions.Error.KICKED);
					} else if (codes.contains(MucOptions.STATUS_CODE_BANNED)) {
						mucOptions.setError(MucOptions.Error.BANNED);
					} else if (codes.contains(MucOptions.STATUS_CODE_LOST_MEMBERSHIP)) {
						mucOptions.setError(MucOptions.Error.MEMBERS_ONLY);
					} else if (codes.contains(MucOptions.STATUS_CODE_AFFILIATION_CHANGE)) {
						mucOptions.setError(MucOptions.Error.MEMBERS_ONLY);
					} else if (codes.contains(MucOptions.STATUS_CODE_SHUTDOWN)) {
						mucOptions.setError(MucOptions.Error.SHUTDOWN);
					} else if (!codes.contains(MucOptions.STATUS_CODE_CHANGED_NICK)) {
						mucOptions.setError(MucOptions.Error.UNKNOWN);
						Log.d(Config.LOGTAG, "unknown error in conference "); // + packet);
					}
				} else if (codes.contains(MucOptions.STATUS_CODE_SHUTDOWN)) { //ALF AM-78
					mucOptions.setError(MucOptions.Error.SHUTDOWN);
					mXmppConnectionService.archiveConversation(conversation);
					mXmppConnectionService.updateConversationUi();
				} else if (!from.isBareJid()){
					Element item = x.findChild("item");
					if (item != null) {
						mucOptions.updateUser(parseItem(conversation, item, from));
					}
					MucOptions.User user = mucOptions.deleteUser(from);
					if (user != null) {
						mXmppConnectionService.getAvatarService().clear(user);
					}
				}
			} else if (type.equals("error")) {
				final Element error = packet.findChild("error");
				if (error == null) {
					return;
				}
				if (error.hasChild("conflict")) {
					if (mucOptions.online()) {
						invokeRenameListener(mucOptions, false);
					} else {
						mucOptions.setError(MucOptions.Error.NICK_IN_USE);
					}
				} else if (error.hasChild("not-authorized")) {
					mucOptions.setError(MucOptions.Error.PASSWORD_REQUIRED);
				} else if (error.hasChild("forbidden")) {
					mucOptions.setError(MucOptions.Error.BANNED);
				} else if (error.hasChild("registration-required")) {
					mucOptions.setError(MucOptions.Error.MEMBERS_ONLY);
				} else if (error != null && error.hasChild("item-not-found")) { //ALF AM-78
					mucOptions.setError(MucOptions.Error.SHUTDOWN);
					mXmppConnectionService.archiveConversation(conversation);
					mXmppConnectionService.updateConversationUi();
				} else if (error.hasChild("resource-constraint")) {
					mucOptions.setError(MucOptions.Error.RESOURCE_CONSTRAINT);
				} else if (error.hasChild("remote-server-timeout")) {
					mucOptions.setError(MucOptions.Error.REMOTE_SERVER_TIMEOUT);
				} else if (error.hasChild("gone")) {
					final String gone = error.findChildContent("gone");
					final Jid alternate;
					if (gone != null) {
						final XmppUri xmppUri = new XmppUri(gone);
						if (xmppUri.isJidValid()) {
							alternate = xmppUri.getJid();
						} else {
							alternate = null;
						}
					} else {
						alternate = null;
					}
					mucOptions.setError(MucOptions.Error.DESTROYED);
					if (alternate != null) {
						Log.d(Config.LOGTAG, conversation.getAccount().getLogJid() + ": muc destroyed. alternate location " + alternate);
					}
				} else {
					final String text = error.findChildContent("text");
					if (text != null && text.contains("attribute 'to'")) {
						if (mucOptions.online()) {
							invokeRenameListener(mucOptions, false);
						} else {
							mucOptions.setError(MucOptions.Error.INVALID_NICK);
						}
					} else {
						mucOptions.setError(MucOptions.Error.UNKNOWN);
						Log.d(Config.LOGTAG, "unknown error in conference "); // + packet);
					}
				}
			}
		}
	}

	private static void invokeRenameListener(final MucOptions options, boolean success) {
		if (options.onRenameListener != null) {
			if (success) {
				options.onRenameListener.onSuccess();
			} else {
				options.onRenameListener.onFailure();
			}
			options.onRenameListener = null;
		}
	}

	private static List<String> getStatusCodes(Element x) {
		List<String> codes = new ArrayList<>();
		if (x != null) {
			for (Element child : x.getChildren()) {
				if (child.getName().equals("status")) {
					String code = child.getAttribute("code");
					if (code != null) {
						codes.add(code);
					}
				}
			}
		}
		return codes;
	}

	private void parseContactPresence(final PresencePacket packet, final Account account) {
		final PresenceGenerator mPresenceGenerator = mXmppConnectionService.getPresenceGenerator();
		final Jid from = packet.getFrom();
		if (from == null) {
			return;
		}
		//AM-642 adjusted above, and below
		if (from.asBareJid().equals(account.getJid().asBareJid())) {
			Element xel = packet.findChild("x", "vcard-temp:x:update");
			String displayname = xel == null ? null : xel.findChildContent("displayname");
			if (displayname != null && !displayname.equals(account.getDisplayName())) {
				account.setDisplayName(displayname);
				mXmppConnectionService.setRoomsNickname(displayname, false, null);
				mXmppConnectionService.databaseBackend.updateAccount(account);
				mXmppConnectionService.updateConversationUi();
				mXmppConnectionService.updateAccountUi();
			}
			return;
		}

		final String type = packet.getAttribute("type");
		final Contact contact = account.getRoster().getContact(from);
		if (type == null) {
			final String resource = from.isBareJid() ? "" : from.getResource();

			//ALF AM-48
			Element xel = packet.findChild("x", "vcard-temp:x:update");
			String displayname = xel == null ? null : xel.findChildContent("displayname");
			if (displayname != null) {
				contact.setServerName(displayname);
			}

			Avatar avatar = Avatar.parsePresence(packet.findChild("x", "vcard-temp:x:update"));
			if (avatar != null && (!contact.isSelf() || account.getAvatar() == null)) {
				avatar.owner = from.asBareJid();
				if (mXmppConnectionService.getFileBackend().isAvatarCached(avatar)) {
					if (avatar.owner.equals(account.getJid().asBareJid())) {
						account.setAvatar(avatar.getFilename());
						mXmppConnectionService.databaseBackend.updateAccount(account);
						mXmppConnectionService.getAvatarService().clear(account);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateAccountUi();
					} else if (contact.setAvatar(avatar)) {
						mXmppConnectionService.syncRoster(account);
						mXmppConnectionService.getAvatarService().clear(contact);
						mXmppConnectionService.updateConversationUi();
						mXmppConnectionService.updateRosterUi();
					}
				} else if (mXmppConnectionService.isDataSaverDisabled()){
					mXmppConnectionService.fetchAvatar(account, avatar);
				}
			} else if (displayname != null) { //ALF AM-139 or setNick
				mXmppConnectionService.updateAccountUi();
			}

			if (mXmppConnectionService.isMuc(account, from)) {
				return;
			}

			int sizeBefore = contact.getPresences().size();

			final String show = packet.findChildContent("show");
			final Element caps = packet.findChild("c", "http://jabber.org/protocol/caps");
			final String message = packet.findChildContent("status");
			final Presence presence = Presence.parse(show, caps, message);
			contact.updatePresence(resource, presence);
			if (presence.hasCaps()) {
				mXmppConnectionService.fetchCaps(account, from, presence);
			}

			final Element idle = packet.findChild("idle", Namespace.IDLE);
			if (idle != null) {
				try {
					final String since = idle.getAttribute("since");
					contact.setLastseen(AbstractParser.parseTimestamp(since));
					contact.flagInactive();
				} catch (Throwable throwable) {
					if (contact.setLastseen(AbstractParser.parseTimestamp(packet))) {
						contact.flagActive();
					}
				}
			} else {
				if (contact.setLastseen(AbstractParser.parseTimestamp(packet))) {
					contact.flagActive();
				}
			}

			/*PgpEngine pgp = mXmppConnectionService.getPgpEngine();
			Element x = packet.findChild("x", "jabber:x:signed");
			if (pgp != null && x != null) {
				Element status = packet.findChild("status");
				String msg = status != null ? status.getContent() : "";
				if (contact.setPgpKeyId(pgp.fetchKeyId(account, msg, x.getContent()))) {
					mXmppConnectionService.syncRoster(account);
				}
			}*/
			boolean online = sizeBefore < contact.getPresences().size();
			mXmppConnectionService.onContactStatusChanged.onContactStatusChanged(contact, online);
		} else if (type.equals("unavailable")) {
			if (contact.setLastseen(AbstractParser.parseTimestamp(packet,0L,true))) {
				contact.flagInactive();
			}
			if (from.isBareJid()) {
				contact.clearPresences();
			} else {
				contact.removePresence(from.getResource());
			}
			if (contact.getShownStatus() == Presence.Status.OFFLINE) {
				contact.flagInactive();
			}
			mXmppConnectionService.onContactStatusChanged.onContactStatusChanged(contact, false);
		} else if (type.equals("subscribe")) {
			if (contact.setPresenceName(packet.findChildContent("nick", Namespace.NICK))) {
				mXmppConnectionService.getAvatarService().clear(contact);
			}
			if (contact.getOption(Contact.Options.PREEMPTIVE_GRANT)) {
				mXmppConnectionService.sendPresencePacket(account,
						mPresenceGenerator.sendPresenceUpdatesTo(contact));
			} else {
				contact.setOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST);
				final Conversation conversation = mXmppConnectionService.findOrCreateConversation(
						account, contact.getJid().asBareJid(), false, false);
				final String statusMessage = packet.findChildContent("status");
				if (statusMessage != null
						&& !statusMessage.isEmpty()
						&& conversation.countMessages() == 0) {
					conversation.add(new Message(
							conversation,
							statusMessage,
							Message.ENCRYPTION_NONE,
							Message.STATUS_RECEIVED
					));
				}
			}
		}
		mXmppConnectionService.updateRosterUi();
	}

	@Override
	public void onPresencePacketReceived(Account account, PresencePacket packet) {
		if (packet.hasChild("x", "http://jabber.org/protocol/muc#user")) {
			this.parseConferencePresence(packet, account);
		} else if (packet.hasChild("x", "http://jabber.org/protocol/muc")) {
			this.parseConferencePresence(packet, account);
			//CMG AM-248 group chat creation error
		} else if ("error".equals(packet.getAttribute("type")) && mXmppConnectionService.isMuc(account, packet.getFrom())) {
            Element err = packet.findChild("error");
            if (err != null && err.hasChild("item-not-found", "urn:ietf:params:xml:ns:xmpp-stanzas")) {
                return;
            }
            this.parseConferencePresence(packet, account);
        } else {
			this.parseContactPresence(packet, account);
		}
	}
}
