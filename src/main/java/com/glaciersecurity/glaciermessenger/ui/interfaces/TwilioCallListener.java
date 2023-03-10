package com.glaciersecurity.glaciermessenger.ui.interfaces;

import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.Room;

import java.util.List;

public interface TwilioCallListener {
    void handleParticipantConnected(TwilioCallParticipant remoteCallParticipant);
    void handleParticipantDisconnected(RemoteParticipant remoteParticipant);

    //AM-558 moved to TwilioRemoteParticipantListener
    /*void handleAddRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    void handleRemoveRemoteParticipantVideo(RemoteVideoTrack videoTrack);
    void handleVideoTrackEnabled();
    void handleVideoTrackDisabled();*/

    void handleConnected(Room room);
    void handleReconnecting(boolean reconnecting);
    void handleConnectFailure();
    void endListening();

    LocalAudioTrack getLocalAudioTrack();
    LocalVideoTrack getLocalVideoTrack();
}
