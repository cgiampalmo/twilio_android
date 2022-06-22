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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
    void notifyMessages(String newMessage,String messageAuthor);
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

    public static ConversationsClient conversationsClient;

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
        if( (tokenURL == null || conv_identity == null) &&  conversationsClient != null) {
            tokenURL = mContext.getString(R.string.chat_token_url);
            conv_identity = conversationsClient.getMyIdentity();
        }
        if(conv_identity != null && tokenURL != null) {
            Log.d("Glacier", "identiy " + conv_identity + " " + tokenURL + "----------" + mContext.getClass());
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new FormBody.Builder()
                    .add("identity", conv_identity)
                    .build();
            Request request = new Request.Builder()
                    .url(tokenURL)
                    .post(requestBody)
                    .build();
            Log.d("Glacier", "request " + request);
            try (Response response = client.newCall(request).execute()) {
                String responseBody = "";
                if (response != null && response.body() != null) {
                    responseBody = response.body().string();
                }
                Log.d("Glacier", "Response from server: " + responseBody);
                Gson gson = new Gson();
                TokenResponse tokenResponse = gson.fromJson(responseBody, TokenResponse.class);
                String accessToken = tokenResponse.token;
                this.proxyAddress = tokenResponse.user_number;
                Log.d("Glacier", "Retrieved access token from server: " + accessToken);
                listener.receivedAccessToken(accessToken, null);

            } catch (IOException ex) {
                Log.e("Glacier", ex.getLocalizedMessage(), ex);
                listener.receivedAccessToken(null, ex);
            }
        }else{
            // listener.receivedAccessToken(null, null);
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
                    /*int nh = (int) ( bitImg.getHeight() * (512.0 / bitImg.getWidth()) );
                    bitmap = bitImg.createScaledBitmap(bitImg, 512, nh, true);*/
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("Glacier","Exception occured "+e.getMessage());
                    return ;
                }
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                int nh = (int) ( bitmap.getHeight() * (512.0 / bitmap.getWidth()) );
                bitmap = bitmap.createScaledBitmap(bitmap, 512, nh, true);
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, arrayOutputStream);
                InputStream is = new ByteArrayInputStream(arrayOutputStream.toByteArray());
                Message.Options options = Message.options().withMedia(is, "image/bmp");
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

    protected void addListenerLoadChannels(final ConversationsClient conversationsClient){
        ConversationsManager.this.conversationsClient = conversationsClient;
        conversationsClient.removeAllListeners();
        conversationsClient.addListener(ConversationsManager.this.mConversationsClientListener);

        //loadChannels(conversationsClient);
    }

    protected void loadChannels(ConversationsClient conversationsClient) {
        Log.d("Glacier","conversationsClient "+conversationsClient.getMyConversations() + " conversationsClient size"+conversationsClient.getMyConversations().size());
        if (conversationsClient == null || conversationsClient.getMyConversations() == null) {
//            createConversation();
            return;
        }
        if(conversationsClient.getMyConversations().size() > 0) {
            conv_list.clear();
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
                            conversationsManagerListener.reloadMessages();
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
                    Log.d("Glacier", "onConversationAdded " + conversation.getCreatedBy());
                    if (conversation.getCreatedBy().equals("system")){
                        conversation.setLastReadMessageIndex(0, new CallbackListener<Long>() {
                            @Override
                            public void onSuccess(Long result) {
                                Log.d("Glacier", "setUnreadCount " + result);
                                conversation.setAllMessagesUnread(new CallbackListener<Long>() {
                                    @Override
                                    public void onSuccess(Long result) {
                                        Log.d("Glacier", "setUnreadCount " + result);
                                        loadChannels(conversationsClient);
                                        conversation.getLastMessages(10, new CallbackListener<List<Message>>() {

                                            @Override
                                            public void onSuccess(List<Message> result) {
                                                conversationsManagerListener.notifyMessages("New message from "+result.get(0).getAuthor() + " : "+result.get(0).getMessageBody(),result.get(0).getAuthor());
                                            }

                                            @Override
                                            public void onError(ErrorInfo errorInfo) {
                                                CallbackListener.super.onError(errorInfo);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                }

                @Override
                public void onConversationUpdated(Conversation conversation, Conversation.UpdateReason updateReason) {
                    Log.d("Glacier","onConversationUpdated "+conversation);
                }

                @Override
                public void onConversationDeleted(Conversation conversation) {
                    Log.d("Glacier","onConversationDeleted "+conversation);
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
                    Log.d("Glacier","onUserUpdated "+user);
                }

                @Override
                public void onUserSubscribed(User user) {
                    Log.d("Glacier","onUserSubscribed "+user);
                }

                @Override
                public void onUserUnsubscribed(User user) {
                    Log.d("Glacier","onUserUnsubscribed "+user);
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
                    Log.d("Glacier","onUserUnsubscribed "+s);
                }

                @Override
                public void onRemovedFromConversationNotification(String s) {
                    Log.d("Glacier","onUserUnsubscribed "+s);
                }

                @Override
                public void onNotificationSubscribed() {
                    Log.d("Glacier","onNotificationSubscribed");}

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
                    try {
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
                    }catch (Exception ex){
                        Log.d("Glacier","Execption "+ex.getMessage());
                    }
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
                if(message.getAuthor().equals(conv_identity)){

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

                conversationsManagerListener.receivedNewMessage("New sms from " + message.getAuthor() + " : " + message.getMessageBody(),message.getConversationSid(),message.getAuthor());
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
            Log.d("Glacier", "onSynchronizationChanged : " + conversation.getFriendlyName());
        }
    };

    public ArrayList<Message> getMessages() {
        Log.d("Glacier","getMessages getConversation "+conv_list +"---------"+messages);
        return messages;
    }
    public ArrayList<Conversation> getConversation() {
        sortconv(conv_list);
        return conv_list;
    }
    private void sortconv(List conversations){
        Collections.sort(conversations,new ConversationsManager.EventDetailSortByDate());
    }
    public void setListener(ConversationsManagerListener listener)  {
        ConversationsManager.this.conversationsManagerListener = listener;
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

