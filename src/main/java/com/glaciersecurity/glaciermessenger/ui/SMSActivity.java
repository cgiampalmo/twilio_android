package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glaciersecurity.glaciermessenger.R;
//import com.glaciersecurity.glaciermessenger.databinding.ActivityChooseContactBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.utils.LogoutListener;
import com.glaciersecurity.glaciermessenger.ui.NewSMSActivity;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;

public class SMSActivity  extends XmppActivity implements ConversationsManagerListener,OnSMSConversationClickListener, LogoutListener {
    private ActionBar actionBar;
    private Toolbar toolbar;
    private View nav_view_sms, main_content_sms;
    private boolean isHide = true;
    private int animation_duration = 250;

    private MessagesAdapter messagesAdapter;
    private String accessToken;
    private Context mContext = this;
    private Account account;
    private String identity;
    private String mSavedInstanceAccount;
    TokenModel Atoken = new TokenModel();
    public String AccessToken;
    RecyclerView recyclerView;
    private static final String KEY_TEXT_REPLY = "key_text_reply";
    private static final String MARK_AS_READ = "mark_as_read";
    NotificationManagerCompat managerCompat;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"Glacier");
    //ContactAdapter adapter;
    Map<String, String> cList =new HashMap<>();
    ArrayList<ContactModel> arrayList = new ArrayList<ContactModel>();
    @Override
    public void receivedNewMessage(String newMessage,String messageConversationSid,String messageAuthor) {
        messagesAdapter.notifyDataSetChanged();
        String checkIdentity = model.getIdentity();
        if(checkIdentity.equals(identity)) {
            Conversation current_conv = model.getConversation();
            Log.d("Glacier", "receivedNewMessage called----" + current_conv + "----" + messageConversationSid);
            if (current_conv != null)
                Log.d("Glacier", "Current Conversation new message " + current_conv.getSid() + " : " + messageConversationSid + " : " + current_conv.getSid().equals(messageConversationSid) + " : " + messageAuthor);
            if (current_conv == null)
                notifyMessage(newMessage, messageAuthor);
            else if (!identity.equals(messageAuthor)) {
                notifyMessage(newMessage, messageAuthor);
            }
        }
    }
    private void notifyMessage(String newMessage,String messageAuthor){
        Log.d("Glacier", "New notification notifyMessage called");
        Intent intent = new Intent(mContext, SMSActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Intent broadcastIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this,
                0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);


// Create the RemoteInput specifying this key
        Log.d("Glacier", "New notification before remoteInput");
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY).setLabel("Reply").build();
        RemoteInput remoteInput2 = new RemoteInput.Builder(MARK_AS_READ).setLabel("Mark as read").build();
        Intent replyIntent = new Intent(this,RemoteReceiver.class);
        Log.d("Glacier", "New notification before replyIntent");
        replyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent replyPendingIntent = PendingIntent.getActivity(this,0,replyIntent,PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_launcher,"Reply",replyPendingIntent).addRemoteInput(remoteInput).build();
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
                messagesAdapter.notifyDataSetChanged();
            }
        });

    }

    public void showList() {
        Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation()+"------"+ConversationsManager.conversationsClient.getMyConversations().size());
        if(ConversationsManager.conversationsClient.getMyConversations().size() > 0) {
            List<Conversation> conversationList = ConversationsManager.conversationsClient.getMyConversations();
            Map<String, String> aList = new HashMap<>();
            for (Conversation conv : conversationList) {
                aList.put(conv.getFriendlyName(), conv.getSid());
            }
            model.setContConv(aList);
            sortconv(conversationList);
            model.setConversationsClient(ConversationsManager.conversationsClient);
            messagesAdapter = new SMSActivity.MessagesAdapter((OnSMSConversationClickListener) this, conversationList);
            recyclerView.setAdapter(messagesAdapter);
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.GONE);
        }else{
            View emptyLayout = findViewById(R.id.empty_list);
            emptyLayout.setVisibility(View.VISIBLE);
        }
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }
    private final ConversationsManager ConversationsManager = new ConversationsManager(this);
    ConversationModel model;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_drawer_simple_dark);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setTitle("SMS");
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        model = (ConversationModel) getApplicationContext();
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        //Tools.setSystemBarColor(this);
        initNavigationMenu();
        if(getIntent().hasExtra("account")) {
            identity = getIntent().getExtras().getString("account");
            setTitle(identity);
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
        recyclerView = findViewById(R.id.choose_conversation_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);

        Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation());
        ConversationsClient conversationsClient = model.getConversationsClient();

        ConversationsManager.setListener(this);
        Log.d("Glacier","Twilio ConversationsClient "+conversationsClient);
        if(conversationsClient != null){
            model.setConversation(null);
            ConversationsManager.loadChannels(conversationsClient);
        }else{
            retrieveTokenFromServer();
        }
        Log.d("Glacier","identity sdns n "+identity);

        FloatingActionButton ContactNumber = findViewById(R.id.button_contact_sms);
        ContactNumber.setOnClickListener(new View.OnClickListener() {
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
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_group, menu);

//        getMenuInflater().inflate(R.menu.menu_navigation_drawer_news, menu);
//        Tools.changeMenuIconColor(menu, getResources().getColor(R.color.grey_40));
        return super.onCreateOptionsMenu(menu);
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("Glacier","menu_group_call_participants_list "+item.getItemId()+"======="+R.id.menu_group_call_participants_list);
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.group_add_sms:
                Intent intent = new Intent(mContext, GroupSMS.class);
                String token = Atoken.getAccessToken();
                startActivity(intent.putExtra("identity",identity).putExtra("conversationToken",token));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initNavigationMenu() {
        nav_view_sms = findViewById(R.id.nav_view_sms);
//        nav_view_sms.post(new Runnable() {
//            @Override
//            public void run() {
//                nav_view_sms.setTranslationX(-nav_view_sms.getWidth());
//            }
//        });

        main_content_sms = findViewById(R.id.main_content_sms);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isHide) {
//                    showMenu(nav_view_sms, main_content_sms);
//                } else {
//                    hideMenu(nav_view_sms, main_content_sms);
//                }
//            }
//        });

        new Handler(this.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {


               // showMenu(nav_view_sms, main_content_sms);
            }
        }, 1000);
    }

    public ObjectAnimator hideMenu(View menu_view, View content_view) {
        isHide = true;
        // menu animation
        ObjectAnimator animation = ObjectAnimator.ofFloat(menu_view, "translationX", -menu_view.getWidth());
        animation.setDuration(300);
        animation.start();

        // content animation
        ObjectAnimator animationContent = ObjectAnimator.ofFloat(content_view, "translationX", 0);
        animationContent.setDuration(300);
        animationContent.start();

        return animation;
    }

    public ObjectAnimator showMenu(View menu_view, View content_view) {
        isHide = false;
        ObjectAnimator animation = ObjectAnimator.ofFloat(menu_view, "translationX", 0);
        animation.setDuration(animation_duration);
        animation.start();

        // content animation
        ObjectAnimator animationContent = ObjectAnimator.ofFloat(content_view, "translationX", menu_view.getWidth());
        animationContent.setDuration(animation_duration);
        animationContent.start();

        return animation;
    }



