package com.glaciersecurity.glaciermessenger.utils;

import com.glaciersecurity.glaciermessenger.entities.TwilioCall;

//ALF AM-410
public interface TwilioCallListener {
    public void onCallSetupResponse(TwilioCall call);
    public void onCallAcceptResponse(TwilioCall call);
    public void onCallReceived(TwilioCall call);

    public void informUser(int r);
}
