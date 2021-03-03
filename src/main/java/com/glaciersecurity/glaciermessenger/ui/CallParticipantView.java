package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioRemoteParticipantListener;
import com.makeramen.roundedimageview.RoundedImageView;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
//AM-558

public class CallParticipantView extends ConstraintLayout implements TwilioRemoteParticipantListener {
    private LinearLayout noVideoView;
    private RoundedImageView avatar;
    private String remoteParticipantIdentity;
    private VideoView primaryVideoView;
    private TwilioCallParticipant callParticipant;
    private AppCompatImageView audioMuted;

    public CallParticipantView(@NonNull Context context) {
        super(context);
        onFinishInflate();
    }

    public CallParticipantView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CallParticipantView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        primaryVideoView = findViewById(R.id.primary_video_view);
        noVideoView = findViewById(R.id.no_video_view);
        avatar = findViewById(R.id.no_video_view_avatar);
        avatar.setImageResource(R.drawable.avatar_default);
        audioMuted = findViewById(R.id.call_participant_audio_muted);
    }

    void setMirror(boolean mirror) {
        primaryVideoView.setMirror(mirror);
    }

    /*void setScalingType(@NonNull RendererCommon.ScalingType scalingType) {
        renderer.setScalingType(scalingType);
    }*/

    public void setCallParticipant(@NonNull TwilioCallParticipant participant) {
        callParticipant = participant;
        callParticipant.setRemoteParticipantListener(this);
        boolean participantChanged = remoteParticipantIdentity == null || !remoteParticipantIdentity.equals(participant.getRemoteParticipant().getIdentity());
        remoteParticipantIdentity = participant.getRemoteParticipant().getIdentity();
        //infoMode    = participant.getRecipient().isBlocked() || isMissingMediaKeys(participant);

        RemoteParticipant remoteParticipant = callParticipant.getRemoteParticipant();
        if (remoteParticipant.getRemoteVideoTracks().size() > 0) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);
            //AM-558b add avatar to TwilioCallParticipant
                avatar.setImageBitmap(participant.getAvatar());


            /*
             * Only render video tracks that are subscribed to
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                handleAddRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }

        if (remoteParticipant.getRemoteAudioTracks().size() > 0) {
            RemoteAudioTrackPublication remoteAudioTrackPublication =
                    remoteParticipant.getRemoteAudioTracks().get(0);
            if (remoteAudioTrackPublication.isTrackEnabled()) {
                handleAudioTrackEnabled();
            }
        }
    }

    public void cleanupCallParticipant() {
        RemoteParticipant remoteParticipant = callParticipant.getRemoteParticipant();
        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Remove video only if subscribed to participant track
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                handleRemoveRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }
    }

    public void handleAddRemoteParticipantVideo(RemoteVideoTrack videoTrack){
        primaryVideoView.setMirror(false);
        videoTrack.addRenderer(primaryVideoView);

        if (videoTrack.isEnabled()) { //AM-404
            handleVideoTrackEnabled();
        }
    }
    public void handleRemoveRemoteParticipantVideo(RemoteVideoTrack videoTrack){
        videoTrack.removeRenderer(primaryVideoView);
    }

    public void handleVideoTrackEnabled(){
        noVideoView.setVisibility(View.GONE);
        primaryVideoView.setVisibility(View.VISIBLE);
    }

    public void handleVideoTrackDisabled(){
        noVideoView.setVisibility(View.VISIBLE);
        primaryVideoView.setVisibility(View.GONE);
    }

    public void handleAudioTrackEnabled(){
        audioMuted.setVisibility(View.GONE);
    }

    public void handleAudioTrackDisabled(){
        audioMuted.setVisibility(View.VISIBLE);
    }
}