//    @Override
//    public void onBackPressed() {
//        if(!isHide){
//            hideMenu(nav_view, main_content);
//        } else {
//            super.onBackPressed();
//        }
//    }

    public void onMenuClick(View view) {

        hideMenu(nav_view_sms, main_content_sms);
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
                            setTitle(identity);
                            model.setProxyNumber(ConversationsManager.proxyAddress);
                        }
                    });
                }
                else {
                    String errorMessage = getString(R.string.error_retrieving_access_token);
                    if (exception != null) {
                        errorMessage = errorMessage + " " + exception.getLocalizedMessage();
                    }
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

    class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder>{
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        private OnSMSConversationClickListener listener;
        private List<Conversation> conversations;
        Map<String, String> cList = model.getcList();
        //final public Map<String,String> conversations = new HashMap<>();
//        ConversationsManager.conversationsClient.getMyConversations();

        class ViewHolder extends RecyclerView.ViewHolder  implements View.OnClickListener {
            final View conView;
            //            public Activity unreadCount;
            private OnSMSConversationClickListener listener;

            ViewHolder(View con_view,OnSMSConversationClickListener listener) {
                super(con_view);
                this.listener = listener;
                conView = con_view;
                con_view.setOnClickListener(this);
            }
            public void onClick(View view) {
                //sortconv(conversations);
                Conversation conversation = conversations.get(getAdapterPosition());
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
        }


        MessagesAdapter(OnSMSConversationClickListener listener,List conversationList) {
            this.listener = listener;
            sortconv(conversationList);
            this.conversations = conversationList;
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
            com.glaciersecurity.glaciermessenger.ui.widget.UnreadCountCustomView unreadcount;
            Conversation conversation = conversations.get(holder.getAdapterPosition());
            Map conv_last_msg = ConversationsManager.conv_last_msg;
            Map conv_last_msg_sent = ConversationsManager.conv_last_msg_sent;
            Log.d("Glacier","ConversationsManager "+conversation.getFriendlyName()+"----------"+conversation.getLastMessageDate());
            conversation_name = holder.conView.findViewById(R.id.conversation_name);
            sender_name = holder.conView.findViewById(R.id.sender_name);
            conversation_lastmsg = holder.conView.findViewById(R.id.conversation_lastmsg);
            dateText = holder.conView.findViewById(R.id.conversation_lastupdate);
            String Contact_name = (cList != null && cList.get(conversation.getFriendlyName()) != null) ? cList.get(conversation.getFriendlyName()) : conversation.getFriendlyName();
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
            //Log.d("Glacier","setUnreadCount " + conversation.getFriendlyName()+" adapter position "+holder.getAdapterPosition());
            conversation.getUnreadMessagesCount(new CallbackListener<Long>() {
                @Override
                public void onSuccess(Long result) {
                    //Log.d("Glacier","setUnreadCount "+result +" "+conversation.getFriendlyName()+" adapter position "+holder.getAdapterPosition());
                    if(result != null) {
                        if(result > 0) {
                            if(holder.getAdapterPosition() > -1){
                                Log.d("Glacier ","unreadcount 12344"+unreadcount+conversation.getFriendlyName()+" "+holder.getAdapterPosition()+" "+conversations.get(holder.getAdapterPosition()).getFriendlyName());
                                if(conversation.getFriendlyName().equals(conversations.get(holder.getAdapterPosition()).getFriendlyName())) {
                                    unreadcount.setVisibility(View.VISIBLE);
                                    unreadcount.setUnreadCount(Math.toIntExact(result));
                                    conversation_lastmsg.setTypeface(Typeface.DEFAULT_BOLD);
                                }
                            }
                        }
                        else{

                        }
                    }
                    else{
                        //unreadcount.setVisibility(View.GONE);
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
            //Log.d("Glacier","conversationsClient "+ConversationsManager.getConversation());
            return ConversationsManager.getConversation().size();
        }
    }
    private void sortconv(List conversations){
        Collections.sort(conversations,new EventDetailSortByDate());
    }
    private class EventDetailSortByDate implements java.util.Comparator<Conversation> {
        @Override
        public int compare(Conversation customerEvents1, Conversation customerEvents2) {
            Date DateObject1 = (customerEvents1.getLastMessageDate() == null)?customerEvents1.getDateCreatedAsDate():customerEvents1.getLastMessageDate();
            Date DateObject2 = (customerEvents2.getLastMessageDate() == null)?customerEvents2.getDateCreatedAsDate():customerEvents2.getLastMessageDate();;
            //Log.d("Glacier","DateObject1 "+DateObject1+" DateObject2 "+DateObject2+" other "+customerEvents1.getLastMessageDate()+" "+customerEvents2.getLastMessageDate());
            if(DateObject1 == null || DateObject2 == null){
                return 1;
            }else{
                Calendar cal1 = Calendar.getInstance();
                cal1.setTime(DateObject1);
                Calendar cal2 = Calendar.getInstance();
                cal2.setTime(DateObject2);

                int month1 = cal1.get(Calendar.MONTH);
                int month2 = cal2.get(Calendar.MONTH);
                //Log.d("Glacier","month1 "+month1+" month2 "+month2+" other "+customerEvents1.getFriendlyName());

                if (month1 < month2){
                    //Log.d("Glacier","Inside DAY_OF_MONTH1 "+cal1.get(Calendar.HOUR_OF_DAY)+" DAY_OF_MONTH2 "+cal2.get(Calendar.HOUR_OF_DAY)+" other "+customerEvents1.getFriendlyName()+" returning "+(cal1.get(Calendar.HOUR_OF_DAY) - cal2.get(Calendar.HOUR_OF_DAY)));
                    return -1;
                }
                else if (month1 == month2) {
                    //Log.d("Glacier","DAY_OF_MONTH1 "+cal1.get(Calendar.HOUR_OF_DAY)+" DAY_OF_MONTH2 "+cal2.get(Calendar.HOUR_OF_DAY)+" other "+customerEvents1.getFriendlyName()+" returning "+(cal1.get(Calendar.HOUR_OF_DAY) - cal2.get(Calendar.HOUR_OF_DAY)));
                    if(cal1.get(Calendar.DAY_OF_MONTH) != cal2.get(Calendar.DAY_OF_MONTH)) {
                        return cal1.get(Calendar.DAY_OF_MONTH) - cal2.get(Calendar.DAY_OF_MONTH);
                    }
                    else if(cal1.get(Calendar.HOUR_OF_DAY) != cal2.get(Calendar.HOUR_OF_DAY)) {
                        int returning = (cal1.get(Calendar.HOUR_OF_DAY) - cal2.get(Calendar.HOUR_OF_DAY)) > 0 ? 0 : -1;
                        //Log.d("Glacier","DAY_OF_MONTH1 "+cal1.get(Calendar.HOUR_OF_DAY)+" DAY_OF_MONTH2 "+cal2.get(Calendar.HOUR_OF_DAY)+" other "+customerEvents1.getFriendlyName()+" returning "+returning);
                        return returning;
                    }
                    else if(cal1.get(Calendar.AM_PM) != cal2.get(Calendar.AM_PM))
                        return cal1.get(Calendar.AM_PM) - cal2.get(Calendar.AM_PM);
                    else if(cal1.get(Calendar.MINUTE) != cal2.get(Calendar.MINUTE))
                        return cal1.get(Calendar.MINUTE) - cal2.get(Calendar.MINUTE);
                    else
                        return cal1.get(Calendar.SECOND) - cal2.get(Calendar.SECOND);
                }
                else return -1;
            }
        }
    }
}
interface OnSMSConversationClickListener {
    void OnSMSConversationClick(String connv_sid,String conv_name);
}
