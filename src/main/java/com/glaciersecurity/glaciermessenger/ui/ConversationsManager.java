package com.glaciersecurity.glaciermessenger.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.util.Attachment;
import com.google.gson.Gson;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationListener;
import com.twilio.conversations.ConversationsClient;
import com.twilio.conversations.ConversationsClientListener;
import com.twilio.conversations.ErrorInfo;
import com.twilio.conversations.Message;
import com.twilio.conversations.Participant;
import com.twilio.conversations.StatusListener;
import com.twilio.conversations.User;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.core.app.NotificationCompat;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

interface ConversationsManagerListener {
    void receivedNewMessage(String newMessage,String messageConversationSid,String messageAuthor);
    void messageSentCallback();
    void reloadMessages();
    void showList();
}

interface TokenResponseListener {
    void receivedTokenResponse(boolean success, @Nullable Exception exception,String token);
}

interface AccessTokenListener {
    void receivedAccessToken(@Nullable String token, @Nullable Exception exception);
}


public class ConversationsManager {

    // This is the unique name of the conversation  we are using
    private final static String DEFAULT_CONVERSATION_NAME = "general";

    final private ArrayList<Message> messages = new ArrayList<>();
    final public ArrayList<Conversation> conv_list = new ArrayList<>();
    final public Map<String,String> conv_last_msg = new HashMap<>();
    final public Map<String,String> conv_last_msg_sent = new HashMap<>();
    final public Map<String, Integer> conv_last_msg_count = new HashMap<>();

    public ConversationsClient conversationsClient;

    protected Conversation conversation;

    private ConversationsManagerListener conversationsManagerListener;

    private String tokenURL;

    private String conv_identity;

    private String conversationSid = "";
    private Context mContext;
    protected String proxyAddress;
    protected class TokenResponse {
        public String user_number;
        String token;
    }
    ConversationsManager(final Context context){
        mContext = context;
    }
    void retrieveAccessTokenFromServer(final Context context, String identity,
                                       final TokenResponseListener listener) {

        // Set the chat token URL in your strings.xml file
        String chatTokenURL = context.getString(R.string.chat_token_url);

        if ("https://YOUR_DOMAIN_HERE.twil.io/chat-token".equals(chatTokenURL)) {
            listener.receivedTokenResponse(false, new Exception("You need to replace the chat token URL in strings.xml"),"");
            return;
        }

        tokenURL = chatTokenURL;
        conv_identity = identity;
        new Thread(new Runnable() {
            @Override
            public void run() {
                retrieveToken(new AccessTokenListener() {
                    @Override
                    public void receivedAccessToken(@Nullable String token,
                                                    @Nullable Exception exception) {
                        if (token != null) {
                            Log.d("Glacier","Token create conversationsclient");
                            ConversationsClient.Properties props = ConversationsClient.Properties.newBuilder().createProperties();
                            ConversationsClient.create(context, token, props, mConversationsClientCallback);
                            listener.receivedTokenResponse(true,null,token);
                        } else {
                            listener.receivedTokenResponse(false, exception,token);
                        }
                    }
                });
            }
        }).start();
    }

    void initializeWithAccessToken(final Context context, final String token,final String convSid) {
        Log.d("Glacier","Token received "+token);
        this.conversationSid = convSid;
        ConversationsClient.Properties props = ConversationsClient.Properties.newBuilder().createProperties();
        ConversationsClient.create(context, token, props, mConversationsClientCallback);
    }

    private void retrieveToken(AccessTokenListener listener) {
        Log.d("Glacier","identiy "+conv_identity+" "+tokenURL+"----------"+mContext.getClass());
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("identity", conv_identity)
                .build();
        Request request = new Request.Builder()
                .url(tokenURL)
                .post(requestBody)
                .build();
        Log.d("Glacier","request "+request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Log.d("Glacier", "Response from server: " + responseBody);
            Gson gson = new Gson();
            TokenResponse tokenResponse = gson.fromJson(responseBody,TokenResponse.class);
            String accessToken = tokenResponse.token;
            this.proxyAddress = tokenResponse.user_number;
            Log.d("Glacier", "Retrieved access token from server: " + accessToken);
            listener.receivedAccessToken(accessToken, null);

        }
        catch (IOException ex) {
            Log.e("Glacier", ex.getLocalizedMessage(),ex);
            listener.receivedAccessToken(null, ex);
        }
    }

    void sendMessage(String messageBody) {
        Log.d("Glacier","Message created "+messageBody);
        if (conversation != null) {
            //Message.options().withMedia()
            Message.Options options = Message.options().withBody(messageBody);

            Log.d("Glacier","Message created");
            conversation.sendMessage(options, new CallbackListener<Message>() {
                @Override
                public void onSuccess(Message message) {
                    conversation.setAllMessagesRead(new CallbackListener<Long>() {
                        @Override
                        public void onSuccess(Long result) {

                        }
                    });
                    if (conversationsManagerListener != null) {
                        conversationsManagerListener.messageSentCallback();
                    }
                }
            });
        }
    }

