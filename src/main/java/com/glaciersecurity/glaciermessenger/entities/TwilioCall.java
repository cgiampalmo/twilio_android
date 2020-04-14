package com.glaciersecurity.glaciermessenger.entities;

//ALF AM-410
public class TwilioCall {

    protected Account taccount;
    protected String tcaller;
    protected String treceiver;
    protected String ttoken;
    protected String troomname;
    protected String tstatus;
    protected int tcallid;

    public TwilioCall(Account account) {
        taccount = account;
    }

    public Account getAccount() {
        return taccount;
    }

    public String getCaller() {
        return tcaller;
    }

    public void setCaller(String caller) {
        tcaller = caller;
    }

    public String getReceiver() {
        return treceiver;
    }

    public void setReceiver(String receiver) {
        treceiver = receiver;
    }

    public String getToken() {
        return ttoken;
    }

    public void setToken(String token) {
        ttoken = token;
    }

    public String getRoomName() {
        return troomname;
    }

    public void setRoomName(String roomname) {
        troomname = roomname;
    }

    public String getStatus() {
        return tstatus;
    }

    public void setStatus(String status) {
        tstatus = status;
    }

    public int getCallId() {
        return tcallid;
    }

    public void setCallId(int callid) {
        tcallid = callid;
    }
}
