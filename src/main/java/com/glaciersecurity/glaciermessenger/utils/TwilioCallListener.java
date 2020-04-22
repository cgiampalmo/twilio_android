package com.glaciersecurity.glaciermessenger.utils;

import com.glaciersecurity.glaciermessenger.entities.TwilioCall;

//ALF AM-410
public interface TwilioCallListener {
    void onCallSetupResponse(TwilioCall call);
    void onCallAcceptResponse(TwilioCall call);
    void onCallReceived(TwilioCall call);

    void informUser(int r);
}
