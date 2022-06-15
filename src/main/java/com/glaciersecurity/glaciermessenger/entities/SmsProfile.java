package com.glaciersecurity.glaciermessenger.entities;

import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsProfile {

    protected String number;
    protected String location;
    protected String id;
    final int color;

    public SmsProfile(String number, String location) {
        this.number = number;
        this.location = location;
        color = UIHelper.getColorForSMS(number);
    }

    public SmsProfile(JSONObject jsmsinfo) throws JSONException, Exception {
        number = (String) jsmsinfo.get("text");
        number = Tools.reformatNumber(number);
        id = (String) jsmsinfo.get("id");
        color = UIHelper.getColorForSMS(jsmsinfo.toString());

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getColor(){
        return color;
    }

    public String toJsonString() {
        return "{\"color\":\"" + getColor() + "\"," +
                "\"number\":\"" + getNumber() + "\"," +
                "\"location\":\"" + getLocation() + "}" +
                "\"id\":\"" + getId() + "}" ;
    }

}
