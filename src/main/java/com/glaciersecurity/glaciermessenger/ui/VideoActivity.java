package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.Conversation;
//import com.glaciersecurity.glaciermessenger.services.CallConnectionService;
import com.glaciersecurity.glaciermessenger.services.CallManager;
import com.glaciersecurity.glaciermessenger.services.PhonecallReceiver;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioCallListener;
import com.glaciersecurity.glaciermessenger.ui.util.CameraCapturerCompat;
import com.glaciersecurity.glaciermessenger.ui.util.SoundPoolManager;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.makeramen.roundedimageview.RoundedImageView;
import com.twilio.audioswitch.selection.AudioDevice;
import com.twilio.audioswitch.selection.AudioDeviceSelector;
import com.twilio.video.AudioCodec;
import com.twilio.video.Camera2Capturer;
import com.twilio.video.CameraCapturer;
import com.twilio.video.CameraCapturer.CameraSource;
import com.twilio.video.ConnectOptions;
import com.twilio.video.EncodingParameters;
import com.twilio.video.G722Codec;
import com.twilio.video.H264Codec;
import com.twilio.video.IsacCodec;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.OpusCodec;
import com.twilio.video.PcmaCodec;
import com.twilio.video.PcmuCodec;
import com.twilio.video.RemoteAudioTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoCodec;
import com.twilio.video.VideoRenderer;
import com.twilio.video.VideoTrack;
import com.twilio.video.VideoView;
import com.twilio.video.Vp8Codec;
import com.twilio.video.Vp9Codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Unit; //AM-440


public class VideoActivity extends XmppActivity implements SensorEventListener, PhonecallReceiver.PhonecallReceiverListener, TwilioCallListener {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "VideoActivity";

    //CMG AM-419
    private SensorManager sensorManager;
    private Sensor proximity;
    private CallManager callManager;

    /*
     * Audio and video tracks can be created with names. This feature is useful for categorizing
     * tracks of participants. For example, if one participant publishes a video track with
     * ScreenCapturer and CameraCapturer with the names "screen" and "camera" respectively then
     * other participants can use RemoteVideoTrack#getName to determine which video track is
     * produced from the other participant's screen or camera.
     */
    private static final String LOCAL_AUDIO_TRACK_NAME = "mic";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";


    private boolean isSpeakerPhoneEnabled = false;
    private Boolean isAudioMuted = false;
    private Boolean isVideoMuted = true;

    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private VideoView primaryVideoView;
    private VideoView thumbnailVideoView;

    /*
     * Android shared preferences used for settings
     */
    private SharedPreferences preferences;

    /*
     * Android application UI elements
     */
    private CameraCapturerCompat cameraCapturerCompat;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private FloatingActionButton connectActionFab;
    private FloatingActionButton switchCameraActionFab;
    private View switchCameraActionSpace;
    private FloatingActionButton localVideoActionFab;
    private FloatingActionButton muteActionFab;
    private FloatingActionButton speakerPhoneActionFab;
    private RelativeLayout callBar;

    private LinearLayout reconnectingProgressBar;
    private LinearLayout noVideoView;
    private RoundedImageView avatar;
    private String remoteParticipantIdentity;
    private TextView primaryTitle;

    public static final String PREF_AUDIO_CODEC = "audio_codec";
    public static final String PREF_AUDIO_CODEC_DEFAULT = OpusCodec.NAME;
    public static final String PREF_VIDEO_CODEC = "video_codec";
    public static final String PREF_VIDEO_CODEC_DEFAULT = Vp8Codec.NAME;
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE = "sender_max_audio_bitrate";
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT = "0";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE = "sender_max_video_bitrate";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT = "0";
    public static final String PREF_VP8_SIMULCAST = "vp8_simulcast";
    public static final String PREF_ENABLE_AUTOMATIC_SUBSCRIPTION = "enable_automatic_subscription";
    public static final boolean PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT = true;
    public static final boolean PREF_VP8_SIMULCAST_DEFAULT = false;
    private static final String IS_AUDIO_MUTED = "IS_AUDIO_MUTED";
    private static final String IS_VIDEO_MUTED = "IS_VIDEO_MUTED";

    /*
     * Audio management
     */
    private int savedVolumeControlStream;

    private VideoRenderer localVideoView;
    private boolean disconnectedFromOnDestroy;
    private boolean enableAutomaticSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        primaryVideoView = findViewById(R.id.primary_video_view);
        thumbnailVideoView = findViewById(R.id.thumbnail_video_view);
        reconnectingProgressBar = findViewById(R.id.reconnecting_progress_bar_layout);
        noVideoView = findViewById(R.id.no_video_view);
        avatar = findViewById(R.id.no_video_view_avatar);
        avatar.setImageResource(R.drawable.avatar_default);


