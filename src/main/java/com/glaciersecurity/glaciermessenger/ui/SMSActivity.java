package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import static androidx.recyclerview.widget.ItemTouchHelper.RIGHT;


import com.glaciersecurity.glaciermessenger.R;
//import com.glaciersecurity.glaciermessenger.databinding.ActivityChooseContactBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.ui.adapter.SwipeItemTouchHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingActionHelper;
import com.glaciersecurity.glaciermessenger.ui.util.PendingItem;
import com.glaciersecurity.glaciermessenger.ui.util.ScrollState;
import com.glaciersecurity.glaciermessenger.ui.util.StyledAttributes;
import com.glaciersecurity.glaciermessenger.ui.util.ViewAnimation;
import com.glaciersecurity.glaciermessenger.utils.LogoutListener;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.adapter.SmsProfileAdapter;
import com.glaciersecurity.glaciermessenger.utils.SMSdbInfo;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;



public class SMSActivity  extends XmppActivity implements ConversationsManagerListener,OnSMSConversationClickListener, OnSMSProfileClickListener, LogoutListener {
    private ActionBar actionBar;
    private float mSwipeEscapeVelocity = 0f;
    private PendingActionHelper pendingActionHelper = new PendingActionHelper();
    private final PendingItem<Conversation> swipedSMSConversation = new PendingItem<>();
    private final PendingItem<ScrollState> pendingScrollState = new PendingItem<>();
    private Toolbar toolbar;
    public FloatingActionButton fab_contact;
//    public FloatingActionButton fab_group;
//    public FloatingActionButton fab_add;

