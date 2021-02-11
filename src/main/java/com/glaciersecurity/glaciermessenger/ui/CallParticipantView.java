package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioRemoteParticipantListener;
import com.makeramen.roundedimageview.RoundedImageView;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
//AM-558

public class CallParticipantView extends ConstraintLayout implements TwilioRemoteParticipantListener {
    private LinearLayout noVideoView;
    private RoundedImageView avatar;
    private String remoteParticipantIdentity;
    private VideoView primaryVideoView;
    private TwilioCallParticipant callParticipant;

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

        /*backgroundAvatar = findViewById(R.id.call_participant_background_avatar);
        avatar           = findViewById(R.id.call_participant_item_avatar);
        pipAvatar        = findViewById(R.id.call_participant_item_pip_avatar);
        renderer         = findViewById(R.id.call_participant_renderer);
        audioMuted       = findViewById(R.id.call_participant_mic_muted);
        infoOverlay      = findViewById(R.id.call_participant_info_overlay);
        infoIcon         = findViewById(R.id.call_participant_info_icon);
        infoMessage      = findViewById(R.id.call_participant_info_message);
        infoMoreInfo     = findViewById(R.id.call_participant_info_more_info);

        avatar.setFallbackPhotoProvider(FALLBACK_PHOTO_PROVIDER);
        useLargeAvatar();*/
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

            /*
             * Only render video tracks that are subscribed to
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                handleAddRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }

        /*if (infoMode) {
            renderer.setVisibility(View.GONE);
            renderer.attachBroadcastVideoSink(null);
            audioMuted.setVisibility(View.GONE);
            avatar.setVisibility(View.GONE);
            pipAvatar.setVisibility(View.GONE);

            infoOverlay.setVisibility(View.VISIBLE);

            ImageViewCompat.setImageTintList(infoIcon, ContextCompat.getColorStateList(getContext(), R.color.core_white));

            if (participant.getRecipient().isBlocked()) {
                infoIcon.setImageResource(R.drawable.ic_block_tinted_24);
                infoMessage.setText(getContext().getString(R.string.CallParticipantView__s_is_blocked, participant.getRecipient().getShortDisplayName(getContext())));
                infoMoreInfo.setOnClickListener(v -> showBlockedDialog(participant.getRecipient()));
            } else {
                infoIcon.setImageResource(R.drawable.ic_error_solid_24);
                infoMessage.setText(getContext().getString(R.string.CallParticipantView__cant_receive_audio_video_from_s, participant.getRecipient().getShortDisplayName(getContext())));
                infoMoreInfo.setOnClickListener(v -> showNoMediaKeysDialog(participant.getRecipient()));
            }
        } else {
            infoOverlay.setVisibility(View.GONE);

            renderer.setVisibility(participant.isVideoEnabled() ? View.VISIBLE : View.GONE);

            if (participant.isVideoEnabled()) {
                if (participant.getVideoSink().getEglBase() != null) {
                    renderer.init(participant.getVideoSink().getEglBase());
                }
                renderer.attachBroadcastVideoSink(participant.getVideoSink());
            } else {
                renderer.attachBroadcastVideoSink(null);
            }

            audioMuted.setVisibility(participant.isMicrophoneEnabled() ? View.GONE : View.VISIBLE);
        }

        if (participantChanged || !Objects.equals(contactPhoto, participant.getRecipient().getContactPhoto())) {
            avatar.setAvatarUsingProfile(participant.getRecipient());
            AvatarUtil.loadBlurredIconIntoImageView(participant.getRecipient(), backgroundAvatar);
            setPipAvatar(participant.getRecipient());
            contactPhoto = participant.getRecipient().getContactPhoto();
        }*/
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

    /*public void handleParticipantConnected(TwilioCallParticipant remoteCallParticipant) {
        addRemoteParticipant(remoteCallParticipant);
        reconnectingProgressBar.setVisibility(View.GONE);
    }

    public void handleParticipantDisconnected(RemoteParticipant remoteParticipant) {
        if (!remoteParticipant.getIdentity().equals(remoteParticipantIdentity)) {
            return;
        }

        //Remove remote participant renderer
        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            //Remove video only if subscribed to participant track
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                removeParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }
    }*/

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
}