        connectActionFab = findViewById(R.id.connect_action_fab);
        switchCameraActionFab = findViewById(R.id.switch_camera_action_fab);
        switchCameraActionSpace = findViewById(R.id.switch_camera_action_space);

        localVideoActionFab = findViewById(R.id.local_video_action_fab);
        speakerPhoneActionFab = findViewById(R.id.speaker_phone_action_fab);
        muteActionFab = findViewById(R.id.mute_action_fab);
        this.primaryTitle  = findViewById(R.id.primary_video_title);
        callBar = findViewById(R.id.call_action_bar);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
            this.setShowWhenLocked(true);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*
         * Get shared preferences to read settings
         */
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState != null) {
            isAudioMuted = savedInstanceState.getBoolean(IS_AUDIO_MUTED);
            isVideoMuted = savedInstanceState.getBoolean(IS_VIDEO_MUTED);
        }

        /*
         * Setup audio management and set the volume control stream
         */
        savedVolumeControlStream = getVolumeControlStream();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);


        //CMG AM-419
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        createAudioAndVideoTracks();

        /*
         * Set the initial state of the UI
         */
        intializeUI();


    }


    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();

        if (CallActivity.ACTION_ACCEPTED_CALL.equals(action)) {
            //
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
            boolean cameraAndMicPermissionGranted = true;

            for (int grantResult : grantResults) {
                cameraAndMicPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }

            if (cameraAndMicPermissionGranted) {
                createAudioAndVideoTracks();
            } else {
                Toast.makeText(this,
                        R.string.permissions_needed,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();

        if(!isVideoMuted) {
            recreateVideoTrackIfNeeded();
        }

        /*
         * Route audio through cached value.
         */
        callManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        int muteIcon = !callManager.isMicrophoneMute() ?
                R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
        if (!callManager.isMicrophoneMute()){
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
        } else {
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        }
        muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                VideoActivity.this, muteIcon));

        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);

        /*
         * Update reconnecting UI
         */
        if (!room.getRemoteParticipants().isEmpty()){
            reconnectingProgressBar.setVisibility(View.GONE);
        }
    }

    private void recreateVideoTrackIfNeeded() {
        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
            localVideoTrack = LocalVideoTrack.create(this,
                    false,
                    cameraCapturerCompat.getVideoCapturer(),
                    LOCAL_VIDEO_TRACK_NAME);
            localVideoTrack.addRenderer(localVideoView);

            /*
             * If connected to a Room then share the local video track.
             */
            if (callManager != null) {
                callManager.publishVideoTrack(localVideoTrack);
            }
        }
    }

    @Override
    public void onPause() {
        /*
         * Release the local video track before going in the background. This ensures that the
         * camera can be used by other applications while this app is in the background.
         */
        //ALF AM-419 onPause gets called after open if the screen was locked causing video not to work
        //added remoteParticipantIdentity because this is set after the onPause call
        if (localVideoTrack != null && remoteParticipantIdentity != null) {
            /*
             * If this local video track is being shared in a Room, unpublish from room before
             * releasing the video track. Participants will be notified that the track has been
             * unpublished.
             */
            if (callManager != null) {
                callManager.unpublishVideoTrack(localVideoTrack);
            }

            localVideoTrack.release();
            localVideoTrack = null;
        }
        sensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(IS_AUDIO_MUTED, isAudioMuted);
        outState.putBoolean(IS_VIDEO_MUTED, isVideoMuted);
        super.onSaveInstanceState(outState);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        if (distance == 0f){
            callBar.setClickable(false);
            speakerPhoneActionFab.setClickable(false);
            switchCameraActionFab.setClickable(false);
            connectActionFab.setClickable(false);
            localVideoActionFab.setClickable(false);
            muteActionFab.setClickable(false);
        } else {
            callBar.setClickable(true);
            speakerPhoneActionFab.setClickable(true);
            switchCameraActionFab.setClickable(true);
            connectActionFab.setClickable(true);
            localVideoActionFab.setClickable(true);
            muteActionFab.setClickable(true);
        }
    }

    @Override
    protected void onDestroy() {

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null) {
            //localAudioTrack.enable(false); //ALF test
            localAudioTrack.release();
            localAudioTrack = null;
        }
        if (localVideoTrack != null) {
            //localVideoTrack.enable(false); //ALF test
            localVideoTrack.release();
            localVideoTrack = null;
        }

        //ALF AM-446
        setVolumeControlStream(savedVolumeControlStream);

        xmppConnectionService.getCallManager().setCallListener(null);

        super.onDestroy();
    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void createAudioAndVideoTracks() {
        // Share your microphone
        localAudioTrack = LocalAudioTrack.create(this, true, LOCAL_AUDIO_TRACK_NAME);

        // Share your camera
        if (cameraCapturerCompat == null) {
            cameraCapturerCompat = new CameraCapturerCompat(this, getAvailableCameraSource());
        }

        setupLocalVideoTrack();
    }

    private void setupLocalVideoTrack() {

        localVideoTrack = LocalVideoTrack.create(this,
                false,
                cameraCapturerCompat.getVideoCapturer(),
                LOCAL_VIDEO_TRACK_NAME);
        //AM-450 change local from primary to thumbnail
        localVideoTrack.addRenderer(thumbnailVideoView);
        localVideoView = thumbnailVideoView;
        thumbnailVideoView.setMirror(cameraCapturerCompat.getCameraSource() ==
                CameraSource.FRONT_CAMERA);
        callManager.setLocalVideo(localVideoTrack);
    }

    private CameraSource getAvailableCameraSource() {
        return (CameraCapturer.isSourceAvailable(CameraSource.FRONT_CAMERA)) ?
                (CameraSource.FRONT_CAMERA) :
                (CameraSource.BACK_CAMERA);
    }

    /*
     * The initial state when there is no active room.
     */
    private void intializeUI() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_end_white_24px));
        connectActionFab.show();
        connectActionFab.setOnClickListener(disconnectClickListener());
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
        speakerPhoneActionFab.show();
        speakerPhoneActionFab.setOnClickListener(speakerPhoneClickListener());

    }


    //AM-440 and next method
    /*
     * Show the current available audio devices.
     */
    private void showAudioDevices() {
        AudioDevice selectedDevice = audioDeviceSelector.getSelectedAudioDevice();
        List<AudioDevice> availableAudioDevices = audioDeviceSelector.getAvailableAudioDevices();

        if (selectedDevice != null) {
            int selectedDeviceIndex = availableAudioDevices.indexOf(selectedDevice);

            ArrayList<String> audioDeviceNames = new ArrayList<>();
            for (AudioDevice a : availableAudioDevices) {
                audioDeviceNames.add(a.getName());
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.room_screen_select_device)
                    .setSingleChoiceItems(
                            audioDeviceNames.toArray(new CharSequence[0]),
                            selectedDeviceIndex,
                            (dialog, index) -> {
                                dialog.dismiss();
                                AudioDevice selectedAudioDevice = availableAudioDevices.get(index);
                                updateAudioDeviceIcon(selectedAudioDevice);
                                audioDeviceSelector.selectDevice(selectedAudioDevice);
                            }).create().show();
        }
    }

    /*
     * Update the menu icon based on the currently selected audio device.
     */
    private void updateAudioDeviceIcon(AudioDevice selectedAudioDevice) {
        int audioDeviceIcon = R.drawable.ic_volume_off_gray_24dp;
        speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

        if (selectedAudioDevice instanceof AudioDevice.BluetoothHeadset) {
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
            audioDeviceIcon = R.drawable.ic_bluetooth_white_24dp;
        } else if (selectedAudioDevice instanceof AudioDevice.WiredHeadset) {
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
            audioDeviceIcon = R.drawable.ic_headset_mic_white_24dp;
        } else if (selectedAudioDevice instanceof AudioDevice.Earpiece) {
            audioDeviceIcon = R.drawable.ic_volume_off_gray_24dp;
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        } else if (selectedAudioDevice instanceof AudioDevice.Speakerphone) {
            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
            audioDeviceIcon = R.drawable.ic_volume_up_white_24dp;
        }


        speakerPhoneActionFab.setImageDrawable(
                ContextCompat.getDrawable(VideoActivity.this, audioDeviceIcon));
    }

    private View.OnClickListener disconnectClickListener() {
        return v -> {
            if (xmppConnectionService != null) {
                xmppConnectionService.getCallManager().disconnectCall();
            }
            finish();
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (cameraCapturerCompat != null) {
                CameraSource cameraSource = cameraCapturerCompat.getCameraSource();
                cameraCapturerCompat.switchCamera();
                if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
                    thumbnailVideoView.setMirror(cameraSource == CameraSource.BACK_CAMERA);
                } else {
                    primaryVideoView.setMirror(cameraSource == CameraSource.BACK_CAMERA);
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return v -> {
            /*
             * Enable/disable the local video track
             */
            if (localVideoTrack != null) {
                boolean enable = !localVideoTrack.isEnabled();
                localVideoTrack.enable(enable);
                int icon;
                if (enable) {
                    icon = R.drawable.ic_videocam_white_24dp;
                    localVideoActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                    switchCameraActionFab.show();
                    switchCameraActionSpace.setVisibility(View.VISIBLE);


                    enableSpeakerPhone(true);
                    recreateVideoTrackIfNeeded();
                    isVideoMuted = false;
                    thumbnailVideoView.setVisibility(View.VISIBLE);

                } else {
                    icon = R.drawable.ic_videocam_off_gray_24px;
                    localVideoActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    switchCameraActionFab.hide();
                    switchCameraActionSpace.setVisibility(View.GONE);
                    enableSpeakerPhone(false);
                    isVideoMuted = true;
                    thumbnailVideoView.setVisibility(View.GONE);
                }
                localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, icon));
            }
        };
    }

    private View.OnClickListener speakerPhoneClickListener() {
        return v -> {
            //AM-440
            List<AudioDevice> availableAudioDevices = xmppConnectionService.getCallManager().getAvailableAudioDevices();

            if (availableAudioDevices.size()>2){
                showAudioDevices();
            }else{
                boolean expectedSpeakerPhoneState = !callManager.isSpeakerphoneOn();
                callManager.setSpeakerphoneOn(expectedSpeakerPhoneState);
                isSpeakerPhoneEnabled = expectedSpeakerPhoneState;

                int icon;
                if (expectedSpeakerPhoneState) {
                    icon = R.drawable.ic_volume_up_white_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));


                } else {
                    icon = R.drawable.ic_volume_off_gray_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

                }
                speakerPhoneActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, icon));
            }
        };
    }

    private void enableSpeakerPhone(boolean expectedSpeakerPhoneState){
        if (audioManager != null) {
            //CMG AM-463
            if (audioDeviceSelector.getSelectedAudioDevice()  instanceof AudioDevice.BluetoothHeadset) {
            } else {
                audioManager.setSpeakerphoneOn(expectedSpeakerPhoneState);
                isSpeakerPhoneEnabled = expectedSpeakerPhoneState;

                int icon;
                if (expectedSpeakerPhoneState) {
                    icon = R.drawable.ic_volume_up_white_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));

                } else {
                    icon = R.drawable.ic_volume_off_gray_24dp;
                    speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

                }
                speakerPhoneActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, icon));
            }
        }

    }

    private View.OnClickListener muteClickListener() {
        return v -> {
            /*
             * Enable/disable the local audio track. The results of this operation are
             * signaled to other Participants in the same Room. When an audio track is
             * disabled, the audio is muted.
             */
            if (callManager != null) {
                boolean enable = callManager.handleMuteClick();
                int icon = enable ?
                        R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;

                if (enable){
                    muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                } else {
                    muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                }
                muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                        VideoActivity.this, icon));
            }
        };
    }

    @Override
    protected void onBackendConnected() {
        try {
            if (xmppConnectionService != null) {
                //getCallingActivity().
                //AM-478
                callManager = xmppConnectionService.getCallManager();
                callManager.setCallListener(this);
            }
        } catch (Exception e){

        }
    }


    public void refreshUiReal() {
    }

    @Override
    public void onBackPressed() {
    }

    //AM-478 start TwilioCallListener
    public void handleAddRemoteParticipant(RemoteParticipant remoteParticipant) {
        /*
         * This app only displays video for one additional participant per Room
         */
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            Snackbar.make(connectActionFab,
                    "Multiple participants are not currently support in this UI",
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        remoteParticipantIdentity = remoteParticipant.getIdentity();
        String other = remoteParticipant.getIdentity();
        if (other.contains("@")){
            other = other.substring(0, other.indexOf("@"));
        }
        primaryTitle.setText(other);
    }

    public void handleAddRemoteParticipantVideo(RemoteVideoTrack videoTrack){
        primaryVideoView.setMirror(false);
        videoTrack.addRenderer(primaryVideoView);
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

    public void handleConnected(Room room, AudioDevice audioDevice){
        setTitle(room.getName());
        //audioDeviceSelector.activate(); //AM-440
        updateAudioDeviceIcon(audioDevice);

        for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
            String other = remoteParticipant.getIdentity();
            if (other.contains("@")){
                other = other.substring(0, other.indexOf("@"));
            }
            primaryTitle.setText(other);
            break;
        }
        if (!room.getRemoteParticipants().isEmpty()){
            reconnectingProgressBar.setVisibility(View.GONE);
        }
    }

    public void handleReconnecting(boolean reconnecting){
        if (reconnecting) {
            reconnectingProgressBar.setVisibility(View.VISIBLE);
        } else {
            reconnectingProgressBar.setVisibility(View.GONE);
        }
    }

    public void handleConnectFailure(){
        intializeUI();
    }

    public void endListening(){
        callManager = null;
        finish();
    }

    public void handleSelectedAudioDevice(AudioDevice audioDevice) {
        updateAudioDeviceIcon(audioDevice);
    }
}
