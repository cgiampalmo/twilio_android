package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import com.glaciersecurity.glaciermessenger.R;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;
import com.twilio.conversations.Message;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class smsConvActivity extends AppCompatActivity implements ConversationsManagerListener, Serializable {

    public final static String TAG = "TwilioConversations";
    private String identity,convSid,Convtoken,newMessageBody;

    private MessagesAdapter messagesAdapter;

    private EditText writeMessageEditText;
    android.text.format.DateFormat df = new android.text.format.DateFormat();
    private final ConversationsManager ConversationsManager = new ConversationsManager(this);
    Toolbar toolbar;
    ConversationModel model;
    private Context mContext = this;
    RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_conv);
        model = (ConversationModel) getApplicationContext();
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        ConversationsManager.setListener(this);

        if(getIntent().hasExtra("conv_sid")) {
            convSid = getIntent().getExtras().getString("conv_sid");
        }
        if(getIntent().hasExtra("identity")){
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        ConversationsClient conversationsClient = model.getConversationsClient();
        Log.d("Glacier","Get conversation from model "+conversationsClient);
        if(conversationsClient != null){
            ConversationsManager.getConversation(convSid,false,conversationsClient);
        }else {
            ConversationsManager.initializeWithAccessToken(this, Convtoken,convSid);
        }
        recyclerView = findViewById(R.id.recycler_gchat);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        // for a chat app, show latest messages at the bottom
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        //messagesAdapter = new MessageListAdapter(this);
        messagesAdapter = new MessagesAdapter();
        recyclerView.setAdapter(messagesAdapter);

        writeMessageEditText = findViewById(R.id.edit_gchat_message);


        Button sendChatMessageButton = findViewById(R.id.button_gchat_send);
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"Button clicked "+writeMessageEditText.getText().toString());
                String messageBody = writeMessageEditText.getText().toString();
                if (messageBody.length() > 0) {
                    ConversationsManager.sendMessage(messageBody);
                }
            }
        });
        if(getIntent().hasExtra("title")){
            setTitle(getIntent().getStringExtra("title"));
        }
        if(getIntent().hasExtra("phoneNumber")){
            convSid = getIntent().getStringExtra("phoneNumber");
        }
        if(getIntent().hasExtra("messageBody")){
            newMessageBody = getIntent().getStringExtra("messageBody");
            ConversationsManager.sendMessage(newMessageBody);
            Log.d("Glacier "," To phoneNumber "+convSid+" sendedMessage "+newMessageBody);
        }
    }

    public void showList() {
        /*Intent SMSActivity = new Intent(getApplicationContext(), SMSActivity.class);
        startActivity(SMSActivity);*/
        Log.d("Glacier","ConversationsManager "+ConversationsManager.getConversation());
    }

    private void retrieveTokenFromServer() {
        ConversationsManager.retrieveAccessTokenFromServer(this, identity, new TokenResponseListener() {
            @Override
            public void receivedTokenResponse(boolean success, @org.jetbrains.annotations.Nullable Exception exception,String token) {
                if (success) {
                    //ConversationsManager.initializeWithAccessToken(this,)
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // need to modify user interface elements on the UI thread
//                            setTitle(identity);
                        }
                    });
                }
                else {
                    String errorMessage = getString(R.string.error_retrieving_access_token);
                    if (exception != null) {
                        errorMessage = errorMessage + " " + exception.getLocalizedMessage();
                    }
                    Log.d(TAG,"errorMessage "+errorMessage);
                    /*Toast.makeText(MainActivity.this,
                            errorMessage,
                            Toast.LENGTH_LONG)
                            .show();*/
                }
            }
        });
    }

    @Override
    public void receivedNewMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                messagesAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(ConversationsManager.getMessages().size()-1);
            }
        });
    }

    @Override
    public void reloadMessages() {
        if(convSid.trim().length() < 13 && convSid.length() > 9){
            ConversationsManager.sendMessage(newMessageBody);
            convSid = "Sent";
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                messagesAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void messageSentCallback() {
        smsConvActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // need to modify user interface elements on the UI thread
                writeMessageEditText.setText("");
            }
        });
    }

    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity));
    }

    class MessagesAdapter extends RecyclerView.Adapter {

        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        /*class ViewHolder extends RecyclerView.ViewHolder {

            final TextView messageTextView;

            ViewHolder(TextView textView) {
                super(textView);
                messageTextView = textView;
            }
        }*/

        MessagesAdapter() {

        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
            View view;
            /*TextView messageTextView = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_text_view, parent, false);
            return new ViewHolder(messageTextView);*/
            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.sms_chat_me, parent, false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.sms_chat_other, parent, false);
                return new MessagesAdapter.ReceivedMessageHolder(view);
            }

            return null;
        }
        public int getItemViewType(int position) {
            Message message = ConversationsManager.getMessages().get(position);

            if (message.getAuthor().equals(identity)) {
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Message message = ConversationsManager.getMessages().get(position);
            Message old_message = position == 0 ? ConversationsManager.getMessages().get(position) : ConversationsManager.getMessages().get(position - 1);
            boolean new_one = false;
            Log.d(TAG,"onBindViewHolder "+message.getAuthor()+"---"+message.getMessageBody()+"---"+message);
            String new_data = df.format("dd/MM/yyyy", message.getDateCreatedAsDate()).toString();
            String old_data = df.format("dd/MM/yyyy", old_message.getDateCreatedAsDate()).toString();

            if(!new_data.equals(old_data) || 0 == position){
                new_one = true;
            }
            Log.d(TAG,"onBindViewHolder "+new_data+"---"+old_data+"--"+new_data.equals(old_data)+"--"+((getItemCount()-1) != position)+"---"+getItemCount()+"==="+position+"-------"+new_one);
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message,new_one);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message,new_one);
                    break;
                default:
                    ((SentMessageHolder) holder).bind(message,new_one);
            }
        }

        @Override
        public int getItemCount() {
            return ConversationsManager.getMessages().size();
        }

        class SentMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText,dateText;

            SentMessageHolder(View itemView) {
                super(itemView);
                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_me);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_me);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_me);
            }

            void bind(Message message,boolean new_date) {

                Log.d(TAG,"onBindViewHolder "+message.getAuthor()+"-----------"+message.getMessageBody()+messageText+"------"+itemView);

                messageText.setText(message.getMessageBody());

                // Format the stored timestamp into a readable String using method.

                timeText.setText(df.format("hh:mm", message.getDateCreatedAsDate()).toString());
                if(new_date)
                    dateText.setText(df.format("MMM d", message.getDateCreatedAsDate()).toString());
                else
                    dateText.setVisibility(View.GONE);
                //timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            }
        }

        class ReceivedMessageHolder extends RecyclerView.ViewHolder {
            TextView messageText, timeText, nameText,dateText;
            ImageView profileImage;

            ReceivedMessageHolder(View itemView) {
                super(itemView);

                messageText = (TextView) itemView.findViewById(R.id.text_gchat_message_other);
                timeText = (TextView) itemView.findViewById(R.id.text_gchat_timestamp_other);
                nameText = (TextView) itemView.findViewById(R.id.text_gchat_user_other);
                profileImage = (ImageView) itemView.findViewById(R.id.image_gchat_profile_other);
                dateText = (TextView) itemView.findViewById(R.id.text_gchat_date_other);
            }

            void bind(Message message,boolean new_date) {
                messageText.setText(message.getMessageBody());

                // Format the stored timestamp into a readable String using method.
                timeText.setText(df.format("hh:mm", message.getDateCreatedAsDate()).toString());
                if(new_date)
                    dateText.setText(df.format("MMM d", message.getDateCreatedAsDate()).toString());
                else
                    dateText.setVisibility(View.GONE);
                nameText.setText(message.getAuthor());

                // Insert the profile image from the URL into the ImageView.
                //Utils.displayRoundImageFromUrl(mContext, message.getSender().getProfileUrl(), profileImage);
            }
        }
    }
}

