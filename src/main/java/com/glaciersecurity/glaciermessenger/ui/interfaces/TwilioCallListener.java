package com.glaciersecurity.glaciermessenger.ui.interfaces;

import com.twilio.audioswitch.selection.AudioDevice;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.Room;

public interface TwilioCallListener {
    public void handleAddRemoteParticipant(RemoteParticipant remoteParticipant);
    public void handleAddRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    public void handleRemoveRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    public void handleVideoTrackEnabled();
    public void handleVideoTrackDisabled();

    public void handleConnected(Room room, AudioDevice audioDevice);
    public void handleReconnecting(boolean reconnecting);
    public void handleConnectFailure();
    public void endListening();

    public void handleSelectedAudioDevice(AudioDevice audioDevice);
}