    void sendMMSMessage(List<Attachment> attachments) {
        Log.d("Glacier","Message created "+attachments.size());
        if (conversation != null) {
            for (Iterator<Attachment> i = attachments.iterator(); i.hasNext(); i.remove()) {
                final Attachment attachment = i.next();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), attachment.getUri());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("Glacier","Exception occured "+e.getMessage());
                    return ;
                }
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, arrayOutputStream);
                InputStream is = new ByteArrayInputStream(arrayOutputStream.toByteArray());
                Message.Options options = Message.options().withMedia(is, attachment.getMime());
                conversation.sendMessage(options, new CallbackListener<Message>() {
                    @Override
                    public void onSuccess(Message message) {
                        conversation.setAllMessagesRead(new CallbackListener<Long>() {
                            @Override
                            public void onSuccess(Long result) {
                                Log.d("Glacier","onSuccess message sent");
                            }
                        });
                        if (conversationsManagerListener != null) {
                            conversationsManagerListener.messageSentCallback();
                        }
                    }
                });
            }
        }
    }

    protected void loadChannels(ConversationsClient conversationsClient) {
        ConversationsManager.this.conversationsClient = conversationsClient;
        Log.d("Glacier","conversationsClient "+conversationsClient.getMyConversations() + " conversationsClient size"+conversationsClient.getMyConversations().size());
        if (conversationsClient == null || conversationsClient.getMyConversations() == null) {
//            createConversation();
            return;
        }
        if(conversationsClient.getMyConversations().size() > 0) {
            Log.d("Glacier","conversationsClient "+conversationsClient.getMyConversations().get(0).getUniqueName()+"---"+conversationsClient.getMyConversations().get(0).getFriendlyName()+"----"+conversationsClient.getMyConversations().get(0).getSid());
            conv_list.addAll(conversationsClient.getMyConversations());
            Log.d("Glacier","conv_list "+conv_list +"---"+conv_list.size());
        //conversationsManagerListener.reloadMessages();
            for (int i = 0; i < conv_list.size(); i++) {
                getConversation(conversationsClient.getMyConversations().get(i).getSid(), true, conversationsClient);
            }
        }
        conversationsManagerListener.showList();
//        Log.d("Glacier","conv_list "+conv_list +"---"+conv_list.size());
        //getConversation(conversationsClient.getMyConversations().get(0).getSid());
    }

    protected void getConversation(String convSid, boolean lastmsg, ConversationsClient conversationsClient){
        ConversationsManager.this.conversationsClient = conversationsClient;
        conversationsClient.getConversation(convSid, new CallbackListener<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                if (conversation != null) {
                    if (conversation.getStatus() == Conversation.ConversationStatus.JOINED
                            || conversation.getStatus() == Conversation.ConversationStatus.NOT_PARTICIPATING) {
                        Log.d("Glacier", "Already Exists in Conversation: " + DEFAULT_CONVERSATION_NAME);
                        ConversationsManager.this.conversation = conversation;
                        ConversationsManager.this.conversation.addListener(mDefaultConversationListener);
                        if(!lastmsg) {
                            ConversationsManager.this.loadPreviousMessages(conversation);
                            return;
                        }
                        else {
                            loadlastmsg(conversation);
                            return;
                        }
                    } else {
                        Log.d("Glacier", "Joining Conversation: " + DEFAULT_CONVERSATION_NAME+" channel sid"+conversation.getSid());
                        joinConversation(conversation);
                    }
                    return;
                }
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.e("Glacier", "Error retrieving conversation: " + errorInfo.getMessage());
                createConversation(convSid);
            }

        });
    }
    private void createConversation(String convSid) {
        Log.d("Glacier", "Creating Conversation: " + DEFAULT_CONVERSATION_NAME);

        conversationsClient.createConversation(convSid,
                new CallbackListener<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        if (conversation != null) {
                            Log.d("Glacier", "Joining Conversation: " + DEFAULT_CONVERSATION_NAME);
                            joinConversation(conversation);
                            ConversationsManager.this.conversation = conversation;
                            conversation.setLastReadMessageIndex(0, new CallbackListener<Long>() {
                                @Override
                                public void onSuccess(Long result) {
                                    Log.d("Glacier","setUnreadCount "+result);
                                    conversationsManagerListener.showList();
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        Log.e("Glacier", "Error creating conversation: " + errorInfo.getMessage());
                    }
                });
    }


    protected void joinConversation(final Conversation conversation) {
        Log.d("Glacier", "Joining Conversation: " + conversation.getUniqueName());
        if (conversation.getStatus() == Conversation.ConversationStatus.JOINED) {

            ConversationsManager.this.conversation = conversation;
            Log.d("Glacier", "Already joined default conversation");
            ConversationsManager.this.conversation.addListener(mDefaultConversationListener);
            return;
        }


        conversation.join(new StatusListener() {
            @Override
            public void onSuccess() {
                ConversationsManager.this.conversation = conversation;
                Log.d("Glacier", "Joined default conversation" + conversation.getSid());
                ConversationsManager.this.conversation.addListener(mDefaultConversationListener);
                ConversationsManager.this.loadPreviousMessages(conversation);
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.e("Glacier", "Error joining conversation: " + errorInfo.getMessage());
            }
        });
    }

    protected void loadPreviousMessages(final Conversation conversation) {
        conversation.getLastMessages(100,
                new CallbackListener<List<Message>>() {
                    @Override
                    public void onSuccess(List<Message> result) {
                        messages.addAll(result);
                        if (conversationsManagerListener != null) {
                            conversationsManagerListener.reloadMessages();
                        }
                    }
                });
    }
    private void loadlastmsg (final Conversation conversation) {
        Log.d("Glacier","loadlastmsg "+conversation.getSynchronizationStatus()+" conversation "+conversation.getState()+" "+conversation.getStatus()+conversation.getFriendlyName());
        //if(conversation.getSynchronizationStatus().equals("ALL") || conversation.getSynchronizationStatus().equals("METADATA")) {
            try{
                conversation.getLastMessages(10,
                    new CallbackListener<List<Message>>() {
                        @Override
                        public void onSuccess(List<Message> result) {
                            if (conversationsManagerListener != null) {
                                if (result.size() > 0) {
                                    Log.d("Glacier", "loadlastmsg----" + result.get(result.size() - 1).getMessageBody() + " " + result.size());
                                    conv_last_msg.put(conversation.getSid(), result.get(result.size() - 1).getMessageBody());
                                    conv_last_msg_sent.put(conversation.getSid(), result.get(result.size() - 1).getAuthor());
                                    conversationsManagerListener.reloadMessages();
                                }
                            }
                        }
                    });
        }catch (Exception err){
                Log.d("Glacier","Synchronization Exception "+ err.getMessage());
            }
    }

    private final ConversationsClientListener mConversationsClientListener =
            new ConversationsClientListener() {

                @Override
                public void onConversationAdded(Conversation conversation) {

                }

                @Override
                public void onConversationUpdated(Conversation conversation, Conversation.UpdateReason updateReason) {

                }

                @Override
                public void onConversationDeleted(Conversation conversation) {

                }

                @Override
                public void onConversationSynchronizationChange(Conversation conversation) {
                    Log.d("Glacier","onConversationSynchronizationChange "+conversation);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    Log.d("Glacier","onConversationSynchronizationChange ErrorInfo "+errorInfo.getMessage());
                }

                @Override
                public void onUserUpdated(User user, User.UpdateReason updateReason) {

                }

                @Override
                public void onUserSubscribed(User user) {

                }

                @Override
                public void onUserUnsubscribed(User user) {

                }

                @Override
                public void onClientSynchronization(ConversationsClient.SynchronizationStatus synchronizationStatus) {
                    Log.d("Glacier","onClientSynchronization Completed "+conversationSid.trim()+"------"+(conversationSid.trim().equals(""))+conversationSid.isEmpty());
                    if (synchronizationStatus == ConversationsClient.SynchronizationStatus.COMPLETED && conversationSid.trim().equals("")) {
//                        startAdapter();
                        loadChannels(conversationsClient);
                    }else if(synchronizationStatus == ConversationsClient.SynchronizationStatus.COMPLETED){
                        getConversation(conversationSid,false,conversationsClient);
                    }
                }

                @Override
                public void onNewMessageNotification(String s, String s1, long l) {
                    Log.d("Glacier","onConversationSynchronizationChange "+s);
                }

                @Override
                public void onAddedToConversationNotification(String s) {

                }

                @Override
                public void onRemovedFromConversationNotification(String s) {

                }

                @Override
                public void onNotificationSubscribed() {

                }

                @Override
                public void onNotificationFailed(ErrorInfo errorInfo) {
                    Log.d("Glacier","onNotificationFailed");
                }

                @Override
                public void onConnectionStateChange(ConversationsClient.ConnectionState connectionState) {
                    Log.d("Glacier","ConversationsClient onConnectionStateChange");
                }

                @Override
                public void onTokenExpired() {
                    Log.d("Glacier","ConversationsClient onConversationSynchronizationChange onTokenExpired");
                    retrieveToken(new AccessTokenListener() {
                        @Override
                        public void receivedAccessToken(@Nullable String token, @Nullable Exception exception) {
                            if (token != null) {
                                conversationsClient.updateToken(token, new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("Glacier", "Refreshed access token.");
//                                        mContext.
//                                        TokenModel Atoken = new TokenModel();
//                                        Atoken.setAccessToken(token);
                                    }
                                });
                            }
                        }
                    });
                }

                @Override
                public void onTokenAboutToExpire() {
                    Log.d("Glacier","onTokenAboutToExpire "+tokenURL + "========"+conv_identity);
                    retrieveToken(new AccessTokenListener() {
                        @Override
                        public void receivedAccessToken(@Nullable String token, @Nullable Exception exception) {
                            if (token != null) {
                                conversationsClient.updateToken(token, new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d("Glacier", "Refreshed access token.");
//                                        TokenModel Atoken = new TokenModel();
//                                        Atoken.setAccessToken(token);
                                    }
                                });
                            }
                        }
                    });
                }
            };

    private final CallbackListener<ConversationsClient> mConversationsClientCallback =
            new CallbackListener<ConversationsClient>() {
                @Override
                public void onSuccess(ConversationsClient conversationsClient) {
                    ConversationsManager.this.conversationsClient = conversationsClient;
                    conversationsClient.addListener(ConversationsManager.this.mConversationsClientListener);
                    Log.d("Glacier", "Success creating Twilio Conversations Client");
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    Log.e("Glacier", "Error creating Twilio Conversations Client: " + errorInfo.getMessage());
                }
            };


    private final ConversationListener mDefaultConversationListener = new ConversationListener() {

        @Override
        public void onMessageAdded(final Message message) {
            Log.d("Glacier", "Message added"+message.getDateCreated()+message.getDateCreatedAsDate()+" "+mContext.getClass().toString()+mContext.getClass().toString().equals("class com.glaciersecurity.glaciermessenger.ui.SMSActivity"));
            messages.add(message);
            if (conversationsManagerListener != null) {
                conv_last_msg.replace(message.getConversationSid(),message.getMessageBody());
                conv_last_msg_sent.replace(message.getConversationSid(),message.getAuthor());
                if(message.getAuthor() == conv_identity){

                }else{
                    if(mContext.getClass().toString().equals("class com.glaciersecurity.glaciermessenger.ui.SMSActivity")){
                        Log.d("Glacier","Inside SMSActivity");
                        if(conv_last_msg_count.get(message.getConversationSid()) != null) {
                            conv_last_msg_count.replace(message.getConversationSid(), (conv_last_msg_count.get(message.getConversationSid()) + 1));
                        }else{
                            conv_last_msg_count.put(message.getConversationSid(),1);
                        }
                    }

                }

                conversationsManagerListener.receivedNewMessage("New sms from " + message.getAuthor() + " : " + message.getMessageBody(),message.getConversationSid(),message.getConversation().getFriendlyName());
                //conversationsManagerListener.reloadMessages();
                /*else {
                    conversationsManagerListener.reloadMessages();
                    conversation.setAllMessagesRead(new CallbackListener<Long>() {
                        @Override
                        public void onSuccess(Long result) {

                        }
                    });
                }*/
            }
        }

        @Override
        public void onMessageUpdated(Message message, Message.UpdateReason updateReason) {
            Log.d("Glacier", "Message updated: " + message.getMessageBody());
        }

        @Override
        public void onMessageDeleted(Message message) {
            Log.d("Glacier", "Message deleted");
        }

        @Override
        public void onParticipantAdded(Participant participant) {
            //conversation.addParticipantByIdentity();
            Log.d("Glacier", "Participant added: " + participant.getIdentity());
        }

        @Override
        public void onParticipantUpdated(Participant participant, Participant.UpdateReason updateReason) {
            Log.d("Glacier", "Participant updated: " + participant.getIdentity() + " " + updateReason.toString());
        }

        @Override
        public void onParticipantDeleted(Participant participant) {
            Log.d("Glacier", "Participant deleted: " + participant.getIdentity());
        }

        @Override
        public void onTypingStarted(Conversation conversation, Participant participant) {
            Log.d("Glacier", "Started Typing: " + participant.getIdentity());
        }

        @Override
        public void onTypingEnded(Conversation conversation, Participant participant) {
            Log.d("Glacier", "Ended Typing: " + participant.getIdentity());
        }

        @Override
        public void onSynchronizationChanged(Conversation conversation) {

        }
    };

    public ArrayList<Message> getMessages() {
        Log.d("Glacier","getMessages getConversation "+conv_list +"---------"+messages);
        return messages;
    }
    public ArrayList<Conversation> getConversation() {
        Log.d("Glacier","getConversation "+messages);
        return conv_list;
    }

    public void setListener(ConversationsManagerListener listener)  {
        this.conversationsManagerListener = listener;
    }
}

