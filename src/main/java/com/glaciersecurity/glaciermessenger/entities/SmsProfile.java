package com.glaciersecurity.glaciermessenger.entities;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.glaciersecurity.glaciermessenger.utils.UIHelper;
import com.google.gson.internal.LinkedTreeMap;
import java.util.Objects;

public class SmsProfile {

    protected String formattedNumber;
    protected String unformatted_number;
    protected String id;
    protected Integer unread_count;
    protected String nickname = "";
    protected Integer color;


    public Integer getUnread_count() {
        return unread_count;
    }

    public void setUnread_count(Integer unread_count) {
        this.unread_count = unread_count;
    }

    public SmsProfile(LinkedTreeMap<String, String> linkedTreeMap, String username) {
        unformatted_number = linkedTreeMap.get("text");
        formattedNumber = Tools.reformatNumber(unformatted_number);
        id = linkedTreeMap.get("id");
        nickname = linkedTreeMap.get("nickname");

    }
    public SmsProfile(String number, String id){
        this.unformatted_number = number;
        this.formattedNumber = Tools.reformatNumber(unformatted_number);
        this.id = id;
    }

    public String getFormattedNumber() {
        if (formattedNumber == null) {
            this.formattedNumber = Tools.reformatNumber(unformatted_number);
        }
        return formattedNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String name){
        this.nickname = name;
    }

    public int getColor() {
        if (color == null) {
            if (id != null || !id.equals("")) {
                this.color = UIHelper.getColorForSMS(id);
            } else {
                this.color = UIHelper.getColorForSMS(unformatted_number);
            }
        }
        return color;
    }

    public String getId() {
        return id;
    }

    public String getUnformattedNumber(){
        return unformatted_number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SmsProfile that = (SmsProfile) o;
        return Objects.equals(formattedNumber, that.formattedNumber) && Objects.equals(unformatted_number, that.unformatted_number) && Objects.equals(id, that.id);
    }

    public boolean equals(String id_, String num_) {
        if (id_ != null && id_.equals(id)){
            return true;
        }
        else if (num_ != null){
            if (num_.equals(formattedNumber) || num_.equals(unformatted_number)){
                return true;
            }
        }
        return false;
    }

    public boolean equals(String num_) {
        if (num_ != null){
            if (num_.equals(formattedNumber) || num_.equals(unformatted_number)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(formattedNumber, unformatted_number, id);
    }

}