    RecyclerView recyclerViewConversations;
    RecyclerView recyclerViewSMS;

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
    private Account account;
    private String identity;
    private String proxyNumber;
    private String mSavedInstanceAccount;
    TokenModel Atoken = new TokenModel();
    public String AccessToken;
    private static final String KEY_TEXT_REPLY = "key_text_reply";
    private static final String MARK_AS_READ = "mark_as_read";
    NotificationManagerCompat managerCompat;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"Glacier");
    //ContactAdapter adapter;
    Map<String, String> cList =new HashMap<>();
    ArrayList<ContactModel> arrayList = new ArrayList<ContactModel>();
    private int swipedPos = -1;

    @Override
    public void receivedNewMessage(String newMessage,String messageConversationSid,String messageAuthor) {
        messagesAdapter.notifyDataSetChanged();
        String checkIdentity = model.getIdentity();
        if(checkIdentity.equals(identity) && !(messageAuthor.equals(identity))) {
            Conversation current_conv = model.getConversation();
            Log.d("Glacier", "receivedNewMessage called----" + current_conv + "----" + messageConversationSid);
            if (current_conv != null)
                Log.d("Glacier", "Current Conversation new message " + current_conv.getSid() + " : " + messageConversationSid + " : " + current_conv.getSid().equals(messageConversationSid) + " : " + messageAuthor);
            Log.d("Glacier", "Current Conversation new message" +identity+" : " + messageAuthor + "---"+identity.equals(messageAuthor));
            if (current_conv == null)
                notifyMessage(newMessage, messageAuthor);
            else if (!identity.equals(messageAuthor)) {
                notifyMessage(newMessage, messageAuthor);
            }
        }
    }
    public void notifyMessage(String newMessage,String messageAuthor){
        Log.d("Glacier", "New notification notifyMessage called");
        Intent intent = new Intent(mContext, SMSActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_MUTABLE);


// Create the RemoteInput specifying this key
        Log.d("Glacier", "New notification before remoteInput");
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Reply").build();
        RemoteInput remoteInput2 = new RemoteInput.Builder(MARK_AS_READ).setLabel("Mark as read").build();
        Intent replyIntent = new Intent(this,RemoteReceiver.class);
        Log.d("Glacier", "New notification before replyIntent");
        replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent replyPendingIntent = PendingIntent.getActivity(this,0,replyIntent,PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_baseline_sms_24,"Reply",replyPendingIntent).addRemoteInput(remoteInput).build();
        Log.d("Glacier", "New notification before action");
        builder.addAction(action);
        Log.d("Glacier", "New notification after action");
        builder.setContentTitle("Glacier");
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentText(newMessage);
        Log.d("Glacier", "New notification after newMessage");
        builder.setSmallIcon(R.drawable.ic_baseline_sms_24);
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);
        NotificationCompat.Action action2 = new NotificationCompat.Action.Builder(R.drawable.ic_baseline_sms_24,"Reply",actionIntent).addRemoteInput(remoteInput2).build();
        builder.addAction(action2);
        managerCompat = NotificationManagerCompat.from(this);
        if(messageAuthor.length() > 5) {
            Log.d("Glacier", "New notification " + Integer.parseInt(messageAuthor.substring(2, 5)));
            //if()
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
        Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation(proxyNumber)+"------"+ConversationsManager.conversationsClient.getMyConversations().size());
        if(messagesAdapter == null) {
            Log.d("Glacier","New Message adapter");
            messagesAdapter = new SMSActivity.MessagesAdapter((OnSMSConversationClickListener) this);
            recyclerViewConversations.setAdapter(messagesAdapter);
            onBackendConnected();
        }else{
            Log.d("Glacier","old Message adapter");
            messagesAdapter.notifyDataSetChanged();
        }
        if(ConversationsManager.conversationsClient.getMyConversations().size() > 0) {
            List<Conversation> conversationList = ConversationsManager.conversationsClient.getMyConversations();
            Map<String, String> aList = new HashMap<>();
            for (Conversation conv : conversationList) {
                aList.put(conv.getFriendlyName(), conv.getSid());
            }
            model.setContConv(aList);
            model.setConversationsClient(ConversationsManager.conversationsClient);
//            messagesAdapter = new SMSActivity.MessagesAdapter((OnSMSConversationClickListener) this);
//            recyclerViewConversations.setAdapter(messagesAdapter);

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

                    messagesAdapter.remove(swipedSMSConversation.peek(),position);

                }
            };
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recyclerViewConversations);
            Log.d("Glacier","conversationList " +conversationList.size() + messagesAdapter);
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.GONE);
            Log.d("Glacier","conversationsClient emptyLayout " +conversationList.size() + emptyLayout.getVisibility());
        }else{
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.VISIBLE);
        }
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void notifyMessages(String newMessage,String messageAuthor) {
        notifyMessage(newMessage,messageAuthor);
    }

    private final ConversationsManager ConversationsManager = new ConversationsManager(this);
    ConversationModel model;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {
        if(xmppConnectionService != null) {
            SMSdbInfo info = xmppConnectionService.getSmsInfo();
//        Log.d()
            profileList.clear();
            proxyNumbers.clear();
            Log.d("Glacier", "unread_conv_count----" + ConversationsManager.unread_conv_count);
            for (SmsProfile smsProfile : info.getExistingProfs()) {
                smsProfile.setUnread_count(0);
                if (ConversationsManager.unread_conv_count == null || ConversationsManager.unread_conv_count.isEmpty()) {
                } else {
                    String number = smsProfile.getNumber().replaceAll(" ", "").replace("(", "").replace(")", "").replace("-", "");
                    Integer unread_count = ConversationsManager.unread_conv_count.get(number);
                    Log.d("Glacier", "unread_conv_count---- " + unread_count);
                    if (unread_count != null) {
                        smsProfile.setUnread_count(unread_count);
                    }
                }
                profileList.add(0, smsProfile);
                proxyNumbers.add(smsProfile.getNumber());
                adapter_sms.notifyItemInserted(0);
            }
            adapter_sms.notifyDataSetChanged();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_drawer_sms);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setTitle("SMS");
        toolbar = (Toolbar) findViewById(R.id.toolbar_sms_view);

        model = (ConversationModel) getApplicationContext();
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer_sms.openDrawer(GravityCompat.START);
            }
        });
        if(getIntent().hasExtra("account")) {
            identity = getIntent().getExtras().getString("account");
            //setTitle(identity);
            model.setIdentity(identity);
            Log.d("Glacier ","Twilio Conversation "+model.getConversation());
        }else{
            identity = model.getIdentity();
            model.setConversation(null);
            Log.d("Glacier","Identity "+identity);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Glacier", "Glacier", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

        }

        recyclerViewConversations = findViewById(R.id.choose_conversation_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewConversations.setLayoutManager(layoutManager);

        Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation(proxyNumber));
        ConversationsClient conversationsClient = model.getConversationsClient();

        ConversationsManager.setListener(this);
        Log.d("Glacier","Twilio ConversationsClient "+conversationsClient);
        if(conversationsClient != null){
            model.setConversation(null);
            Log.d("Glacier","proxyNumbers"+ proxyNumbers.toArray());
            ConversationsManager.addListenerLoadChannels(conversationsClient,proxyNumbers.toArray());
            if(model.getProxyNumber() == null || model.getProxyNumber().equals("") ) {
                model.setProxyNumber(proxyNumbers.get(0));
                proxyNumber = proxyNumbers.get(0);
            }else
                proxyNumber = model.getProxyNumber();
        }else{
            retrieveTokenFromServer();
        }


        recyclerViewSMS = (RecyclerView) findViewById(R.id.sms_recycler_view);
        layoutManagerSMS = new LinearLayoutManager(this);
        recyclerViewSMS.setLayoutManager(layoutManagerSMS);
        drawer_sms = (DrawerLayout) findViewById(R.id.drawer_layout_sms);
        adapter_sms = new SmsProfileAdapter((OnSMSProfileClickListener) this, profileList);

        recyclerViewSMS.setAdapter(adapter_sms);
        initSMS();
        Log.d("Glacier","identity sdns n "+identity);

        fab_contact = (FloatingActionButton) findViewById(R.id.fab_chat);
