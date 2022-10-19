package com.glaciersecurity.glaciermessenger.entities;

import com.glaciersecurity.glaciermessenger.ui.SMSActivity;

import java.util.ArrayList;

public class SmsUserInfo {

    public boolean is_SMS_enabled;
    public ArrayList<SmsProfile> selected_twilio_numbers;
    public boolean allowUserToPurchase;

    public SmsUserInfo(boolean is_SMS_enabled, ArrayList<SmsProfile> selected_twilio_numbers, boolean allowUserToPurchase) {
        this.is_SMS_enabled = is_SMS_enabled;
        this.selected_twilio_numbers = selected_twilio_numbers;
        this.allowUserToPurchase = allowUserToPurchase;
    }

    public SmsUserInfo(String responseBody){
        //TODO
        String x = responseBody;
    }

    private ArrayList<SmsProfile> parseString_to_smsProfile(ArrayList<String> selected_twilio_numbers){
        ArrayList<SmsProfile> profs = new ArrayList<>();
        for (String raw_sms : selected_twilio_numbers){
            profs.add(new SmsProfile(raw_sms, raw_sms));
        }
        return profs;
    }
    public boolean is_SMS_enabled() {
        return is_SMS_enabled;
    }

    public void set_SMS_enabled(boolean is_SMS_enabled) {
        this.is_SMS_enabled = is_SMS_enabled;
    }

    public ArrayList<SmsProfile> getSelected_twilio_numbers() {
        return selected_twilio_numbers;
    }

    public void setSelected_twilio_numbers(ArrayList<SmsProfile> selected_twilio_numbers) {
        this.selected_twilio_numbers = selected_twilio_numbers;
    }

    public boolean isAllowUserToPurchase() {
        return allowUserToPurchase;
    }

    public void setAllowUserToPurchase(boolean allowUserToPurchase) {
        this.allowUserToPurchase = allowUserToPurchase;
    }

}
