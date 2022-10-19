package com.glaciersecurity.glaciermessenger.entities;

import android.content.res.ColorStateList;

import com.glaciersecurity.glaciermessenger.ui.SMSActivity;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.google.gson.internal.LinkedTreeMap;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsProfile {

    protected String number;
    protected String unformatted_number;
    protected String id;
    protected Integer unread_count;
    protected String nickname;

    public Integer getUnread_count() {
        return unread_count;
    }

    public void setUnread_count(Integer unread_count) {
        this.unread_count = unread_count;
    }

    public SmsProfile(LinkedTreeMap<String, String> linkedTreeMap) {
        unformatted_number = linkedTreeMap.get("text");
        number = Tools.reformatNumber(unformatted_number);
        id = linkedTreeMap.get("id");
        nickname = linkedTreeMap.get("nickname");

    }
    public SmsProfile(String number, String id){
        this.unformatted_number = number;
        this.number = Tools.reformatNumber(unformatted_number);
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public String getId() {
        return id;
    }

    public String getUnformattedNumber(){
        return unformatted_number;
    }


}
