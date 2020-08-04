package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;

import com.glaciersecurity.glaciermessenger.R;

import static android.content.Context.AUDIO_SERVICE;

public class SoundPoolManager {

    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private float volume;
    private SoundPool soundPool;
    private int ringingSoundId;
    private int ringingStreamId;
    private int disconnectSoundId;
    private int joingSoundId;

    //AM-441
    private boolean speaker = false;
    private int audioMode;

    Ringtone ringtone; //ALF AM-447

    private static SoundPoolManager instance;

    private SoundPoolManager(Context context) {
        // AudioManager audio settings for adjusting the volume
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actualVolume / maxVolume;

        //AM-447
        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ context.getPackageName() + "/" + R.raw.outgoing_ring);
        //Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(context,soundUri);

        // Load the sounds
        int maxStreams = 2; //AM-446
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(maxStreams)
                    .build();
        } else {
            soundPool = new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
        }

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                if (playingCalled) {
                    playRinging();
                    playingCalled = false;
                }
            }

        });
        ringingSoundId = soundPool.load(context, R.raw.outgoing_ring, 1);
        disconnectSoundId = soundPool.load(context, R.raw.disconnect_end_call, 1);
        joingSoundId = soundPool.load(context, R.raw.join_call, 1);
    }

    public static SoundPoolManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundPoolManager(context);
        }
        return instance;
    }

    public void playRinging() {
        if (loaded && !playing) {
            //ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
            ringtone.play(); //AM-447

            playing = true;
        } else {
            playingCalled = true;
        }
    }

    public void stopRinging() {
        if (playing) {
            soundPool.stop(ringingStreamId);
            if (ringtone != null) { //ALF AM-447, probably don't need above line anymore
                ringtone.stop();
            }
            playing = false;
        }
    }

    public void playDisconnect() {
        if (loaded && !playing) {
            soundPool.play(disconnectSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
        setSpeakerOn(false); //AM-441
    }

    public void playJoin() {
        if (loaded && !playing) {
            soundPool.play(joingSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
    }

    //AM-441 (next four)
    public void setSpeakerOn(boolean on) {
        speaker = on;
    }

    public boolean getSpeakerOn() {
        return speaker;
    }

    public void setPreviousAudioMode(int mode) {
        audioMode = mode;
    }

    public int getPreviousAudioMode() {
        return audioMode;
    }

    public void release() {
        if (soundPool != null) {
            soundPool.unload(ringingSoundId);
            soundPool.unload(disconnectSoundId);
            soundPool.unload(joingSoundId);
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }

}
