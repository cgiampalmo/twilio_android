package com.glaciersecurity.glaciermessenger.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;

import androidx.annotation.NonNull;

import com.glaciersecurity.glaciermessenger.ui.VideoActivity;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.google.android.material.snackbar.Snackbar;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;

public class CallConnectionService extends Service implements ServiceConnection, Handler.Callback {
    private static final int MSG_UPDATE_STATE = 0;
    private Room.State roomState = Room.State.DISCONNECTED;
    private final IBinder binder = new CallConnectionBinder();
    private static final String TAG = "CallConnectionService";

    public class CallConnectionBinder extends Binder {
        public CallConnectionService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CallConnectionService.this;
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();

        //Register room / participant listeners

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //TODO do something useful
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        stopSelf();
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d("GOOBER", "CallConnectionService::handleMessage(): " + msg.obj.toString() + "::What = " + msg.what);

        return true;
    }


//    @SuppressLint("SetTextI18n")
//    private Room.Listener roomListener() {
//        return new Room.Listener() {
//            @Override
//            public void onConnected(Room room) {
//                localParticipant = room.getLocalParticipant();
//                setTitle(room.getName());
//                audioDeviceSelector.activate(); //AM-440
//                updateAudioDeviceIcon(audioDeviceSelector.getSelectedAudioDevice());
//
//                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
//                    addRemoteParticipant(remoteParticipant);
//                    String other = remoteParticipant.getIdentity();
//                    if (other.contains("@")){
//                        other = other.substring(0, other.indexOf("@"));
//                    }
//                    primaryTitle.setText(other);
//                    break;
//                }
//                if (!room.getRemoteParticipants().isEmpty()){
//                    reconnectingProgressBar.setVisibility(View.GONE);
//                }
//            }
//
//            @Override
//            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
//                reconnectingProgressBar.setVisibility(View.VISIBLE);
//            }
//
//            @Override
//            public void onReconnected(@NonNull Room room) {
//                if (!room.getRemoteParticipants().isEmpty()){
//                    reconnectingProgressBar.setVisibility(View.GONE);
//                }
//            }
//
//            @Override
//            public void onConnectFailure(Room room, TwilioException e) {
//                configureAudio(false);
//                audioDeviceSelector.deactivate(); //AM-440
//                intializeUI();
//            }
//
//            @Override
//            public void onDisconnected(Room room, TwilioException e) {
//                localParticipant = null;
//                VideoActivity.this.room = null;
//                configureAudio(false);
//                audioDeviceSelector.deactivate(); //AM-440
//                // Only reinitialize the UI if disconnect was not called from onDestroy()
//                if (!disconnectedFromOnDestroy) {
//                    intializeUI();
//                    //moveLocalVideoToPrimaryView(); //AM-450
//                }
//            }
//
//            @Override
//            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
//                addRemoteParticipant(remoteParticipant);
//                reconnectingProgressBar.setVisibility(View.GONE);
//
//            }
//
//            @Override
//            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
//                removeRemoteParticipant(remoteParticipant);
//                room.disconnect();
//                //CMG disconnect when remote leaves
//                localParticipant = null;
//                VideoActivity.this.room = null;
//                handleDisconnect(); //ALF AM-420
//                finish();
//            }
//
//            @Override
//            public void onRecordingStarted(Room room) {
//                /*
//                 * Indicates when media shared to a Room is being recorded. Note that
//                 * recording is only available in our Group Rooms developer preview.
//                 */
//                android.util.Log.d(TAG, "onRecordingStarted");
//            }
//
//            @Override
//            public void onRecordingStopped(Room room) {
//                /*
//                 * Indicates when media shared to a Room is no longer being recorded. Note that
//                 * recording is only available in our Group Rooms developer preview.
//                 */
//                android.util.Log.d(TAG, "onRecordingStopped");
//            }
//        };
//    }
//
//    @SuppressLint("SetTextI18n")
//    private RemoteParticipant.Listener remoteParticipantListener() {
//        return new RemoteParticipant.Listener() {
//            @Override
//            public void onAudioTrackPublished(RemoteParticipant remoteParticipant,
//                                              RemoteAudioTrackPublication remoteAudioTrackPublication) {
//                android.util.Log.i(TAG, String.format("onAudioTrackPublished: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
//                                "subscribed=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteAudioTrackPublication.getTrackSid(),
//                        remoteAudioTrackPublication.isTrackEnabled(),
//                        remoteAudioTrackPublication.isTrackSubscribed(),
//                        remoteAudioTrackPublication.getTrackName()));
//            }
//
//            @Override
//            public void onAudioTrackUnpublished(RemoteParticipant remoteParticipant,
//                                                RemoteAudioTrackPublication remoteAudioTrackPublication) {
//                android.util.Log.i(TAG, String.format("onAudioTrackUnpublished: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
//                                "subscribed=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteAudioTrackPublication.getTrackSid(),
//                        remoteAudioTrackPublication.isTrackEnabled(),
//                        remoteAudioTrackPublication.isTrackSubscribed(),
//                        remoteAudioTrackPublication.getTrackName()));
//            }
//
//            @Override
//            public void onDataTrackPublished(RemoteParticipant remoteParticipant,
//                                             RemoteDataTrackPublication remoteDataTrackPublication) {
//                android.util.Log.i(TAG, String.format("onDataTrackPublished: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
//                                "subscribed=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteDataTrackPublication.getTrackSid(),
//                        remoteDataTrackPublication.isTrackEnabled(),
//                        remoteDataTrackPublication.isTrackSubscribed(),
//                        remoteDataTrackPublication.getTrackName()));
//            }
//
//            @Override
//            public void onDataTrackUnpublished(RemoteParticipant remoteParticipant,
//                                               RemoteDataTrackPublication remoteDataTrackPublication) {
//                android.util.Log.i(TAG, String.format("onDataTrackUnpublished: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
//                                "subscribed=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteDataTrackPublication.getTrackSid(),
//                        remoteDataTrackPublication.isTrackEnabled(),
//                        remoteDataTrackPublication.isTrackSubscribed(),
//                        remoteDataTrackPublication.getTrackName()));
//            }
//
//            @Override
//            public void onVideoTrackPublished(RemoteParticipant remoteParticipant,
//                                              RemoteVideoTrackPublication remoteVideoTrackPublication) {
//                android.util.Log.i(TAG, String.format("onVideoTrackPublished: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
//                                "subscribed=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteVideoTrackPublication.getTrackSid(),
//                        remoteVideoTrackPublication.isTrackEnabled(),
//                        remoteVideoTrackPublication.isTrackSubscribed(),
//                        remoteVideoTrackPublication.getTrackName()));
//            }
//
//            @Override
//            public void onVideoTrackUnpublished(RemoteParticipant remoteParticipant,
//                                                RemoteVideoTrackPublication remoteVideoTrackPublication) {
//                android.util.Log.i(TAG, String.format("onVideoTrackUnpublished: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
//                                "subscribed=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteVideoTrackPublication.getTrackSid(),
//                        remoteVideoTrackPublication.isTrackEnabled(),
//                        remoteVideoTrackPublication.isTrackSubscribed(),
//                        remoteVideoTrackPublication.getTrackName()));
//            }
//
//            @Override
//            public void onAudioTrackSubscribed(RemoteParticipant remoteParticipant,
//                                               RemoteAudioTrackPublication remoteAudioTrackPublication,
//                                               RemoteAudioTrack remoteAudioTrack) {
//                android.util.Log.i(TAG, String.format("onAudioTrackSubscribed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteAudioTrack.isEnabled(),
//                        remoteAudioTrack.isPlaybackEnabled(),
//                        remoteAudioTrack.getName()));
//            }
//
//            @Override
//            public void onAudioTrackUnsubscribed(RemoteParticipant remoteParticipant,
//                                                 RemoteAudioTrackPublication remoteAudioTrackPublication,
//                                                 RemoteAudioTrack remoteAudioTrack) {
//                android.util.Log.i(TAG, String.format("onAudioTrackUnsubscribed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteAudioTrack.isEnabled(),
//                        remoteAudioTrack.isPlaybackEnabled(),
//                        remoteAudioTrack.getName()));
//            }
//
//            @Override
//            public void onAudioTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
//                                                       RemoteAudioTrackPublication remoteAudioTrackPublication,
//                                                       TwilioException twilioException) {
//                android.util.Log.i(TAG, String.format("onAudioTrackSubscriptionFailed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteAudioTrackPublication: sid=%b, name=%s]" +
//                                "[TwilioException: code=%d, message=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteAudioTrackPublication.getTrackSid(),
//                        remoteAudioTrackPublication.getTrackName(),
//                        twilioException.getCode(),
//                        twilioException.getMessage()));
//            }
//
//            @Override
//            public void onDataTrackSubscribed(RemoteParticipant remoteParticipant,
//                                              RemoteDataTrackPublication remoteDataTrackPublication,
//                                              RemoteDataTrack remoteDataTrack) {
//                android.util.Log.i(TAG, String.format("onDataTrackSubscribed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteDataTrack: enabled=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteDataTrack.isEnabled(),
//                        remoteDataTrack.getName()));
//            }
//
//            @Override
//            public void onDataTrackUnsubscribed(RemoteParticipant remoteParticipant,
//                                                RemoteDataTrackPublication remoteDataTrackPublication,
//                                                RemoteDataTrack remoteDataTrack) {
//                android.util.Log.i(TAG, String.format("onDataTrackUnsubscribed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteDataTrack: enabled=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteDataTrack.isEnabled(),
//                        remoteDataTrack.getName()));
//            }
//
//            @Override
//            public void onDataTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
//                                                      RemoteDataTrackPublication remoteDataTrackPublication,
//                                                      TwilioException twilioException) {
//                android.util.Log.i(TAG, String.format("onDataTrackSubscriptionFailed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteDataTrackPublication: sid=%b, name=%s]" +
//                                "[TwilioException: code=%d, message=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteDataTrackPublication.getTrackSid(),
//                        remoteDataTrackPublication.getTrackName(),
//                        twilioException.getCode(),
//                        twilioException.getMessage()));
//            }
//
//            @Override
//            public void onVideoTrackSubscribed(RemoteParticipant remoteParticipant,
//                                               RemoteVideoTrackPublication remoteVideoTrackPublication,
//                                               RemoteVideoTrack remoteVideoTrack) {
//                android.util.Log.i(TAG, String.format("onVideoTrackSubscribed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteVideoTrack: enabled=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteVideoTrack.isEnabled(),
//                        remoteVideoTrack.getName()));
//                addRemoteParticipantVideo(remoteVideoTrack);
//            }
//
//            @Override
//            public void onVideoTrackUnsubscribed(RemoteParticipant remoteParticipant,
//                                                 RemoteVideoTrackPublication remoteVideoTrackPublication,
//                                                 RemoteVideoTrack remoteVideoTrack) {
//                android.util.Log.i(TAG, String.format("onVideoTrackUnsubscribed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteVideoTrack: enabled=%b, name=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteVideoTrack.isEnabled(),
//                        remoteVideoTrack.getName()));
//                removeParticipantVideo(remoteVideoTrack);
//            }
//
//            @Override
//            public void onVideoTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
//                                                       RemoteVideoTrackPublication remoteVideoTrackPublication,
//                                                       TwilioException twilioException) {
//                android.util.Log.i(TAG, String.format("onVideoTrackSubscriptionFailed: " +
//                                "[RemoteParticipant: identity=%s], " +
//                                "[RemoteVideoTrackPublication: sid=%b, name=%s]" +
//                                "[TwilioException: code=%d, message=%s]",
//                        remoteParticipant.getIdentity(),
//                        remoteVideoTrackPublication.getTrackSid(),
//                        remoteVideoTrackPublication.getTrackName(),
//                        twilioException.getCode(),
//                        twilioException.getMessage()));
//                Snackbar.make(connectActionFab,
//                        String.format("Failed to subscribe to %s video track",
//                                remoteParticipant.getIdentity()),
//                        Snackbar.LENGTH_LONG)
//                        .show();
//            }
//
//            @Override
//            public void onAudioTrackEnabled(RemoteParticipant remoteParticipant,
//                                            RemoteAudioTrackPublication remoteAudioTrackPublication) {
//
//            }
//
//            @Override
//            public void onAudioTrackDisabled(RemoteParticipant remoteParticipant,
//                                             RemoteAudioTrackPublication remoteAudioTrackPublication) {
//
//            }
//
//            @Override
//            public void onVideoTrackEnabled(RemoteParticipant remoteParticipant,
//                                            RemoteVideoTrackPublication remoteVideoTrackPublication) {
//                noVideoView.setVisibility(View.GONE);
//                primaryVideoView.setVisibility(View.VISIBLE);
//            }
//
//            @Override
//            public void onVideoTrackDisabled(RemoteParticipant remoteParticipant,
//                                             RemoteVideoTrackPublication remoteVideoTrackPublication) {
//                noVideoView.setVisibility(View.VISIBLE);
//                primaryVideoView.setVisibility(View.GONE);
//            }
//        };
//    }


}
