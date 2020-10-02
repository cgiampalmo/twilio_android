package com.glaciersecurity.glaciermessenger.ui.interfaces;

import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.Room;

public interface TwilioCallListener {
    void handleParticipantConnected(RemoteParticipant remoteParticipant);
    void handleParticipantDisconnected(RemoteParticipant remoteParticipant);
    void handleAddRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    void handleRemoveRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    void handleVideoTrackEnabled();
    void handleVideoTrackDisabled();

    void handleConnected(Room room);
    void handleReconnecting(boolean reconnecting);
    void handleConnectFailure();
    void endListening();

    LocalAudioTrack getLocalAudioTrack();
    LocalVideoTrack getLocalVideoTrack();
}