//        fab_group = (FloatingActionButton) findViewById(R.id.fab_group);
//        fab_add = (FloatingActionButton) findViewById(R.id.fab_add);
//        lyt_group = findViewById(R.id.lyt_mic);
//        lyt_chat = findViewById(R.id.lyt_call);
//        ViewAnimation.initShowOut(lyt_group);
//        ViewAnimation.initShowOut(lyt_chat);
        fab_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accessToken = Atoken.getAccessToken();
                Log.d("Glacier","conversationsClient "+ConversationsManager.conversationsClient);
                if (ConversationsManager.conversationsClient != null){
                    model.setConversationsClient(ConversationsManager.conversationsClient);
                    Intent intent = new Intent(mContext, ContactListActivity.class);
                    String conv_Sid = "new";
                    startActivity(intent.putExtra("conv_sid", conv_Sid).putExtra("identity", identity).putExtra("conversationToken", accessToken).putExtra("title", "New message"));
                }else{
                    Toast.makeText(mContext, "Please wait the SMS is not loaded successfully", Toast.LENGTH_SHORT).show();
                }
            }
        });
//        fab_group.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                model.setConversationsClient(ConversationsManager.conversationsClient);
//                Intent intent = new Intent(mContext, GroupSMS.class);
//                String token = Atoken.getAccessToken();
//                startActivity(intent.putExtra("identity",identity).putExtra("conversationToken",token));
//            }
//            });
//
//                fab_add.setOnClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            toggleFabMode(view);
//        }
//    });
}

        public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_group, menu);
        return super.onCreateOptionsMenu(menu);
    }

//    private void toggleFabMode(View v) {
//        rotate = ViewAnimation.rotateFab(v, !rotate);
//        if (rotate) {
//            ViewAnimation.showIn(lyt_group);
//            ViewAnimation.showIn(lyt_chat);
//            //back_drop.setVisibility(View.VISIBLE);
//        } else {
//            ViewAnimation.showOut(lyt_group);
//            ViewAnimation.showOut(lyt_chat);
//            //back_drop.setVisibility(View.GONE);
//        }
//    }
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("Glacier","menu_group_call_participants_list "+item.getItemId()+"======="+R.id.menu_group_call_participants_list);
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.sms_accounts:
                drawer_sms.openDrawer(GravityCompat.START);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //TODO remove only included to demo ui
    private void initSMS(){
        /*SmsProfile test = new SmsProfile("(999) 999-9999", "City1, State1");
        profileList.add(test);

        SmsProfile test2 = new SmsProfile("(000) 000-0000", "City2, State2");
        profileList.add(test2);*/
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
        Log.d("Glacier","onResume is called");
    }
    public void onRestart(){
        super.onRestart();
        Log.d("Glacier","onRestart is called");
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
                                model.setProxyNumber(ConversationsManager.proxyAddress[0].toString());
                                proxyNumber = ConversationsManager.proxyAddress[0].toString();
                            }else
                                proxyNumber = model.getProxyNumber();
                            model.setProxyNumber(ConversationsManager.proxyAddress);
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
        System.exit(0);
    }

    @Override
    public void OnSMSProfileClick(String id, String number, int color) {
        toolbar.setBackgroundColor(color);
        fab_contact.setBackgroundTintList(ColorStateList.valueOf(color));
        model.setProxyNumber(number);
        proxyNumber = number;
        drawer_sms.closeDrawers();
        messagesAdapter.notifyDataSetChanged();
    }

    public void checkEmptyView(){
        if(ConversationsManager.getConversation(proxyNumber).size() > 0){
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.GONE);
        } else {
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.VISIBLE);
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


        //final public Map<String,String> conversations = new HashMap<>();
