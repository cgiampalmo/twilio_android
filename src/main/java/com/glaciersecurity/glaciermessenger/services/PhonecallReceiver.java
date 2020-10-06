package com.glaciersecurity.glaciermessenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

//ALF AM-474
public class PhonecallReceiver extends BroadcastReceiver {
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;

    private PhonecallReceiverListener phonecallReceiverListener;

    public PhonecallReceiver(PhonecallReceiverListener phonecallReceiverListener) {
        super();
        this.phonecallReceiverListener = phonecallReceiverListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }
            onCallStateChanged(context, state, number);
        }
    }

    public interface PhonecallReceiverListener {
        void onIncomingNativeCallAnswered();
        void onIncomingNativeCallRinging();
    }

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        if(lastState == state){
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                if (phonecallReceiverListener != null) { //AM-498
                    phonecallReceiverListener.onIncomingNativeCallRinging();
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.
                if(lastState == TelephonyManager.CALL_STATE_RINGING){
                    //onIncomingCallAnswered(context);
                    if (phonecallReceiverListener != null) {
                        phonecallReceiverListener.onIncomingNativeCallAnswered();
                    }
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                break;
        }
        lastState = state;
    }
}


