package com.glaciersecurity.glaciermessenger.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

import com.glaciersecurity.glaciercore.api.IOpenVPNAPIService;
import com.glaciersecurity.glaciercore.api.IOpenVPNStatusCallback;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.twilio.video.Room;

public class CallConnectionService extends Service implements ServiceConnection, Handler.Callback {
    private static final int MSG_UPDATE_STATE = 0;
    private Room.State roomState = Room.State.DISCONNECTED;

    private Handler mHandler;


    @Override
    public void onCreate() {
        try {
            super.onCreate();
            mHandler = new Handler(this);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName className) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("GOOBER", "CallConnectionService::handleMessage(): " + msg.obj.toString() + "::What = " + msg.what);

        return true;
    }


}
