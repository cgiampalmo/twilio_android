package com.glaciersecurity.glaciermessenger.ui;

import android.app.Application;
import android.app.NotificationManager;

import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationsClient;

import java.util.ArrayList;
import java.util.Map;

import androidx.core.app.NotificationManagerCompat;

public class ConversationModel extends Application {
    private Conversation conversation;
    private String identity;
    private ConversationsClient conversationsClient;
    private Map<String,String> contConv;
    private NotificationManagerCompat notificationManager;
    private ArrayList<ContactModel> arrayList;
    private String proxyNumber;

    public ArrayList<ContactModel> getGroupArrayList() {
        return groupArrayList;
    }

    public void setGroupArrayList(ArrayList<ContactModel> groupArrayList) {
        this.groupArrayList = groupArrayList;
    }

    private ArrayList<ContactModel> groupArrayList;

    public String getProxyNumber() {
        return proxyNumber;
    }

    public void setProxyNumber(String proxyNumber) {
        this.proxyNumber = proxyNumber;
    }

    public Map<String, String> getcList() {
        return cList;
    }

    public void setcList(Map<String, String> cList) {
        this.cList = cList;
    }

    private Map<String, String> cList;

    public ArrayList<ContactModel> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<ContactModel> arrayList) {
        this.arrayList = arrayList;
    }

    public void setNotificationManager(NotificationManagerCompat notificationManager){
        this.notificationManager = notificationManager;
    }
    public NotificationManagerCompat getNotificationManager(){
        return notificationManager;
    }
    public void clearNotification(int notId){
        notificationManager.cancel(notId);
    }
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
    public void cancelAllNotification(){
        if(notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
}
