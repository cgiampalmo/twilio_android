package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.ContactsContract;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.databinding.DialogPresenceBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Presence;
import com.glaciersecurity.glaciermessenger.entities.PresenceTemplate;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;
import com.glaciersecurity.glaciermessenger.ui.util.ActivityResult;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;


import com.glaciersecurity.glaciermessenger.R;
//import com.glaciersecurity.glaciermessenger.databinding.ActivityChooseContactBinding;
import com.glaciersecurity.glaciermessenger.ui.adapter.SwipeItemTouchHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingActionHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.LogoutListener;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.adapter.SmsProfileAdapter;
import com.glaciersecurity.glaciermessenger.utils.SMSdbInfo;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;
import com.twilio.conversations.ErrorInfo;
import com.twilio.conversations.StatusListener;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SMSActivity  extends XmppActivity implements ConversationsManagerListener,OnSMSConversationClickListener, OnSMSProfileClickListener, LogoutListener, OnSMSRemoveClickListener, OnSMSNameClickListener, ConnectivityReceiver.ConnectivityReceiverListener {
    private ActionBar actionBar;
    private float mSwipeEscapeVelocity = 0f;
    private static final int PURCHASE_NUM_REQUEST = 1;
    private PendingActionHelper pendingActionHelper = new PendingActionHelper();
    private final PendingItem<Conversation> swipedSMSConversation = new PendingItem<>();
    private Toolbar toolbar;
    public FloatingActionButton fab_contact;
    private Button addNumberBtn;
    private Button releaseNumberBtn;
    private Button nameNumberBtn;
    public  ProgressBar progressBar;
    private String lastWaitMsg = null;
    private TextView waitTextField = null;
    private android.app.AlertDialog waitDialog = null;
//    public FloatingActionButton fab_group;
//    public FloatingActionButton fab_add;

    RecyclerView recyclerViewConversations;
    RecyclerView recyclerViewSMS;
    private ConnectivityReceiver connectivityReceiver; //CMG AM-41
    protected LinearLayout offlineLayout;

    @Override
    public void OnSMSRemoveClick(SmsProfile selectedSMSforRelease) {
        drawer_sms.close();
        showWaitDialog("Releasing number");
        new Thread(new Runnable() {
            public void run() {
                try {
                    ReleaseNum(selectedSMSforRelease);
                } catch (Exception e) {
                }
                closeWaitDialog();


            }
        }).start();
    }
    @Override
    public void OnSMSNameClick(String nickname, SmsProfile selectedSMSforName) {
        drawer_sms.close();
        showWaitDialog("Updating nickname");
        new Thread(new Runnable() {
            public void run() {
                try {
                    NicknameNum(nickname, selectedSMSforName);
                } catch (Exception e) {
                }
                closeWaitDialog();


            }
        }).start();



    }

    private void NicknameNum(String nickname, SmsProfile smsProfile){
        String nicknameNumberUrl = mContext.getString(R.string.nickname_num_url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("twilioNumber", smsProfile.getUnformattedNumber())
                .add("username",identity)
                .add("nickname",nickname)
                .build();
        Request request = new Request.Builder()
                .url(nicknameNumberUrl)
                .put(requestBody)
                .addHeader("API-Key", mContext.getResources().getString(R.string.twilio_token))
                .build();
        Log.d("Glacier", "request " + request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Gson gson = new Gson();
            NicknameNumResponse nicknameNumResponse = gson.fromJson(responseBody, NicknameNumResponse.class);
            if(nicknameNumResponse.message.equals("success")){
                //Toast.makeText(mContext,"Nickname updated successfully",Toast.LENGTH_LONG).show();

                runOnUiThread(() -> {
                    smsProfile.setNickname(nickname);
                    setUpdateName(smsProfile);
                    xmppConnectionService.setSmsProfList(profileList);
                    adapter_sms.notifyDataSetChanged();
                    //reload_adapter_sms();
                    setColorForNumber(proxyNumber);

                });
            }else{
                Toast.makeText(mContext,"Failed to update nickname. Please try again",Toast.LENGTH_LONG).show();
            }
            //onBackendConnected();
            Log.d("Glacier", "Response from server: " + responseBody);
        }catch (Exception ex){
            Log.e("Glacier", ex.getLocalizedMessage(), ex);
            Toast.makeText(mContext,"Failed to update nickname",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.e("Glacier", "onclick");
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        updateOfflineStatusBar();
    }


    private class NicknameNumResponse{
        String message;
        String data;
    }


    private class ReleaseNumResponse{
        String message;
        String data;
    }
    private View back_drop;
    private boolean rotate = false;
//    private View lyt_group;
//    private View lyt_chat;

    private DrawerLayout drawer_sms;
    SmsProfileAdapter adapter_sms;
    RecyclerView.LayoutManager layoutManagerSMS;
    ArrayList<SmsProfile> profileList= new ArrayList<>();
    ArrayList<String> proxyNumbers = new ArrayList<>();


    private MessagesAdapter messagesAdapter;
    private String accessToken;
    private Context mContext = this;
    private String identity;
    private String proxyNumber;
    private boolean PurchaseNumber;
    TokenModel Atoken = new TokenModel();
    private static final String KEY_TEXT_REPLY = "key_text_reply";
    private static final String MARK_AS_READ = "mark_as_read";
    NotificationManagerCompat managerCompat;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"sms");
    Map<String, String> cList =new HashMap<>();
    ArrayList<ContactModel> arrayList = new ArrayList<ContactModel>();
    private int swipedPos = -1;

    @Override
    public void receivedNewMessage(String newMessage,String messageConversationSid,String messageAuthor,String messageTo) {
        messagesAdapter.notifyDataSetChanged();
        Log.d("Glacier","unread_conv_count----"+profileList);
        reload_adapter_sms();
        String checkIdentity = model.getIdentity();
        if(checkIdentity.equals(identity) && !(messageAuthor.equals(identity))) {
            Conversation current_conv = model.getConversation();
            Log.d("Glacier", "receivedNewMessage called----" + current_conv + "----" + messageConversationSid);
            if (current_conv != null)
                Log.d("Glacier", "Current Conversation new message " + current_conv.getSid() + " : " + messageConversationSid + " : " + current_conv.getSid().equals(messageConversationSid) + " : " + messageAuthor);
            Log.d("Glacier", "Current Conversation new message" +identity+" : " + messageAuthor + "---"+identity.equals(messageAuthor));
            if (current_conv == null)
                notifyMessage(newMessage, messageAuthor,messageTo);
            else if (!identity.equals(messageAuthor)) {
                notifyMessage(newMessage, messageAuthor,messageTo);
            }
        }
    }
    public void notifyMessage(String newMessage,String messageAuthor,String messageTo){
        Log.d("Glacier", "New notification notifyMessage called"+messageTo);
        Intent intent = new Intent(mContext, SMSActivity.class);
        intent.removeExtra("ProxyNum");
        intent.putExtra("ProxyNum",messageTo);
        //Toast.makeText(mContext, "notifyMessage "+messageTo, Toast.LENGTH_SHORT).show();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_MUTABLE);

        Log.d("Glacier", "New notification before remoteInput");
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Reply").build();
        RemoteInput remoteInput2 = new RemoteInput.Builder(MARK_AS_READ).setLabel("Mark as read").build();
        Intent replyIntent = new Intent(this,RemoteReceiver.class);
        Log.d("Glacier", "New notification before replyIntent");
        replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent replyPendingIntent = PendingIntent.getActivity(this,0,replyIntent,PendingIntent.FLAG_MUTABLE);
        builder.setContentTitle("Glacier SMS for "+ Tools.lastFourDigits(messageTo));
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentText(newMessage);
        Log.d("Glacier", "New notification after newMessage");
        builder.setSmallIcon(R.drawable.ic_baseline_sms_24);
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);
        managerCompat = NotificationManagerCompat.from(this);
        if(messageAuthor.length() > 5) {
            managerCompat.notify(Integer.parseInt(messageAuthor.substring(2, 5)), builder.build());
            model.setNotificationManager(managerCompat);
        }
    }
    @Override
    public void messageSentCallback() {
        messagesAdapter.notifyDataSetChanged();
    }

    @Override
    public void reloadMessages() {

        //getContactList();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                Log.d("Glacier","Reload messages called");
                messagesAdapter.notifyDataSetChanged();
            }
        });

    }

    public void showList() {
        //Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation(proxyNumber)+"------"+ConversationsManager.conversationsClient.getMyConversations().size());
        if(messagesAdapter == null) {
            Log.d("Glacier","New Message adapter");
            messagesAdapter = new SMSActivity.MessagesAdapter((OnSMSConversationClickListener) this);
            recyclerViewConversations.setAdapter(messagesAdapter);

        }else{
            Log.d("Glacier","old Message adapter");
            messagesAdapter.notifyDataSetChanged();
        }
        //reload_adapter_sms();
        if(ConversationsManager.conversationsClient.getMyConversations().size() > 0) {
            List<Conversation> conversationList = ConversationsManager.conversationsClient.getMyConversations();
            Map<String, String> aList = new HashMap<>();
            for (Conversation conv : conversationList) {
                aList.put(conv.getFriendlyName(), conv.getSid());
            }
            model.setContConv(aList);
            model.setConversationsClient(ConversationsManager.conversationsClient);
            this.recyclerViewConversations.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (swipedPos < 0) return false;
                    Point point = new Point((int) motionEvent.getRawX(), (int) motionEvent.getRawY());

                    RecyclerView.ViewHolder swipedViewHolder = recyclerViewConversations.findViewHolderForAdapterPosition(swipedPos);
                    View swipedItem = swipedViewHolder.itemView;
                    Rect rect = new Rect();
                    swipedItem.getGlobalVisibleRect(rect);

                    return false;
                }
            });

            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public float getSwipeEscapeVelocity (float defaultValue) {
                    return mSwipeEscapeVelocity;
                }

                @Override
                public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                        float dX, float dY, int actionState, boolean isCurrentlyActive) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                    new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                            .addSwipeRightBackgroundColor(Color.rgb(234, 122, 98))

                            .addSwipeRightActionIcon(R.drawable.ic_delete)

                            .create()
                            .decorate();

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                    if(actionState != ItemTouchHelper.ACTION_STATE_IDLE){
                        Paint paint = new Paint();
                        paint.setColor(StyledAttributes.getColor(getApplicationContext(),R.attr.conversations_overview_background));
                        paint.setStyle(Paint.Style.FILL);
                        c.drawRect(viewHolder.itemView.getLeft(),viewHolder.itemView.getTop()
                                ,viewHolder.itemView.getRight(),viewHolder.itemView.getBottom(), paint);
                    }
                }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    viewHolder.itemView.setAlpha(1f);
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    pendingActionHelper.execute();
                    int position = viewHolder.getLayoutPosition();
                    try {
                       swipedSMSConversation.push(conversationList.get(position));
                    } catch (IndexOutOfBoundsException e) {
                        return;
                    }

                    swipeDelete(swipedSMSConversation, position);
                    //messagesAdapter.remove(swipedSMSConversation.peek(),position);

                }
            };
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recyclerViewConversations);
            Log.d("Glacier","conversationList " +conversationList.size() + messagesAdapter);
