package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glaciersecurity.glaciermessenger.R;
//import com.glaciersecurity.glaciermessenger.databinding.ActivityChooseContactBinding;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.ui.NewSMSActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;
import com.twilio.conversations.Message;
import com.twilio.conversations.Participant;

public class SMSActivity  extends AppCompatActivity implements ConversationsManagerListener,OnSMSConversationClickListener {
    private MessagesAdapter messagesAdapter;
    private String accessToken;
    private Context mContext = this;
    private Account account;
    private String identity;
    private String mSavedInstanceAccount;
    TokenModel Atoken = new TokenModel();
    public String AccessToken;
    RecyclerView recyclerView;
    @Override
    public void receivedNewMessage() {
        messagesAdapter.notifyDataSetChanged();
    }

    @Override
    public void messageSentCallback() {
        messagesAdapter.notifyDataSetChanged();
    }

    @Override
    public void reloadMessages() {
        messagesAdapter.notifyDataSetChanged();
    }
    public void showList() {
        Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation());

        List<Conversation> conversationList = ConversationsManager.conversationsClient.getMyConversations();
        Map<String, String> aList =new HashMap<>();
        for (Conversation conv:conversationList) {
            aList.put(conv.getFriendlyName(),conv.getSid());
        }
        model.setContConv(aList);
        sortconv(conversationList);
        messagesAdapter = new SMSActivity.MessagesAdapter((OnSMSConversationClickListener) this,conversationList);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        recyclerView.setAdapter(messagesAdapter);
    }
    private final ConversationsManager ConversationsManager = new ConversationsManager(this);
    Toolbar toolbar;
    ConversationModel model;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_sms1);
        setTitle("SMS");
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        model = (ConversationModel) getApplicationContext();
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        if(getIntent().hasExtra("account")) {
            identity = getIntent().getExtras().getString("account");
            model.setIdentity(identity);
            Log.d("Glacier ","Twilio Conversation "+model.getConversation());
        }else{
            identity = model.getIdentity();
            Log.d("Glacier","Identity "+identity);
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
    public void onResume(){
        super.onResume();
        Log.d("Glacier","onResum is called");
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
                            setTitle(identity);
                        }
                    });
                }
                else {
                    String errorMessage = getString(R.string.error_retrieving_access_token);
                    if (exception != null) {
                        errorMessage = errorMessage + " " + exception.getLocalizedMessage();
                    }
                    Log.d("Glacier","errorMessage "+errorMessage);
                    /*Toast.makeText(MainActivity.this,
                            errorMessage,
                            Toast.LENGTH_LONG)
                            .show();*/
                }
            }
        });

    }
    public void OnSMSConversationClick(String conv_sid,String conv_name) {
        Log.d("Glacier","OnSMSConversationClick called "+Atoken.getAccessToken());
        String token = Atoken.getAccessToken();
        Intent intent = new Intent(this,smsConvActivity.class);
        startActivity(intent.putExtra("conv_sid",conv_sid).putExtra("identity",identity).putExtra("conversationToken", token).putExtra("title","New message").putExtra("title",conv_name));
    }

    class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder>{
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        private OnSMSConversationClickListener listener;
        private List<Conversation> conversations;

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
                sortconv(conversations);
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
            conversations = conversationList;
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
            sortconv(conversations);
            Conversation conversation = conversations.get(position);
            Map conv_last_msg = ConversationsManager.conv_last_msg;
            Map conv_last_msg_sent = ConversationsManager.conv_last_msg_sent;
            Log.d("Glacier","ConversationsManager "+conversation.getFriendlyName()+"----------"+conversation.getLastMessageDate());
            conversation_name = holder.conView.findViewById(R.id.conversation_name);
            sender_name = holder.conView.findViewById(R.id.sender_name);
            conversation_lastmsg = holder.conView.findViewById(R.id.conversation_lastmsg);
            dateText = holder.conView.findViewById(R.id.conversation_lastupdate);
            conversation_name.setText(conversation.getFriendlyName());
            conversation_lastmsg.setText((CharSequence) conv_last_msg.get(conversation.getSid()));
            unreadcount = holder.conView.findViewById(R.id.unread_count);
            Log.d("Glacier ","unreadcount 12344"+unreadcount);
            conversation.getUnreadMessagesCount(new CallbackListener<Long>() {
                @Override
                public void onSuccess(Long result) {
                    Log.d("Glacier","setUnreadCount "+result);
                    if(result != null) {
                        if(result > 0) {
                            unreadcount.setVisibility(View.VISIBLE);
                            unreadcount.setUnreadCount(Math.toIntExact(result));
                        }
                        else{
                            unreadcount.setVisibility(View.GONE);
                        }
                    }
                    else{
                        /*conversation.setLastReadMessageIndex(0, new CallbackListener<Long>() {
                            @Override
                            public void onSuccess(Long result) {
                                Log.d("Glacier","setUnreadCount "+result);
                                if(result != null) {
                                    unreadcount.setUnreadCount(Math.toIntExact(result));
                                }
                            }
                        });*/
                    }
                    }
            });
            if(conv_last_msg_sent.containsKey(conversation.getSid()) && conv_last_msg_sent.get(conversation.getSid()).toString().equals(identity))
                sender_name.setText("Me :");
            else if(conv_last_msg_sent.containsKey(conversation.getSid()) && conv_last_msg_sent.get(conversation.getSid()) != null)
                sender_name.setText((CharSequence) conv_last_msg_sent.get(conversation.getSid()) +" :");
            else
                sender_name.setText("");

            if(conv_last_msg.get(conversation.getSid()) != null)
                dateText.setText(df.format("MMM d hh:mm", conversation.getLastMessageDate()).toString());
            else
                dateText.setText("");

            //String messageText = String.format(                dateText.setText(df.format("d hh:mm", conversation.getLastMessageDate()).toString());"%s: %s", conversation.getFriendlyName(), conversation.getSid());
            //holder.messageTextView.setText(messageText);
        }

        @Override
        public int getItemCount() {
            Log.d("Glacier","conversationsClient "+ConversationsManager.getConversation());
            return ConversationsManager.getConversation().size();
        }
    }
    private void sortconv(List conversations){
        Collections.sort(conversations,new EventDetailSortByDate());
    }
    private class EventDetailSortByDate implements java.util.Comparator<Conversation> {
        @Override
        public int compare(Conversation customerEvents1, Conversation customerEvents2) {
            Date DateObject1 = customerEvents1.getLastMessageDate();
            Date DateObject2 = customerEvents2.getLastMessageDate();
            if(DateObject1 == null || DateObject2 == null){
                Log.d("Glacier","DateObject1 "+DateObject1+" DateObject2 "+DateObject2+" other "+customerEvents1.getFriendlyName());
                return 1;
            }else{
                Calendar cal1 = Calendar.getInstance();
                cal1.setTime(DateObject1);
                Calendar cal2 = Calendar.getInstance();
                cal2.setTime(DateObject2);

                int month1 = cal1.get(Calendar.MONTH);
                int month2 = cal2.get(Calendar.MONTH);

                if (month1 < month2)
                    return -1;
                else if (month1 == month2)
                    return cal1.get(Calendar.DAY_OF_MONTH) - cal2.get(Calendar.DAY_OF_MONTH);

                else return 1;
            }
        }
    }
}
interface OnSMSConversationClickListener {
    void OnSMSConversationClick(String connv_sid,String conv_name);
}