//        ConversationsManager.conversationsClient.getMyConversations();

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
                //sortconv(conversations);
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
            Log.d("Glacier","IN message adapter");
        }
        public void setConversationClickListener(OnSMSConversationClickListener listener) {
            this.listener = listener;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent,
                                             int viewType) {
            View conView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sms_list_row, parent, false);
            return new ViewHolder(conView,listener);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            TextView conversation_name,sender_name,conversation_lastmsg,dateText,conv_Sid;
            RelativeLayout avatar_circle;
            com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView unreadcount;
            Conversation conversation = ConversationsManager.getConversation(proxyNumber).get(holder.getAdapterPosition());
            Map conv_last_msg = ConversationsManager.conv_last_msg;
            Map conv_last_msg_sent = ConversationsManager.conv_last_msg_sent;
            Log.d("Glacier","ConversationsManager "+conversation.getFriendlyName()+"----------"+conversation.getLastMessageDate());
            conversation_name = holder.conView.findViewById(R.id.conversation_name);
            avatar_circle = holder.conView.findViewById(R.id.avatar_circle);
            sender_name = holder.conView.findViewById(R.id.sender_name);
            conversation_lastmsg = holder.conView.findViewById(R.id.conversation_lastmsg);
            dateText = holder.conView.findViewById(R.id.conversation_lastupdate);
            String Contact_name = (cList != null && cList.get(conversation.getFriendlyName()) != null) ? cList.get(conversation.getFriendlyName()) : conversation.getFriendlyName();
            avatar_circle.setBackgroundTintList(ColorStateList.valueOf(UIHelper.getColorForSMS(Contact_name)));
            String sender_name_text = "";
            if(conv_last_msg_sent.containsKey(conversation.getSid()) && conv_last_msg_sent.get(conversation.getSid()).toString().equals(identity))
                sender_name_text = "Me :";
            else if(conv_last_msg_sent.containsKey(conversation.getSid()) && conv_last_msg_sent.get(conversation.getSid()) != null)
                sender_name_text = (CharSequence) conv_last_msg_sent.get(conversation.getSid()) +" :";
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
                                Log.d("Glacier ", "unreadcount 12344" + unreadcount + conversation.getFriendlyName() + " " + holder.getAdapterPosition() + " " + ConversationsManager.getConversation(proxyNumber).get(holder.getAdapterPosition()).getFriendlyName());
                                if (conversation.getFriendlyName().equals(ConversationsManager.getConversation(proxyNumber).get(holder.getAdapterPosition()).getFriendlyName())) {
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
                            if (conversation.getFriendlyName().equals(ConversationsManager.getConversation(proxyNumber).get(holder.getAdapterPosition()).getFriendlyName())) {
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
            Log.d("Glacier","conversationsClient getItemCount "+ConversationsManager.getConversation(proxyNumber).size());
            return ConversationsManager.getConversation(proxyNumber).size();
        }
        public void remove(Conversation conversation, int position) {
            ConversationsManager.getConversation(proxyNumber).remove(conversation);
            notifyItemRemoved(position);
            //TODO implement removal on conversation
            checkEmptyView();
            String contactName = (cList != null && cList.get(conversation.getFriendlyName()) != null) ? cList.get(conversation.getFriendlyName()) : conversation.getFriendlyName();

            Snackbar.make(recyclerViewConversations, contactName + ", DELETED.", Snackbar.LENGTH_LONG)

                    .setAction("Undo", new View.OnClickListener() {

                        @Override

                        public void onClick(View view) {

                            ConversationsManager.getConversation(proxyNumber).add(position, conversation);
                            notifyItemInserted(position);
                            checkEmptyView();
                        }

                    }).show();
        }

    }


}
interface OnSMSConversationClickListener {
    void OnSMSConversationClick(String connv_sid,String conv_name);
}
