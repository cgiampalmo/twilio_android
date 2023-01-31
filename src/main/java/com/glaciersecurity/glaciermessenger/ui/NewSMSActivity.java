package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.IdRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
public class NewSMSActivity extends XmppActivity implements OnSMSConversationClickListener, ConnectivityReceiver.ConnectivityReceiverListener{
    private String identity, convSid, Convtoken;
    private EditText writeMessageEditText,phoneNumber;
    private Context mContext = this;
    Toolbar toolbar;
    FilterAdapter adapter;
    RecyclerView recyclerView;
    ConversationModel cModel;
    Map<String, String> convContList;
    private ConnectivityReceiver connectivityReceiver;
    protected LinearLayout offlineLayout;

    @Override
    protected void refreshUiReal() {
    }

    @Override
    protected void onBackendConnected() {
        updateOfflineStatusBar();

    }
    protected void onStart() {
        super.onStart();
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop () {
        unregisterReceiver(connectivityReceiver);
        super.onStop();
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_sms);
        setTitle("New SMS ");
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        cModel = (ConversationModel) getApplicationContext();
        convContList = cModel.getContConv();
        ArrayList aList = new ArrayList();
        if(convContList.size() > 0) {
            for (Map.Entry<String, String> numList : convContList.entrySet()) {
                aList.add(numList.getKey());
            }
        }
        Log.d("Glacier","NewSMS aList "+aList+ aList.size());
        recyclerView = findViewById(R.id.choose_conversation_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FilterAdapter((OnSMSConversationClickListener) this,aList);
        recyclerView.setAdapter(adapter);

        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        if (getIntent().hasExtra("conv_sid")) {
            convSid = getIntent().getExtras().getString("conv_sid");
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        writeMessageEditText = findViewById(R.id.edit_gchat_message);
        phoneNumber = findViewById(R.id.edit_gchat_number);
        phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d("Glacier","onTextChanged "+charSequence);
                adapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        ImageView sendChatMessageButton = findViewById(R.id.button_submit_phonenumber);
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String toNumber = phoneNumber.getText().toString().replace("+1","").replace("(","").replace(")","").replace(" ","").replace("-","");
                if(toNumber.length() > 9)
                    OnSMSConversationClick("", toNumber);
                else
                    Toast.makeText(NewSMSActivity.this, "Please enter valid number", Toast.LENGTH_SHORT).show();
            }
        });
        this.offlineLayout = findViewById(R.id.offline_layout);
        this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
        connectivityReceiver = new ConnectivityReceiver(this);
        updateOfflineStatusBar();
    }
    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity).putExtra("token",Convtoken));
    }

    @Override
    public void OnSMSConversationClick(String connv_sid, String conv_name) {
        if(convContList.size() > 0){
            String sid = convContList.get(conv_name);
            Log.d("Glacier","OnSMSConversationClick sid "+sid);
            Intent intent = new Intent(this,smsConvActivity.class);
            if(sid != null && !(sid.trim().equals("No sid"))){
                startActivity(intent.putExtra("conv_sid",sid).putExtra("identity",identity).putExtra("conversationToken", Convtoken).putExtra("title",conv_name).putExtra("title",conv_name));
            }else{
                startActivity(intent.putExtra("conv_sid",conv_name).putExtra("identity",identity).putExtra("conversationToken", Convtoken).putExtra("title",conv_name).putExtra("title",conv_name));
            }
        }
    }
    private View.OnClickListener mRefreshNetworkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView networkStatus = findViewById(R.id.network_status);
            networkStatus.setCompoundDrawables(null, null, null, null);
            String previousNetworkState = networkStatus.getText().toString();
            final Account account = xmppConnectionService.getAccounts().get(0);
            if (account != null) {
                // previousNetworkState: ie what string is displayed currently in the offline status bar
                if (previousNetworkState != null) {

				    /*
				     Case 1a. PRESENCE -> OFFLINE ) "_____: tap to Reconnect"
				     -> refresh to "Attempting to Connect"
				     -> presence is offline, need to reenable account
				     -> change presence to online
				      */
                    if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_enable))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_connection));
                        if (account.getPresenceStatus().equals(Presence.Status.OFFLINE)){
                            xmppConnectionService.enableAccount(account);
                        }
                        PresenceTemplate template = new PresenceTemplate(Presence.Status.ONLINE, account.getPresenceStatusMessage());
                        //if (account.getPgpId() != 0 && hasPgp()) {
                        //	generateSignature(null, template);
                        //} else {
                        xmppConnectionService.changeStatus(account, template, null);
                        //}
                    }
					/*
				     Case 1b. PRESENCE) "_____: tap to set to Available"
				     -> refresh to "Changing status to Available"
				     -> if was offline need to reenable account
				     -> change presence to online
				      */
                    else if (previousNetworkState.contains(getResources().getString(R.string.status_tap_to_available))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_status));
                        changePresence(account);

                     /*
				     Case 2. ACCOUNT) "Disconnected: tap to connect"
				     -> refresh to "Attempting to Connect"
				     -> toggle account connection(ie what used to be manage accounts toggle)
				      */
                    } else if (previousNetworkState.contains(getResources().getString(R.string.disconnect_tap_to_connect))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_connection));
                        if (!(account.getStatus().equals(Account.State.CONNECTING) || account.getStatus().equals(Account.State.ONLINE))){
                            xmppConnectionService.enableAccount(account);
                        }
                     /*
				     Case 2. NETWORK) "No internet connection"
				     -> refresh to "Checking for signal"
				     -> ???
				      */
                    } else if (previousNetworkState.contains(getResources().getString(R.string.status_no_network))) {
                        networkStatus.setText(getResources().getString(R.string.refreshing_network));
                        xmppConnectionService.enableAccount(account);
                    }
                } else {
                    // should not reach here... Offline status message state should be defined in one of the above cases
                    networkStatus.setText(getResources().getString(R.string.refreshing_connection));
                }

                updateOfflineStatusBar();
            }

        }
    };
    private void runStatus(String str, boolean isVisible, boolean withRefresh){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isVisible){
                    offlineLayout.setVisibility(View.VISIBLE);
                } else {
                    offlineLayout.setVisibility(View.GONE);
                }
                reconfigureOfflineText(str, withRefresh);
            }
        }, 1000);
    }
    private void reconfigureOfflineText(String str, boolean withRefresh) {
        if (offlineLayout.isShown()) {
            TextView networkStatus = findViewById(R.id.network_status);
            if (networkStatus != null) {
                networkStatus.setText(str);
                if (withRefresh) {
                    Drawable refreshIcon =
                            ContextCompat.getDrawable(this, R.drawable.ic_refresh_black_24dp);
                    networkStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(refreshIcon, null, null, null);
                } else {
                    networkStatus.setCompoundDrawables(null, null, null, null);
                }
            }
        }
    }
    protected void updateOfflineStatusBar(){
        if (ConnectivityReceiver.isConnected(this)) {
            if (xmppConnectionService != null  && !xmppConnectionService.getAccounts().isEmpty()){
                final Account account = xmppConnectionService.getAccounts().get(0);
                Account.State accountStatus = account.getStatus();
                Presence.Status presenceStatus = account.getPresenceStatus();
                if (presenceStatus.equals(Presence.Status.OFFLINE)){
                    runStatus( getResources().getString(R.string.status_tap_to_enable) ,true, true);
                    Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_enable));
                } else if (!presenceStatus.equals(Presence.Status.ONLINE)){
                    runStatus( presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available) ,true, true);
                    Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + presenceStatus.toDisplayString()+ getResources().getString(R.string.status_tap_to_available));
                } else {
                    if (accountStatus == Account.State.ONLINE ) {
                        runStatus("", false, false);
                    } else if (accountStatus == Account.State.CONNECTING) {
                        runStatus(getResources().getString(R.string.connecting),true, false);
                        Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + getResources().getString(accountStatus.getReadableId()));
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                updateOfflineStatusBar();
                            }
                        },1000);
                    } else {
                        runStatus(getResources().getString(R.string.disconnect_tap_to_connect),true, true);
                        Log.w(Config.LOGTAG ,"updateOfflineStatusBar " + getResources().getString(accountStatus.getReadableId()));
                    }
                }
            }
        } else {
            runStatus(getResources().getString(R.string.status_no_network), true, true);
            Log.w(Config.LOGTAG ,"updateOfflineStatusBar disconnected from network");

        }
    }
    protected void changePresence(Account fragAccount) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        final DialogPresenceBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_presence, null, false);

        String current = fragAccount.getPresenceStatusMessage();
        if (current != null && !current.trim().isEmpty()) {
            binding.statusMessage.append(current);
        }
        xmppConnectionService.setAvailabilityRadioButton(fragAccount.getPresenceStatus(), binding);
        xmppConnectionService.setStatusMessageRadioButton(fragAccount.getPresenceStatusMessage(), binding);

        binding.clearPrefs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.statuses.clearCheck();
                binding.statusMessage.setText("");
            }
        });
        binding.statuses.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch(checkedId){
                    case R.id.in_meeting:
                        binding.statusMessage.setText(Presence.StatusMessage.IN_MEETING.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.on_travel:
                        binding.statusMessage.setText(Presence.StatusMessage.ON_TRAVEL.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.out_sick:
                        binding.statusMessage.setText(Presence.StatusMessage.OUT_SICK.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.vacation:
                        binding.statusMessage.setText(Presence.StatusMessage.VACATION.toShowString());
                        binding.statusMessage.setEnabled(false);
                        break;
                    case R.id.custom:
                        binding.statusMessage.setEnabled(true);
                        break;
                    default:
                        binding.statusMessage.setEnabled(false);
                        break;
                }
            }
        });

        builder.setTitle(R.string.edit_status_message_title);
        builder.setView(binding.getRoot());
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            PresenceTemplate template = new PresenceTemplate(getAvailabilityRadioButton(binding), binding.statusMessage.getText().toString().trim());
            //CMG AM-218
            //if (mAccount.getPgpId() != 0 && hasPgp()) {
            //	generateSignature(null, template);
            //} else {
            xmppConnectionService.changeStatus(fragAccount, template, null);
            //}
            if (template.getStatus().equals(Presence.Status.OFFLINE)){
                xmppConnectionService.disableAccount(fragAccount);
            } else {
                if (!template.getStatus().equals(Presence.Status.OFFLINE) && fragAccount.getStatus().equals(Account.State.DISABLED)){
                    xmppConnectionService.enableAccount(fragAccount);
                }
            }
            updateOfflineStatusBar();

        });
        builder.create().show();
    }
    private static Presence.Status getAvailabilityRadioButton(DialogPresenceBinding binding) {
        if (binding.dnd.isChecked()) {
            return Presence.Status.DND;
        } else if (binding.xa.isChecked()) {
            return Presence.Status.OFFLINE;
        } else if (binding.away.isChecked()) {
            return Presence.Status.AWAY;
        } else {
            return Presence.Status.ONLINE;
        }
    }
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        updateOfflineStatusBar();
    }
}