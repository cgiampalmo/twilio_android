package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.ActivityMucUsersBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
import com.glaciersecurity.glaciermessenger.entities.MucOptions;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.adapter.UserAdapter;
import com.glaciersecurity.glaciermessenger.ui.util.MucDetailsContextMenuHelper;
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

public class MucUsersActivity extends XmppActivity implements XmppConnectionService.OnMucRosterUpdate, XmppConnectionService.OnAffiliationChanged, MenuItem.OnActionExpandListener, TextWatcher {

    private UserAdapter userAdapter;

    private Conversation mConversation = null;

    private EditText mSearchEditText;

    private ArrayList<MucOptions.User> allUsers = new ArrayList<>();

    @Override
    protected void refreshUiReal() {
    }

    @Override
    void onBackendConnected() {
        final Intent intent = getIntent();
        final String uuid = intent == null ? null : intent.getStringExtra("uuid");
        if (uuid != null) {
            mConversation = xmppConnectionService.findConversationByUuid(uuid);
        }
        loadAndSubmitUsers();
    }

    private void loadAndSubmitUsers() {
        if (mConversation != null) {
            ArrayList<MucOptions.User> users = mConversation.getMucOptions().getUsers();
            Collections.sort(users);
            userAdapter.submitList(users);
            allUsers = mConversation.getMucOptions().getUsers();
            //AM-377
            allUsers.add(mConversation.getMucOptions().getSelf());
            Collections.sort(allUsers);
            submitFilteredList(mSearchEditText != null ? mSearchEditText.getText().toString() : null);
        }
    }

    private void submitFilteredList(String search) {
        if (TextUtils.isEmpty(search)) {
            userAdapter.submitList(allUsers);
        } else {
            final String needle = search.toLowerCase(Locale.getDefault());
            ArrayList<MucOptions.User> filtered = new ArrayList<>();
            for(MucOptions.User user : allUsers) {
                final String name = user.getName();
                final Contact contact = user.getContact();
                if (contact != null) {
                    if (name != null && name.toLowerCase(Locale.getDefault()).contains(needle) || contact != null && contact.getDisplayName().toLowerCase(Locale.getDefault()).contains(needle)) {
                        filtered.add(user);
                    }
                }

            }
//            //AM-377
            MucOptions.User selfUser = mConversation.getMucOptions().getSelf();
            final String name = selfUser.getName();
            final Account account = selfUser.getAccount();
            if (name != null && name.toLowerCase(Locale.getDefault()).contains(needle) || account != null && account.getDisplayName().toLowerCase(Locale.getDefault()).contains(needle)) {
                filtered.add(mConversation.getMucOptions().getSelf());
            }
            userAdapter.submitList(filtered);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!MucDetailsContextMenuHelper.onContextItemSelected(item, userAdapter.getSelectedUser(), this)) {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMucUsersBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_muc_users);
        setSupportActionBar((Toolbar) binding.toolbar);
        configureActionBar(getSupportActionBar(), true);
        this.userAdapter = new UserAdapter(getPreferences().getBoolean("advanced_muc_mode", false));
        binding.list.setAdapter(this.userAdapter);

    }


    @Override
    public void onMucRosterUpdate() {
        loadAndSubmitUsers();
    }

    private void displayToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAffiliationChangedSuccessful(Jid jid) {
        loadAndSubmitUsers(); //AM-573
    }

    @Override
    public void onAffiliationChangeFailed(Jid jid, int resId) {
        displayToast(getString(resId, jid.asBareJid().toString()));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.muc_users_activity, menu);
        final MenuItem menuSearchView = menu.findItem(R.id.action_search);
        final View mSearchView = menuSearchView.getActionView();
        mSearchEditText = mSearchView.findViewById(R.id.search_field);
        mSearchEditText.addTextChangedListener(this);
        mSearchEditText.setHint(R.string.search_participants);
        menuSearchView.setOnActionExpandListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mSearchEditText.post(() -> {
            mSearchEditText.requestFocus();
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mSearchEditText, InputMethodManager.SHOW_IMPLICIT);
        });
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        mSearchEditText.setText("");
        submitFilteredList("");
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        submitFilteredList(s.toString());
    }
}