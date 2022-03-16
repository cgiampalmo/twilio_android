package com.glaciersecurity.glaciermessenger.ui;

import android.app.Application;

import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;

import java.util.ArrayList;
import java.util.Map;

public class ConversationModel extends Application {
    private Conversation conversation;
    private String identity;
    private ConversationsClient conversationsClient;
    private Map<String,String> contConv;

    public Map<String, String> getContConv() {
        return contConv;
    }

    public void setContConv(Map<String, String> contConv) {
        this.contConv = contConv;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public ConversationsClient getConversationsClient() {
        return conversationsClient;
    }

    public void setConversationsClient(ConversationsClient conversationsClient) {
        this.conversationsClient = conversationsClient;
    }
}
