package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.app.PendingIntent;
import android.content.IntentSender;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.openintents.openpgp.util.OpenPgpUtils;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.crypto.PgpEngine;
import com.glaciersecurity.glaciermessenger.databinding.ContactBinding;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.ConferenceDetailsActivity;
import com.glaciersecurity.glaciermessenger.ui.XmppActivity;
import com.glaciersecurity.glaciermessenger.ui.util.AvatarWorkerTask;
import com.glaciersecurity.glaciermessenger.ui.util.MucDetailsContextMenuHelper;
import rocks.xmpp.addr.Jid;

public class UserAdapter extends ListAdapter<MucOptions.User, UserAdapter.ViewHolder> implements View.OnCreateContextMenuListener {

    static final DiffUtil.ItemCallback<MucOptions.User> DIFF = new DiffUtil.ItemCallback<MucOptions.User>() {
        @Override
        public boolean areItemsTheSame(@NonNull MucOptions.User a, @NonNull MucOptions.User b) {
            final Jid fullA = a.getFullJid();
            final Jid fullB = b.getFullJid();
            final Jid realA = a.getRealJid();
            final Jid realB = b.getRealJid();
            if (fullA != null && fullB != null) {
                return fullA.equals(fullB);
            } else if (realA != null && realB != null) {
                return realA.equals(realB);
            } else {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull MucOptions.User a, @NonNull MucOptions.User b) {
            return a.equals(b);
        }
    };
    private final boolean advancedMode;
    private MucOptions.User selectedUser = null;

    public UserAdapter(final boolean advancedMode) {
        super(DIFF);
        this.advancedMode = advancedMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        return new ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.contact, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        final MucOptions.User user = getItem(position);
        AvatarWorkerTask.loadAvatar(user, viewHolder.binding.contactPhoto, R.dimen.avatar);
        viewHolder.binding.getRoot().setOnClickListener(v -> {
            final XmppActivity activity = XmppActivity.find(v);
            if (activity != null) {
                //CMG AM-302
                activity.switchToContactDetails(user.getContact(), null);
                // activity.highlightInMuc(user.getConversation(), user.getName());
            }
        });
        viewHolder.binding.getRoot().setTag(user);
        viewHolder.binding.getRoot().setOnCreateContextMenuListener(this);
        viewHolder.binding.getRoot().setOnLongClickListener(v -> {
            selectedUser = user;
            return false;
        });
        final String name = user.getName();
        final Contact contact = user.getContact();
        if (contact != null) {
            final String displayName = contact.getDisplayName();
            viewHolder.binding.contactDisplayName.setText(displayName);
            if (name != null && !name.equals(displayName)) {
                viewHolder.binding.contactJid.setText(String.format("%s \u2022 %s", name, ConferenceDetailsActivity.getStatus(viewHolder.binding.getRoot().getContext(), user, advancedMode)));
            } else {
                viewHolder.binding.contactJid.setText(ConferenceDetailsActivity.getStatus(viewHolder.binding.getRoot().getContext(), user, advancedMode));
            }
        } else {
            viewHolder.binding.contactDisplayName.setText(name == null ? "" : name);
            viewHolder.binding.contactJid.setText(ConferenceDetailsActivity.getStatus(viewHolder.binding.getRoot().getContext(), user, advancedMode));
        }
        if (advancedMode && user.getPgpKeyId() != 0) {
            viewHolder.binding.key.setVisibility(View.VISIBLE);
            viewHolder.binding.key.setOnClickListener(v -> {
                final XmppActivity activity = XmppActivity.find(v);
                final XmppConnectionService service = activity == null ? null : activity.xmppConnectionService;
                final PgpEngine pgpEngine = service == null ? null : service.getPgpEngine();
                if (pgpEngine != null) {
                    PendingIntent intent = pgpEngine.getIntentForKey(user.getPgpKeyId());
                    if (intent != null) {
                        try {
                            activity.startIntentSenderForResult(intent.getIntentSender(), 0, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException ignored) {

                        }
                    }
                }
            });
            viewHolder.binding.key.setText(OpenPgpUtils.convertKeyIdToHex(user.getPgpKeyId()));
        }


    }

    public MucOptions.User getSelectedUser() {
        return selectedUser;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MucDetailsContextMenuHelper.onCreateContextMenu(menu,v);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ContactBinding binding;

        private ViewHolder(ContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
