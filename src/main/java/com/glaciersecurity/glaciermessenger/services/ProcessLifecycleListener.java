package com.glaciersecurity.glaciermessenger.services;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

//AM#14
public class ProcessLifecycleListener implements DefaultLifecycleObserver {

    //should only do this if 1) logged in, 2) Biometrics is turned on 3) with lockscreen
    //Maybe wait until then to instantiate?

    XmppConnectionService xmppConnectionService;

    public static enum LifecycleStatus {
        CREATE,
        DESTROY,
        PAUSE,
        RESUME,
        START,
        STOP
    }

    //biometricPrompt.authenticate(promptInfo)

    public ProcessLifecycleListener(XmppConnectionService service) {
        xmppConnectionService = service;
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onCreate(LifecycleOwner owner) {
        //
    }

    @Override
    public void onDestroy(LifecycleOwner owner) {
        //
    }

    @Override
    public void onPause(LifecycleOwner owner) {
        //
    }

    @Override
    public void onResume(LifecycleOwner owner) {
        //
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        xmppConnectionService.handleLifecycleUpdate(LifecycleStatus.START);
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        xmppConnectionService.handleLifecycleUpdate(LifecycleStatus.STOP);
    }
}
