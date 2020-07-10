package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.Context;

import com.twilio.audioswitch.selection.AudioDeviceSelector;

import kotlin.Unit;

public class AudioDeviceManager {

    private static AudioDeviceManager instance;
    private AudioDeviceSelector audioDeviceSelector;

    private AudioDeviceManager(Context context) {
        audioDeviceSelector = new AudioDeviceSelector(context);
        audioDeviceSelector.start((audioDevices, audioDevice) -> Unit.INSTANCE); //AM-440
    }

    public static AudioDeviceManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioDeviceManager(context);
        }
        return instance;
    }

    public void selectorStart(){
        audioDeviceSelector.activate();
    }

    public void selectorEnd(){

    }

}