//            View emptyLayout = findViewById(R.id.empty_list);
//            emptyLayout.setVisibility(View.GONE);
//            Log.d("Glacier","conversationsClient emptyLayout " +conversationList.size() + emptyLayout.getVisibility());

        }
        progressBar.setVisibility(View.GONE);
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkPermission();
            }
        });*/
        checkEmptyView();
    }

    protected void swipeDelete(PendingItem<Conversation> swipedConversation, int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(SMSActivity.this);
        builder.setMessage(R.string.delete_sms_convo_dialog);
        builder.setTitle("Confirmation");
        builder.setCancelable(false);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)    {
                messagesAdapter.remove(swipedSMSConversation.peek(),position);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)    {
                pendingActionHelper.undo();
                Conversation conversation = swipedConversation.pop();
                messagesAdapter.insert(conversation, position);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void notifyMessages(String newMessage,String messageAuthor,String messageTo) {
        notifyMessage(newMessage,messageAuthor,messageTo);
        reload_adapter_sms();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStop () {
        unregisterReceiver(connectivityReceiver);
        super.onStop();
    }
    public void reload_adapter_sms(){
        ArrayList<SmsProfile> smSInfo;


        if(xmppConnectionService != null) {
//            //xmppConnectionService.updateSmsInfo();
//            proxyNumbers.clear();
//            profileList.clear();
//            xmppConnectionService.updateSmsInfo();
//            try { Thread.sleep(750); } catch (InterruptedException ie) {}
            smSInfo = xmppConnectionService.getSmsInfo().getExistingProfs();
        } else {
            smSInfo = profileList;
        }

        Log.d("Glacier", "unread_conv_count----" + ConversationsManager.unread_conv_count + xmppConnectionService + smSInfo);
        for (SmsProfile smsProfile : smSInfo) {
            Log.d("Glacier", "unread_conv_count---- " + ConversationsManager.unread_conv_count + profileList.contains(smsProfile));
            if(!(profileList.contains(smsProfile)))
                profileList.add(0, smsProfile);
            smsProfile.setUnread_count(0);
            if (ConversationsManager.unread_conv_count == null || ConversationsManager.unread_conv_count.isEmpty()) {
                smsProfile.setUnread_count(0);
            } else {
                String number = smsProfile.getFormattedNumber().replaceAll(" ", "").replace("(", "").replace(")", "").replace("-", "");
                Integer unread_count = ConversationsManager.unread_conv_count.get(number);
                Log.d("Glacier", "unread_conv_count---- " + unread_count);
                if (unread_count != null) {
                    smsProfile.setUnread_count(unread_count);
                }
            }

            proxyNumbers.add(smsProfile.getFormattedNumber());
            adapter_sms.notifyItemInserted(0);
        }

        if(proxyNumber == null){
            if(proxyNumbers.size() > 0) {
                proxyNumber = proxyNumbers.get(0);
                model.setProxyNumber(proxyNumber);
                OnSMSProfileClick("", proxyNumber);
            }
        }

        if(adapter_sms != null)
            adapter_sms.notifyDataSetChanged();
        else
            Log.d("Glacier","adapter_sms is null");


        checkEmptyView();
        showPurchaseView();
    }

    private final ConversationsManager ConversationsManager = new ConversationsManager(this);
    ConversationModel model;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {
        progressBar.setVisibility(View.VISIBLE);
        xmppConnectionService.updateSmsInfo();

        try { Thread.sleep(750); } catch (InterruptedException ie) {}
        reload_adapter_sms();

        //Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation(proxyNumber));
        ConversationsClient conversationsClient = model.getConversationsClient();

        ConversationsManager.setListener(this);
        Log.d("Glacier","Twilio ConversationsClient "+conversationsClient);
        if(conversationsClient != null){
            model.setConversation(null);
            Log.d("Glacier","proxyNumbers"+ proxyNumbers.toArray());
            ConversationsManager.addListenerLoadChannels(conversationsClient,proxyNumbers.toArray());
            if(model.getProxyNumber() == null || model.getProxyNumber().equals("") ) {
                if(proxyNumbers != null && (proxyNumber == null || proxyNumber.length() > 0)) {
                    if(proxyNumbers.size() > 0) {
                        model.setProxyNumber(proxyNumbers.get(0));
                        proxyNumber = proxyNumbers.get(0);
                    }
                }
            }



        }else{
            retrieveTokenFromServer();
        }

        updateOfflineStatusBar();
        checkEmptyView();
        progressBar.setVisibility(View.GONE);
    }



    private void ReleaseNum(SmsProfile releaseProfile){
        String releaseNumberUrl = SMSActivity.this.getString(R.string.release_num_url);
        String unformattedNumber = releaseProfile.getUnformattedNumber();
        String formattedNumber = releaseProfile.getFormattedNumber();

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("releaseNumber", unformattedNumber)
                .add("username",xmppConnectionService.getAccounts().get(0).getUsername())
                .build();
        Request request = new Request.Builder()
                .url(releaseNumberUrl)
                .post(requestBody)
                .addHeader("API-Key", xmppConnectionService.getApplicationContext().getResources().getString(R.string.twilio_token))
                .build();
        Log.d("Glacier", "request " + request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Gson gson = new Gson();
            ReleaseNumResponse releaseNumResponse = gson.fromJson(responseBody, ReleaseNumResponse.class);
            if(releaseNumResponse.message.equals("success")){
                runOnUiThread(() -> {
                    removeProfile(releaseProfile);

                    if (proxyNumber.equals(unformattedNumber)) {
                        proxyNumber = null;
                        model.setProxyNumber(null);
                    }
                    if (proxyNumbers != null && (proxyNumber == null || proxyNumber.equals(""))) {
                        if (proxyNumbers.size() > 0) {
                            String nextProxy = proxyNumbers.get(proxyNumbers.size()-1);
                            model.setProxyNumber(nextProxy);
                            proxyNumber = nextProxy;
                            drawer_sms.close();
                            OnSMSProfileClick("", nextProxy);
                        }
                    }
                });

            }else{
                runOnUiThread(() -> {
                    Toast.makeText(SMSActivity.this, "Failed to release. Please try again", Toast.LENGTH_LONG).show();
                });
                }
            //onBackendConnected();
            Log.d("Glacier", "Response from server: " + responseBody);
            closeWaitDialog();

        }catch (Exception ex){
            Log.e("Glacier", ex.getLocalizedMessage(), ex);
            Toast.makeText(SMSActivity.this,"Failed to release. Please try again",Toast.LENGTH_LONG).show();
            closeWaitDialog();

        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        proxyNumber = model.getProxyNumber();
        Log.d("Glacier","onNewIntent "+intent.hasExtra("ProxyNum"));
        if(intent.hasExtra("ProxyNum") && (!(intent.getExtras().getString("ProxyNum").equals(proxyNumber)))){
            Log.d("Glacier","onNewIntent proxy num not equal");
            OnSMSProfileClick("", intent.getExtras().getString("ProxyNum"));
        }else{
            if(!intent.hasExtra("ProxyNum")) {
                ConversationsManager.loadChannels(model.getConversationsClient());
            }
        }
    }

    public void closeWaitDialog() {
        if (waitDialog != null) {
            waitDialog.dismiss();
            //ALF AM-190
            waitDialog = null;
            lastWaitMsg = null;
            waitTextField = null;
        }
    }

    public void showWaitDialog(String message) {
        //ALF AM-202 extended also check if Activity is finishing
        if (this.isFinishing()) {
            return;
        }

        //ALF AM-190
        if (lastWaitMsg != null && message.equalsIgnoreCase(lastWaitMsg)) {
            return;
        } else if (waitDialog != null && waitTextField != null) {
            waitTextField.setText(message);
            return;
        }

        lastWaitMsg = message;
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_wait, null);
        waitTextField = layout.findViewById(R.id.status_message);
        waitTextField.setText(message);

        //AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(SMSActivity.this);
        builder.setView(layout);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setTitle("Please Wait");

        waitDialog = builder.create();
        waitDialog.show();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_drawer_sms);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setTitle("Glacier SMS");
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        model = (ConversationModel) getApplicationContext();
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        fab_contact = (FloatingActionButton) findViewById(R.id.fab_chat);
        fab_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accessToken = Atoken.getAccessToken();
                //getContactList();
                Log.d("Glacier","conversationsClient "+ConversationsManager.conversationsClient);
                if (ConversationsManager.conversationsClient != null){
                    if (proxyNumber == null){
                        Toast.makeText(mContext, "No SMS numbers configured for this account", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        model.setConversationsClient(ConversationsManager.conversationsClient);
                        Intent intent = new Intent(mContext, ContactListActivity.class);
                        String conv_Sid = "new";
                        startActivity(intent.putExtra("conv_sid", conv_Sid).putExtra("identity", identity).putExtra("conversationToken", accessToken).putExtra("title", "New message"));

                    }}else{
                    Toast.makeText(mContext, "Please wait the SMS is not loaded successfully", Toast.LENGTH_SHORT).show();
                }
            }
        });
        actionBar.setHomeButtonEnabled(true);
        this.offlineLayout = findViewById(R.id.offline_layout);
        this.offlineLayout.setOnClickListener(mRefreshNetworkClickListener);
        connectivityReceiver = new ConnectivityReceiver(this);
        updateOfflineStatusBar();



        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDrawerOpened();
            }
        });
        if(getIntent().hasExtra("account")) {
            identity = getIntent().getExtras().getString("account");
            model.setIdentity(identity);
            Log.d("Glacier ","Twilio Conversation "+model.getConversation());
        }else{
            identity = model.getIdentity();
            model.setConversation(null);
            Log.d("Glacier","Identity "+identity);
        }
        if(getIntent().hasExtra("ProxyNum")) {
            proxyNumber = getIntent().getExtras().getString("ProxyNum");
            model.setProxyNumber(proxyNumber);
            setColorForNumber(proxyNumber);
            if (messagesAdapter != null) {
                messagesAdapter.notifyDataSetChanged();
            }

        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel smsChannel = new NotificationChannel("sms",
                    this.getString(R.string.sms_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            smsChannel.setShowBadge(true);
            smsChannel.enableVibration(true);
            smsChannel.enableLights(true);
            smsChannel.setGroup("sms");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(smsChannel);

        }

        recyclerViewConversations = findViewById(R.id.choose_conversation_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewConversations.setLayoutManager(layoutManager);


        recyclerViewSMS = (RecyclerView) findViewById(R.id.sms_recycler_view);
        layoutManagerSMS = new LinearLayoutManager(this);
        recyclerViewSMS.setLayoutManager(layoutManagerSMS);
        drawer_sms = (DrawerLayout) findViewById(R.id.drawer_layout_sms);
        adapter_sms = new SmsProfileAdapter(this, identity,  this, this, this,  profileList);
        releaseNumberBtn = (Button) findViewById(R.id.release_number);
        releaseNumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter_sms.toggleNameOff();
                adapter_sms.toggleDeleteVisible();
            }
        });

        nameNumberBtn = (Button) findViewById(R.id.name_a_number);
        nameNumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter_sms.toggleDeleteOff();
                adapter_sms.toggleNameVisible();
            }
        });

        addNumberBtn = (Button) findViewById(R.id.add_number);
        addNumberBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, PurchaseNumbers.class);
                startActivityForResult(intent, PURCHASE_NUM_REQUEST);
            }});
        progressBar = findViewById(R.id.progressBar);
        recyclerViewSMS.setAdapter(adapter_sms);
        Log.d("Glacier","identity sdns n "+identity);
        showPurchaseView();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PURCHASE_NUM_REQUEST) {
            if(resultCode == Activity.RESULT_OK){
                String proxyData = data.getStringExtra("proxyNum");
                String sid = data.getStringExtra("sid");
                proxyNumber = proxyData;
                model.setProxyNumber(proxyNumber);
                drawer_sms.close();
                addProfile(new SmsProfile(proxyData, sid));
                OnSMSProfileClick(sid, proxyData);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                // Write your code if there's no result
            }
        }
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_group, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void showUnimplimentedToast(){
        if (xmppConnectionService != null) {
            SMSdbInfo info = xmppConnectionService.getSmsInfo();
            ArrayList<SmsProfile> smSdbInfo = info.getExistingProfs();
            PurchaseNumber = info.getUserPurchasePermission();
            if (!PurchaseNumber) {
                if (smSdbInfo.size() <= 0) {
                    Toast.makeText(this, R.string.no_auth_sms, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    public void showPurchaseView(){
        if (xmppConnectionService != null) {
            SMSdbInfo info = xmppConnectionService.getSmsInfo();
            ArrayList<SmsProfile> smSdbInfo = info.getExistingProfs();
            PurchaseNumber = info.getUserPurchasePermission();
            if (PurchaseNumber) {
                addNumberBtn.setVisibility(View.VISIBLE);
                if (smSdbInfo.size() > 0) {
                    releaseNumberBtn.setVisibility(View.VISIBLE);
                } else {
                    releaseNumberBtn.setVisibility(View.GONE);
                }
            } else {
                addNumberBtn.setVisibility(View.GONE);
                releaseNumberBtn.setVisibility(View.GONE);
            }
            if (smSdbInfo.size() > 0) {
                nameNumberBtn.setVisibility(View.VISIBLE);
            } else {
                nameNumberBtn.setVisibility(View.GONE);
            }
        }
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("Glacier","menu_group_call_participants_list "+item.getItemId()+"======="+R.id.menu_group_call_participants_list);
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.sms_accounts:
                onDrawerOpened();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onDrawerOpened(){
            if (xmppConnectionService != null) {
                reload_adapter_sms();
                setColorForNumber(proxyNumber);
                showPurchaseView();
                showUnimplimentedToast();
            }
            adapter_sms.toggleDeleteOff();
            adapter_sms.toggleNameOff();
            drawer_sms.openDrawer(GravityCompat.START);
    }

    private void removeProfile(SmsProfile smsProfile){
        for (int i = 0; i < profileList.size(); i++){
            if (smsProfile.equals(profileList.get(i))){
                profileList.remove(i);
            }
        }
        xmppConnectionService.setSmsProfList(profileList);
        adapter_sms.notifyDataSetChanged();
    }

    private void addProfile(SmsProfile smsProfile){
        profileList.add(smsProfile);
        xmppConnectionService.setSmsProfList(profileList);
        adapter_sms.notifyDataSetChanged();
    }
    private void setUpdateName(SmsProfile smsProfile){
        for (int i = 0; i < profileList.size(); i++){
            if (smsProfile.equals(profileList.get(i))){
                profileList.set(i,smsProfile);
            }
        }
        xmppConnectionService.setSmsProfList(profileList);
        adapter_sms.notifyDataSetChanged();
    }


    private void checkPermission() {
        Log.d("Glacier","checkPermission called");
        if (ContextCompat.checkSelfPermission(SMSActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SMSActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, 100);
        } else {
            Log.d("Glacier","getContactList ");
            getContactList();
        }
    }
    @SuppressLint("Range")
    private void getContactList(){
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC";
        Cursor cursor = getContentResolver().query(uri,null,null,null,sort);
        Log.d("Glacier","cursor "+cursor.getCount());
        Map<String, String> convContList = new HashMap<>();
        convContList = model.getContConv();
        if(cursor.getCount() > 0){
            while (cursor.moveToNext()){
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Uri uriPhone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                    String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?";
                    Cursor phoneCursor = getContentResolver().query(uriPhone, null, selection, new String[]{id}, null);
                    if (phoneCursor.moveToNext()) {
                        String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        ContactModel model = new ContactModel();
                        model.setName(name);

                        arrayList.add(model);
                        //Log.d("Glacier","convContList "+convContList);

                        number = number.replaceAll(" ","");
                        number = number.replace("+1","");
                        number = number.replace("(","");
                        number = number.replace(")","");
                        number = number.replace("-","");
                        model.setNumber(number);
                        if(number.length() > 8 && ( convContList == null || !(convContList.size() > 0) || !convContList.containsKey(number))) {
                            if(convContList == null){
                                convContList = new HashMap<>();
                            }
                            convContList.put(number,"No sid");
                        }
                        phoneCursor.close();
                        cList.put(number,name);
                    }
                    //Log.d("Glacier", "cursor arrayList " + arrayList.size());
                }
            }
            Log.d("Glacier","cursor final arrayList "+arrayList.size());
            cursor.close();
        }
        model.setContConv(convContList);
        model.setArrayList(arrayList);
        model.setcList(cList);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.d("Glacier","getContactList ");
            getContactList();
        }else{
            //Toast.makeText(ContactListActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            checkPermission();
        }
    }
    public void onResume(){
        super.onResume();
        drawer_sms.close();
        Log.d("Glacier","onResume is called");
    }
    public void onRestart(){
        super.onRestart();
        Log.d("Glacier","onRestart is called");
        //TODO
    }
    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, ConversationsActivity.class);
        startActivity(intent.putExtra("account",identity));
    }
    private void retrieveTokenFromServer() {
        ConversationsManager.retrieveAccessTokenFromServer(this, identity, new TokenResponseListener() {
            @Override
            public void receivedTokenResponse(boolean success, @Nullable Exception exception,String token) {
                if (success) {
//                    SMSActivity.accessToken = token;
                    Atoken.setAccessToken(token);
                    //ConversationsManager.initializeWithAccessToken(this,)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // need to modify user interface elements on the UI thread
                            checkPermission();
                            //setTitle(identity);
                            if(model.getProxyNumber() == null || model.getProxyNumber().equals("") ) {
                                if(ConversationsManager.proxyAddress != null && ConversationsManager.proxyAddress.length > 0) {
                                    model.setProxyNumber(ConversationsManager.proxyAddress[0].toString());
                                    proxyNumber = ConversationsManager.proxyAddress[0].toString();
                                }

                            }else
                                proxyNumber = model.getProxyNumber();
                            if(model.getPurchaseNumber() == null) {
                                Log.d("Glacier", "setPurchaseNumber " + ConversationsManager.PurchaseNumber);
                                model.setPurchaseNumber(ConversationsManager.PurchaseNumber);
                                PurchaseNumber = ConversationsManager.PurchaseNumber;
                            }
                            setColorForNumber(proxyNumber);
                        }
                    });
                }
                else {
                    String errorMessage = getString(R.string.error_retrieving_access_token);
                    if (exception != null) {
                        errorMessage = errorMessage + " " + exception.getLocalizedMessage();
                    }
                    else
                        Log.d("Glacier","errorMessage "+errorMessage);
                }
            }
        });

    }

    public void setColorForNumber(String number){
        if (xmppConnectionService == null || number == null) {
            setTitle("Glacier SMS");
            toolbar.setBackgroundColor(getColor(R.color.primary_bg_color));
            //fab_contact.setVisibility(View.INVISIBLE);
            return;
        }

        SmsProfile sp_= xmppConnectionService.getSmsInfo().getSMSProfilefromNumber(number);
        if (sp_ != null) {
            if(sp_.getNickname() != null && !sp_.getNickname().isEmpty() ){
                setTitle(sp_.getNickname());
            } else {
                setTitle(sp_.getFormattedNumber());
            }
            toolbar.setBackgroundColor(sp_.getColor());
            fab_contact.setVisibility(View.VISIBLE);
            fab_contact.setBackgroundTintList(ColorStateList.valueOf(sp_.getColor()));
        }
        else {
            setTitle("Glacier SMS");
            toolbar.setBackgroundColor(getColor(R.color.primary_bg_color));
            //fab_contact.setVisibility(View.INVISIBLE);
            // fab_contact.setBackgroundTintList(ColorStateList.valueOf(UIHelper.getColorForSMS(formattedNumber)));
        }
    }

    public SmsProfile getSMSProfilefromNumber(String number){
        for (SmsProfile sp: profileList){
            if(sp.getUnformattedNumber().equals(number) || sp.getFormattedNumber().equals(number)){
                return sp;
            }
        }
        return null;
    }
    public void OnSMSConversationClick(String conv_sid,String conv_name) {
        Log.d("Glacier","OnSMSConversationClick called "+Atoken.getAccessToken()+" conv_sid "+conv_sid);
        String token = Atoken.getAccessToken();
        Intent intent = new Intent(this,smsConvActivity.class);
        String check_group = conv_name.replace("+","");
        if(check_group.matches("\\d+(?:\\.\\d+)?"))
            startActivity(intent.putExtra("conv_sid",conv_sid).putExtra("identity",identity).putExtra("conversationToken", token).putExtra("title","New message").putExtra("title",conv_name));
        else
            startActivity(intent.putExtra("conv_sid",conv_sid).putExtra("is_group",true).putExtra("identity",identity).putExtra("conversationToken", token).putExtra("title","New message").putExtra("title",conv_name));
    }

    @Override
    public void onLogout() {
        Log.d("Glacier","User is not null and logging out");
        ConversationModel model = (ConversationModel) getApplicationContext();
        ArrayList<ContactModel> arrayList = new ArrayList<>();
        Map<String, String> cList = new HashMap<>();
        model.cancelAllNotification();
        model.setArrayList(arrayList);
        model.setcList(cList);
        model.setConversation(null);
        model.setIdentity("");
        model.setConversationsClient(null);
        model.setContConv(null);
        model.setNotificationManager(null);
        model.setProxyNumber(null);
        System.exit(0);
    }

    @Override
    public void OnSMSProfileClick(String id, String number) {
        drawer_sms.closeDrawers();
        number = number.replace("(","").replace(")","").replace("-","").replace(" ","");
        model.setProxyNumber(number);
        proxyNumber = number;
        setColorForNumber(proxyNumber);
        if (messagesAdapter != null) {
            messagesAdapter.notifyDataSetChanged();
        }
    }

    public void checkEmptyView(){
        if( xmppConnectionService == null || ConversationsManager.getConversation(proxyNumber) == null || ConversationsManager.getConversation(proxyNumber).size() == 0){
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.GONE);
        }
    }
    class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> implements SwipeItemTouchHelper.SwipeHelperAdapter{
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        private OnSMSConversationClickListener listener;
        private List<Conversation> conversations;
        Map<String, String> cList = model.getcList();

        @Override
        public void onItemDismiss(int position) {

        }


        class ViewHolder extends RecyclerView.ViewHolder  implements View.OnClickListener, View.OnTouchListener, SwipeItemTouchHelper.TouchViewHolder {
            final View conView;
            //            public Activity unreadCount;
            private OnSMSConversationClickListener listener;
            @Override
            public void onItemSelected() {
                itemView.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.grey_5));
            }

            @Override
            public void onItemClear() {
                itemView.setBackgroundColor(0);
            }

            ViewHolder(View con_view,OnSMSConversationClickListener listener) {
                super(con_view);
                this.listener = listener;
                conView = con_view;
                con_view.setOnClickListener(this);
            }
            public void onClick(View view) {
                Conversation conversation = ConversationsManager.getConversation(proxyNumber).get(getAdapterPosition());
                String conversation_sid = conversation.getSid();
                String conversation_name = conversation.getFriendlyName();
                model.setConversationsClient(ConversationsManager.conversationsClient);
                model.setConversation(conversation);
                conversation.setAllMessagesRead(new CallbackListener<Long>() {
                    @Override
                    public void onSuccess(Long result) {
                        Log.d("Glacier","setAllMessagesRead "+result);
                    }
                });
                listener.OnSMSConversationClick(conversation_sid,conversation_name);
            }

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        }

        MessagesAdapter(OnSMSConversationClickListener listener) {
            this.listener = listener;
            Log.d("Glacier","IN message adapter"+proxyNumber);
        }
        public void setConversationClickListener(OnSMSConversationClickListener listener) {
            this.listener = listener;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent,
                                             int viewType) {
            Log.d("Glacier","onCreateViewHolder called");
            View conView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sms_list_row, parent, false);
            return new ViewHolder(conView,listener);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            TextView conversation_name, sender_name, conversation_lastmsg, dateText, conv_Sid;
            RelativeLayout avatar_circle;
            com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView unreadcount;
            conversations = ConversationsManager.getConversation(proxyNumber);
            Conversation conversation = conversations.get(holder.getAdapterPosition());
            Map conv_last_msg = ConversationsManager.conv_last_msg;
            Map conv_last_msg_sent = ConversationsManager.conv_last_msg_sent;
            Log.d("Glacier", "ConversationsManager " + conversation.getFriendlyName() + "----------" + conversation.getLastMessageDate());
            conversation_name = holder.conView.findViewById(R.id.conversation_name);
            avatar_circle = holder.conView.findViewById(R.id.avatar_circle);
            sender_name = holder.conView.findViewById(R.id.sender_name);
            conversation_lastmsg = holder.conView.findViewById(R.id.conversation_lastmsg);
            dateText = holder.conView.findViewById(R.id.conversation_lastupdate);
            String Contact_name = (cList != null && cList.get(conversation.getFriendlyName()) != null) ? cList.get(conversation.getFriendlyName()) : Tools.reformatNumber(conversation.getFriendlyName());
            avatar_circle.setBackgroundTintList(ColorStateList.valueOf(UIHelper.getColorForName(Contact_name)));
            String sender_name_text = "";
            if (conv_last_msg_sent.containsKey(conversation.getSid()) && conv_last_msg_sent.get(conversation.getSid()).toString().equals(identity)) {
                sender_name_text = "Me :";
                sender_name.setVisibility(View.VISIBLE);
            } else if (conv_last_msg_sent.containsKey(conversation.getSid()) && conv_last_msg_sent.get(conversation.getSid()) != null){
                sender_name_text = (CharSequence) conv_last_msg_sent.get(conversation.getSid()) +" :";
                sender_name.setVisibility(View.GONE);
            }
            sender_name.setText(sender_name_text);

            conversation_name.setText(Contact_name);
            conversation_lastmsg.setText((CharSequence) conv_last_msg.get(conversation.getSid()));
            unreadcount = holder.conView.findViewById(R.id.unread_count);
            unreadcount.setVisibility(View.GONE);
            conversation_lastmsg.setTypeface(Typeface.DEFAULT);
            conversation.getUnreadMessagesCount(new CallbackListener<Long>() {
                @Override
                public void onSuccess(Long result) {
                    Log.d("Glacier", "setUnreadCount " + result + " " + conversation.getFriendlyName() + " adapter position " + holder.getAdapterPosition());
                    if (result != null) {
                        if (result > 0) {
                            if (holder.getAdapterPosition() > -1) {
                                if (conversation.getFriendlyName().equals(conversations.get(holder.getAdapterPosition()).getFriendlyName())) {
                                    unreadcount.setUnreadCount(Math.toIntExact(result));
                                    if (Math.toIntExact(result) > 0){
                                        unreadcount.setVisibility(View.VISIBLE);
                                    } else {
                                        unreadcount.setVisibility(View.GONE);
                                    }
                                    conversation_lastmsg.setTypeface(Typeface.DEFAULT_BOLD);
                                }
                            }
                        } else {

                        }
                    } else {
                        if (holder.getAdapterPosition() > -1) {
                            if (conversation.getFriendlyName().equals(conversations.get(holder.getAdapterPosition()).getFriendlyName())) {
                                conversation.getMessagesCount(new CallbackListener<Long>() {
                                    @Override
                                    public void onSuccess(Long result) {
                                        if (Math.toIntExact(result) > 0){
                                            unreadcount.setVisibility(View.VISIBLE);
                                            unreadcount.setUnreadCount(Math.toIntExact(result));
                                        } else {
                                            unreadcount.setVisibility(View.GONE);
                                        }
                                    }
                                });
                                conversation_lastmsg.setTypeface(Typeface.DEFAULT_BOLD);
                            }
                        }
                    }
                }
            });


            if(conv_last_msg.get(conversation.getSid()) != null)
                dateText.setText(df.format("MMM d hh:mm", conversation.getLastMessageDate()).toString());
            else
                dateText.setText("");

            //String messageText = String.format(                dateText.setText(df.format("d hh:mm", conversation.getLastMessageDate()).toString());"%s: %s", conversation.getFriendlyName(), conversation.getSid());
            //holder.messageTextView.setText(messageText);
        }

        @Override
        public int getItemCount() {
            Log.d("Glacier","conversationsClient getItemCount "+ConversationsManager.getConversation_size(proxyNumber).size());
            return ConversationsManager.getConversation_size(proxyNumber).size();
        }

        public void insert(Conversation conversation, int position){
            notifyDataSetChanged();
        }
        public void remove(Conversation conversation, int position) {
            Conversation remove_conv =  ConversationsManager.getConversation(proxyNumber).get(position);
            Log.d("Glacier","Removing conversation "+remove_conv.getFriendlyName());
            remove_conv.destroy(new StatusListener() {
                @Override
                public void onSuccess() {
                    ConversationsManager.getConversation(proxyNumber).remove(remove_conv);
                    notifyItemRemoved(position);
                    Map<String, String> aList = model.getContConv();
                    if(aList != null){
                        aList.remove(remove_conv.getFriendlyName());
                        model.setContConv(aList);
                    }
                    Log.d("Glacier","Conversation deleted" + remove_conv.getFriendlyName() + aList);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    Log.d("Glacier","Deleting conversation error info "+errorInfo.getMessage());
                }
            });
            //TODO implement removal on conversation

            String contactName = (cList != null && cList.get(remove_conv.getFriendlyName()) != null) ? cList.get(remove_conv.getFriendlyName()) : remove_conv.getFriendlyName();

            Snackbar.make(recyclerViewConversations, contactName + ", DELETED.", Snackbar.LENGTH_LONG)

                    .setAction("OK", new View.OnClickListener() {

                        @Override

                        public void onClick(View view) {
                        }

                    }).show();
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
        List<PresenceTemplate> templates = xmppConnectionService.getPresenceTemplates(fragAccount);
        //CMG AM-365
//		PresenceTemplateAdapter presenceTemplateAdapter = new PresenceTemplateAdapter(this, R.layout.simple_list_item, templates);
// 		binding.statusMessage.setAdapter(presenceTemplateAdaptreer);
//		binding.statusMessage.setOnItemClickListener((parent, view, position, id) -> {
//			PresenceTemplate template = (PresenceTemplate) parent.getItemAtPosition(position);
//			setAvailabilityRadioButton(template.getStatus(), binding);
//			setStatusMessageRadioButton(mAccount.getPresenceStatusMessage(), binding);
//		});

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

}


interface OnSMSConversationClickListener {
    void OnSMSConversationClick(String connv_sid,String conv_name);
}
