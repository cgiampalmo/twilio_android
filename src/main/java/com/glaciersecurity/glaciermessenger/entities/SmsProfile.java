package com.glaciersecurity.glaciermessenger.entities;

import android.content.res.ColorStateList;

import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsProfile {

    protected String number;
    protected String unformatted_number;
    protected String location;
    protected String id;
    protected Integer unread_count;

    public Integer getUnread_count() {
        return unread_count;
    }

    public void setUnread_count(Integer unread_count) {
        this.unread_count = unread_count;
    }


    public SmsProfile(JSONObject jsmsinfo) throws JSONException, Exception {
        unformatted_number = (String) jsmsinfo.get("text");
        number = Tools.reformatNumber(unformatted_number);
        id = (String) jsmsinfo.get("id");

        /*TODO pull location from db
        //hardcoded for now
        location = (String) jsmsinfo.get("location")
        */
        location = "City, State";
    }

    public String getNumber() {
        return number;
    }

    public String getId() {
        return id;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getUnformattedNumber(){
        return unformatted_number;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public String toJsonString() {
        return "{\"number\":\"" + getNumber() + "\"," +
                "\"location\":\"" + getLocation() + "}" +
                "\"id\":\"" + getId() + "}" ;
    }

}
