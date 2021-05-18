package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.glaciersecurity.glaciermessenger.R;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class SoundPoolManager {

    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private boolean outgoingCalled = false;  private float volume;
    private SoundPool soundPool;
    private int ringingSoundId;
    private int ringingStreamId;
    private int outgoingSoundId;
    private int disconnectSoundId;
    private int joingSoundId;

    //AM-441
    private boolean speaker = false;
    private int audioMode;

    Ringtone ringtone; //ALF AM-447
    Ringtone outgoingRingtone; //AM-588

    private static SoundPoolManager instance;

    //AM-475
    private AudioManager audioManager;
    private Vibrator vibrator;

    private SoundPoolManager(Context context) {
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        volume = actualVolume / maxVolume;

        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE); //AM-475

        //AM-447
        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ context.getPackageName() + "/" + R.raw.incoming_ring);
        //Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(context,soundUri);

        //AM-588
        Uri outgoingUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ context.getPackageName() + "/" + R.raw.outgoing);
        //Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        outgoingRingtone = RingtoneManager.getRingtone(context,outgoingUri);

        // Load the sounds
        int maxStreams = 2; //AM-446
        soundPool = new SoundPool.Builder()
                .setMaxStreams(maxStreams)
                .build();

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                if (playingCalled) {
                    playRinging();
                    playingCalled = false;
                }

                if(outgoingCalled){
                    playOutgoing();
                    outgoingCalled = false;
                }
            }

        });
        ringingSoundId = soundPool.load(context, R.raw.incoming_ring, 1);
        outgoingSoundId = soundPool.load(context, R.raw.outgoing, 1);
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
            vibrateIfNeeded(); //AM-475

            playing = true;
        } else {
            playingCalled = true;
        }
    }

    public void playOutgoing() {
        if (loaded && !playing) {
            //ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
            outgoingRingtone.play(); //AM-447
            vibrateIfNeeded(); //AM-475

            playing = true;
        } else {
            outgoingCalled = true;
        }
    }
    //AM-475
    public void vibrateIfNeeded() {
        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE ||
            audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            // Start without a delay, Vibrate for 1000 milliseconds, Sleep for 1000 milliseconds
            long[] pattern = {0, 1000, 1000};
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    public void stopRinging() {
        vibrator.cancel(); //AM-475
        if (playing) {
            soundPool.stop(ringingStreamId);
            if (ringtone != null) { //ALF AM-447, probably don't need above line anymore
                ringtone.stop();
            }

            if (outgoingRingtone != null) { //AM-588
                outgoingRingtone.stop();
            }
            playing = false;
        }
    }

    public void playDisconnect() {
        if (loaded && !playing) {
            soundPool.play(disconnectSoundId, 3, volume, 1, 0, 1f);
            playing = false;
        }
        setSpeakerOn(false); //AM-441
    }

    public void playJoin() {
        if (loaded && !playing) {
            soundPool.play(joingSoundId, 3, volume, 1, 0, 1f);
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
            soundPool.unload(outgoingSoundId);
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }

}
