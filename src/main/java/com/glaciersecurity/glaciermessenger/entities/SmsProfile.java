package com.glaciersecurity.glaciermessenger.entities;

import com.glaciersecurity.glaciermessenger.utils.UIHelper;

public class SmsProfile {

    protected String number;
    protected String location;
    final int color;

    public SmsProfile(String number, String location) {
        this.number = number;
        this.location = location;
        color = UIHelper.getColorForName(number + " " + location);
    }

    public String getNumber() {
        return number;
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
}
