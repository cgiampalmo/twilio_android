package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.Contact;
import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.services.CallManager;
import com.glaciersecurity.glaciermessenger.services.PhonecallReceiver;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.interfaces.TwilioCallListener;
import com.glaciersecurity.glaciermessenger.ui.util.CameraCapturerCompat;
import com.glaciersecurity.glaciermessenger.ui.util.SoundPoolManager;
import com.glaciersecurity.glaciermessenger.utils.Compatibility;
import com.glaciersecurity.glaciermessenger.utils.Log;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.makeramen.roundedimageview.RoundedImageView;
import com.twilio.audioswitch.AudioDevice;
import com.twilio.audioswitch.AudioSwitch;
import com.twilio.video.Camera2Capturer;
import com.twilio.video.CameraCapturer;
import com.twilio.video.EncodingParameters;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.Room;
import com.twilio.video.VideoDimensions;
import com.twilio.video.VideoFormat;
import com.twilio.video.VideoView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import kotlin.Unit; //AM-440
import com.glaciersecurity.glaciermessenger.xmpp.Jid;

//AM-650
import tvi.webrtc.Camera2Enumerator;
import tvi.webrtc.CameraEnumerator;
import tvi.webrtc.VideoSink;
import tvi.webrtc.Camera1Enumerator;


public class VideoActivity extends XmppActivity implements SensorEventListener, PhonecallReceiver.PhonecallReceiverListener, TwilioCallListener {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "VideoActivity";

    //CMG AM-419
    private SensorManager sensorManager;
    private Sensor proximity;
    private CallManager callManager;

    private static PowerManager.WakeLock wakeLock; //AM-561

    /*
     * Audio and video tracks can be created with names. This feature is useful for categorizing
     * tracks of participants. For example, if one participant publishes a video track with
     * ScreenCapturer and CameraCapturer with the names "screen" and "camera" respectively then
     * other participants can use RemoteVideoTrack#getName to determine which video track is
     * produced from the other participant's screen or camera.
     */
    private static final String LOCAL_AUDIO_TRACK_NAME = "mic";
    private static final String LOCAL_VIDEO_TRACK_NAME = "camera";

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private AudioManager audioManager;
    private Boolean isAudioMuted = false;
    private Boolean isVideoMuted = true;

    //AM-545
    private boolean endingCall = false;
    private Handler returningNotificationHandler;
    private boolean returning = false;

    /*
     * A VideoView receives frames from a local or remote video track and renders them
     * to an associated view.
     */
    private VideoView thumbnailVideoView;

    //AM-558
    private CallParticipantsLayout callParticipantsLayout;

    /*
     * Android application UI elements
     */
    private CameraCapturerCompat cameraCapturerCompat;
    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private ImageView connectActionFab;
    private ImageView switchCameraActionFab;
    private View switchCameraActionSpace;
    private RelativeLayout callView;
    private ImageView localVideoActionFab;
    private ImageView muteActionFab;
    private ImageView speakerPhoneActionFab;
    private RelativeLayout callBar;
    private int currentVideoIcon; //AM-404

    private LinearLayout reconnectingProgressBar;

    private String remoteParticipantIdentity;
    private TextView primaryTitle;
    private ImageButton minimizeVideo;
    private ImageButton addToCall; //AM-569

    private static final String IS_AUDIO_MUTED = "IS_AUDIO_MUTED";
    private static final String IS_VIDEO_MUTED = "IS_VIDEO_MUTED";
    private static final String IS_SPEAKERPHONE_ENABLED = "IS_SPEAKERPHONE_ENABLED";

    /*
     * Audio management
     */
    private int savedVolumeControlStream;

    private AudioFocusRequest focusRequest; //ALF AM-446

    //AM-650
    private VideoSink localVideoView;
    private CameraEnumerator camor;
    private String frontCam;
    private String rearCam;
    //private boolean disconnectedFromOnDestroy;

    private PhonecallReceiver phonecallReceiver; //ALF AM-474

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        thumbnailVideoView = findViewById(R.id.thumbnail_video_view);
        reconnectingProgressBar = findViewById(R.id.reconnecting_progress_bar_layout);

        phonecallReceiver = new PhonecallReceiver(this); //ALF AM-474

        connectActionFab = findViewById(R.id.connect_action_fab);
        switchCameraActionFab = findViewById(R.id.switch_camera_action_fab);
        switchCameraActionSpace = findViewById(R.id.switch_camera_action_space);
        callView = findViewById(R.id.call_view);
        localVideoActionFab = findViewById(R.id.local_video_action_fab);
        speakerPhoneActionFab = findViewById(R.id.speaker_phone_action_fab);
        muteActionFab = findViewById(R.id.mute_action_fab);
        this.primaryTitle  = findViewById(R.id.primary_video_title);
        callBar = findViewById(R.id.call_action_bar);
        this.currentVideoIcon = R.drawable.ic_videocam_off_gray_24px; //AM-404
        minimizeVideo = findViewById(R.id.down_arrow);
        minimizeVideo.setOnClickListener(minimizeCall());
        addToCall = findViewById(R.id.add_contact_to_call); //AM-569
        addToCall.setOnClickListener(addCallParticipant());

        //AM-545
        if (getIntent().getStringExtra("returning") != null) {
            returning = true;
        }

        //AM-558
        callParticipantsLayout = findViewById(R.id.call_screen_call_participants);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
            this.setShowWhenLocked(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            isAudioMuted = savedInstanceState.getBoolean(IS_AUDIO_MUTED);
            isVideoMuted = savedInstanceState.getBoolean(IS_VIDEO_MUTED);
        }

        /*
         * Setup audio management and set the volume control stream
         */
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        savedVolumeControlStream = getVolumeControlStream();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        //CMG AM-419
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        reconnectingProgressBar.setVisibility(View.VISIBLE); //AM-478 moved from onStart
        initializeProximityWakeLock(this); //AM-561

        createAudioAndVideoTracks();

        /*
         * Set the initial state of the UI
         */
        intializeUI();
    }

    //AM-561
    private void initializeProximityWakeLock(Context context) {
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager == null ? null : powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "glacier:twiliocalllock");
        wakeLock.setReferenceCounted(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        primaryTitle.setVisibility(View.GONE);
        //ALF AM-558
        final String title = intent.getStringExtra("roomtitle");
        if (title != null) {
            setTitle(title);
            primaryTitle.setText(title);
        }

        //final String title = intent.getStringExtra("roomtitle"); get conversation from title

        registerReceiver(phonecallReceiver, new IntentFilter("android.intent.action.PHONE_STATE")); //ALF AM-474
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

        recreateVideoTrackIfNeeded();
        if(!isVideoMuted) {
            if (currentVideoIcon == R.drawable.ic_videocam_off_gray_24px) {
                localVideoActionFab.callOnClick();
            } else {
                localVideoTrack.enable(true);
            }

            //localVideoTrack.enable(true);
        }

        //AM-478 changed from audioManager.isMicrophoneMute to isAudioMuted
        int muteIcon = !isAudioMuted ?
                R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
        if (!isAudioMuted){
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
        } else {
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        }
        muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                VideoActivity.this, muteIcon));

        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void recreateVideoTrackIfNeeded() {
        /*
         * If the local video track was released when the app was put in the background, recreate.
         */
        if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
            VideoFormat videoFormat = new VideoFormat(VideoDimensions.VGA_VIDEO_DIMENSIONS, 24);
            localVideoTrack = LocalVideoTrack.create(this,
                    false,
                    cameraCapturerCompat.getVideoCapturer(), videoFormat,
                    LOCAL_VIDEO_TRACK_NAME);
            localVideoTrack.addSink(localVideoView);

            /*
             * If connected to a Room then share the local video track.
             */
            if (callManager != null && callManager.getLocalParticipant() != null) {
                callManager.getLocalParticipant().publishTrack(localVideoTrack);
                callManager.getLocalParticipant().setEncodingParameters(callManager.getEncodingParameters());
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
        if (localVideoTrack != null && remoteParticipantIdentity != null && endingCall) {
            /*
             * If this local video track is being shared in a Room, unpublish from room before
             * releasing the video track. Participants will be notified that the track has been
             * unpublished.
             */
            if (callManager != null && callManager.getLocalParticipant() != null) {
                callManager.getLocalParticipant().unpublishTrack(localVideoTrack);
            }

            localVideoTrack.release();
            localVideoTrack = null;
        }
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        savedInstanceState.putBoolean(IS_AUDIO_MUTED, isAudioMuted);
        savedInstanceState.putBoolean(IS_VIDEO_MUTED, isVideoMuted);
        //savedInstanceState.putBoolean(IS_SPEAKERPHONE_ENABLED, isSpeakerPhoneEnabled);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        if (distance == 0f){
            if(!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            if(wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    protected void onDestroy() {

        /*
         * Release the local audio and video tracks ensuring any memory allocated to audio
         * or video is freed.
         */
        if (localAudioTrack != null && endingCall) { //AM-545 added minimizing and all places below
            localAudioTrack.release();
            localAudioTrack = null;
        }

        if (localVideoTrack != null && endingCall) {
            localVideoTrack.release();
            localVideoTrack = null;
        }

        unregisterReceiver(phonecallReceiver); //ALF AM-474

        // DJF - AM-512
        if (endingCall) {
            callManager.stopCallAudio(); //AM-581
            setVolumeControlStream(savedVolumeControlStream); //ALF AM-446

            isAudioMuted = false;
            isVideoMuted = true;
        } else { //AM-545
            //cleanup CallParticipant views when minimizing
            callParticipantsLayout.update(Collections.emptyList());
            if (localVideoTrack != null) {
                localVideoTrack.removeSink(thumbnailVideoView);
            }
        }

        //AM-545
        if (returningNotificationHandler != null) {
            returningNotificationHandler.removeCallbacksAndMessages(null);
        }

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

        //ALF add video format here
        VideoFormat videoFormat = new VideoFormat(VideoDimensions.VGA_VIDEO_DIMENSIONS, 24);

        localVideoTrack = LocalVideoTrack.create(this,
                false,
                cameraCapturerCompat.getVideoCapturer(), videoFormat,
                LOCAL_VIDEO_TRACK_NAME);
        //AM-450 change local from primary to thumbnail
        localVideoTrack.addSink(thumbnailVideoView);
        localVideoView = thumbnailVideoView;
        thumbnailVideoView.setMirror(cameraCapturerCompat.getCameraSource() ==
                frontCam); //changed for AM-650
    }

    private String getAvailableCameraSource() { //changed for AM-650
        if (camor == null) {
            if (Camera2Capturer.isSupported(this)) {
                camor = new Camera2Enumerator(this);
            } else {
                camor = new Camera1Enumerator();
            }
            String[] camSources = camor.getDeviceNames();
            for (String camSource : camSources) {
                if (camor.isFrontFacing(camSource)) {
                    frontCam = camSource;
                } else if (camor.isBackFacing(camSource)) {
                    rearCam = camSource;
                }
            }
        }
        return frontCam != null ? frontCam : rearCam;
        //return (CameraCapturer.isSourceAvailable(CameraSource.FRONT_CAMERA)) ?
                //(CameraSource.FRONT_CAMERA) :
                //(CameraSource.BACK_CAMERA);
    }

    /*
     * The initial state when there is no active room.
     */
    private void intializeUI() {
        connectActionFab.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_call_end_white_24px));
        connectActionFab.setVisibility(View.VISIBLE);
        connectActionFab.setOnClickListener(disconnectClickListener());
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.setVisibility(View.VISIBLE);
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.setVisibility(View.VISIBLE);
        muteActionFab.setOnClickListener(muteClickListener());
        speakerPhoneActionFab.setVisibility(View.VISIBLE);
        speakerPhoneActionFab.setOnClickListener(speakerPhoneClickListener());
    }


    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private void addRemoteParticipant(TwilioCallParticipant remoteCallParticipant) {
        callParticipantsLayout.update(callManager.getRemoteParticipants());

        //AM-484
        SoundPoolManager.getInstance(VideoActivity.this).playJoin();
    }

    /*
     * Called when remote participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private void removeRemoteParticipant(RemoteParticipant remoteParticipant) {
        //ALF AM-558
        callParticipantsLayout.update(callManager.getRemoteParticipants());
    }

    //AM-440 and next method
    /*
     * Show the current available audio devices.
     */
    private void showAudioDevices() {
        AudioDevice selectedDevice = callManager.getCallAudio().getSelectedAudioDevice();
        List<AudioDevice> availableAudioDevices = callManager.getCallAudio().getAvailableAudioDevices();

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
                                callManager.getCallAudio().selectDevice(selectedAudioDevice);
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
            /*
             * Disconnect from room
             */
            if (callManager != null) {
                callManager.handleDisconnect();
            }
            handleDisconnect(); //ALF AM-420
        };
    }

    //ALF AM-420
    @SuppressLint("WrongConstant")
    private void handleDisconnect() {
        endingCall = true;
        SoundPoolManager.getInstance(VideoActivity.this).playDisconnect();
        final Intent intent = new Intent(this, XmppConnectionService.class);
        intent.setAction(XmppConnectionService.ACTION_FINISH_CALL);
        Compatibility.startService(this, intent);

        endListening();
    }

    private View.OnClickListener minimizeCall() {
        return v -> {
            endingCall = false; //AM-545
            Intent chatsActivity = new Intent(getApplicationContext(), ConversationsActivity.class);
            startActivity(chatsActivity);
        };
    }

    //AM-569
    private View.OnClickListener addCallParticipant() {
        return v -> {
            endingCall = false; //AM-545

            String acct = xmppConnectionService.getAccounts().get(0).getJid().asBareJid().toEscapedString();
            Intent intent = ChooseContactActivity.createForCall(this, callManager.getRemoteParticipantIds(), acct);
            //intent.putExtra(ChooseContactActivity.EXTRA_CALL_ID, callid);
            startActivityForResult(intent, REQUEST_INVITE_TO_CALL);
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return v -> {
            if (cameraCapturerCompat != null) {
                String cameraSource = cameraCapturerCompat.getCameraSource();
                cameraCapturerCompat.switchCamera();
                if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
                    thumbnailVideoView.setMirror(cameraSource == rearCam);
                    thumbnailVideoView.bringToFront();
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
                if (enable) {
                    currentVideoIcon = R.drawable.ic_videocam_white_24dp;
                    localVideoActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                    switchCameraActionFab.setVisibility(View.VISIBLE);
                    switchCameraActionSpace.setVisibility(View.VISIBLE);


                    enableSpeakerPhone(true);
                    recreateVideoTrackIfNeeded();
                    isVideoMuted = false;
                    thumbnailVideoView.setVisibility(View.VISIBLE);

                } else {
                    currentVideoIcon = R.drawable.ic_videocam_off_gray_24px;
                    localVideoActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    switchCameraActionFab.setVisibility(View.GONE);
                    switchCameraActionSpace.setVisibility(View.GONE);
                    enableSpeakerPhone(false);
                    isVideoMuted = true;
                    thumbnailVideoView.setVisibility(View.GONE);
                }
                localVideoActionFab.setImageDrawable(
                        ContextCompat.getDrawable(VideoActivity.this, currentVideoIcon));
            }
        };
    }

    private View.OnClickListener speakerPhoneClickListener() {
        return v -> {
            //AM-440
            List<AudioDevice> availableAudioDevices = callManager.getCallAudio().getAvailableAudioDevices();
            if (availableAudioDevices.size()>2){
                showAudioDevices();
            }else{
                AudioDevice selected = callManager.getCallAudio().getSelectedAudioDevice();
                boolean expectedSpeakerPhoneState = !(selected instanceof AudioDevice.Speakerphone);

                //AM-581
                for (AudioDevice a : availableAudioDevices) {
                    if (expectedSpeakerPhoneState && a instanceof AudioDevice.Speakerphone) {
                        selected = a;
                    } else if (!expectedSpeakerPhoneState && a instanceof AudioDevice.Earpiece) {
                        selected = a;
                    }
                }
                callManager.getCallAudio().selectDevice(selected);

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
            AudioDevice selected = callManager.getCallAudio().getSelectedAudioDevice();
            if (selected instanceof AudioDevice.BluetoothHeadset ||
                    selected instanceof AudioDevice.WiredHeadset) {
                //do nothing
            } else {
                //AM-581
                List<AudioDevice> availableAudioDevices = callManager.getCallAudio().getAvailableAudioDevices();
                for (AudioDevice a : availableAudioDevices) {
                    if (expectedSpeakerPhoneState && a instanceof AudioDevice.Speakerphone) {
                        selected = a;
                    } else if (!expectedSpeakerPhoneState && a instanceof AudioDevice.Earpiece) {
                        selected = a;
                    }
                }
                callManager.getCallAudio().selectDevice(selected);

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
            if (localAudioTrack != null) {
                boolean enable = !localAudioTrack.isEnabled();
                localAudioTrack.enable(enable);
                isAudioMuted = !enable; //AM-404
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
            if (xmppConnectionService != null && callManager == null) {
                //AM-478
                callManager = xmppConnectionService.getCallManager();
                updateAudioDeviceIcon(callManager.getCallAudio().getSelectedAudioDevice());
                callManager.setCallListener(this);
                callManager.readyToConnect();
                boolean localAudioHandled = false; //AM-545

                //recreateVideoTrackIfNeeded();
                //AM-404
                if (callManager.getLocalParticipant() != null) {
                    //AM-545
                    //callManager.getLocalParticipant().publishTrack(localVideoTrack);
                    if (callManager.getLocalParticipant().getLocalVideoTracks().size() > 0) {
                        LocalVideoTrack lvTrack = callManager.getLocalParticipant().getLocalVideoTracks().get(0).getLocalVideoTrack();
                        boolean wasEnabled = lvTrack.isEnabled();
                        callManager.getLocalParticipant().unpublishTrack(lvTrack);
                        lvTrack.release();
                        callManager.getLocalParticipant().publishTrack(localVideoTrack);
                        if (wasEnabled) {
                            localVideoActionFab.callOnClick();
                        }
                    } else {
                        callManager.getLocalParticipant().publishTrack(localVideoTrack);
                    }

                    callManager.getLocalParticipant().setEncodingParameters(callManager.getEncodingParameters());

                    //AM-545
                    //callManager.getLocalParticipant().publishTrack(localAudioTrack);
                    if (callManager.getLocalParticipant().getLocalAudioTracks().size() > 0) {
                        localAudioTrack = callManager.getLocalParticipant().getLocalAudioTracks().get(0).getLocalAudioTrack();
                        isAudioMuted = !localAudioTrack.isEnabled();
                        int muteIcon = !isAudioMuted ?
                                R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
                        if (!isAudioMuted){
                            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
                        } else {
                            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                        }
                        muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                                VideoActivity.this, muteIcon));
                        localAudioHandled = true;
                    } else {
                        callManager.getLocalParticipant().publishTrack(localAudioTrack);
                    }
                }
                if (isAudioMuted && !localAudioHandled) { //AM-404
                    muteActionFab.callOnClick();
                }

                //AM-545
                if (returning) {
                    returningNotificationHandler = new Handler();
                    returningNotificationHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(callManager!= null) {
                                callParticipantsLayout.update(callManager.getRemoteParticipants());
                            }
                        }
                    }, 1000);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }


    public void refreshUiReal() {
    }

    @Override
    public void onBackPressed() {
    }

    //ALF AM-474
    @Override
    public void onIncomingNativeCallAnswered() {
        callManager.handleDisconnect();
        handleDisconnect();
    }

    View activityView;
    public Snackbar snackbar = null;

    //ALF AM-498
    @Override
    public void onIncomingNativeCallRinging(int call_act) {
        activityView = this.callView;
        if (call_act == 0) {
            if (snackbar != null) {
                snackbar.dismiss();
            }
        } else {
            if (activityView != null) {
                snackbar = Snackbar.make(activityView, R.string.native_ringing, Snackbar.LENGTH_INDEFINITE);

                View mView = snackbar.getView();
                TextView mTextView = (TextView) mView.findViewById(R.id.snackbar_text);
                mTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                mTextView.setBackgroundColor(getResources().getColor(R.color.blue_palette_hex1));
                mTextView.setTextColor(getResources().getColor(R.color.almost_black));

                snackbar.show();
            } else {
                Toast.makeText(this, R.string.native_ringing, Toast.LENGTH_LONG).show();
            }
        }
    }

    //AM-478 start TwilioCallListener
    public void handleParticipantConnected(TwilioCallParticipant remoteCallParticipant) {
        addRemoteParticipant(remoteCallParticipant);
        reconnectingProgressBar.setVisibility(View.GONE);
    }

    public void handleParticipantDisconnected(RemoteParticipant remoteParticipant) {
        //ALF AM-558 this shouldn't handle disconnect unless no other participants
        removeRemoteParticipant(remoteParticipant);

        if (callManager.getRemoteParticipants().size() == 0) { //ALF AM-558
            handleDisconnect(); //ALF AM-420
        }
    }

    public void handleConnected(Room room){
        setTitle(room.getName()); //ALF AM-558 should this be title sent with call data? Or doesn't matter?

        updateAudioDeviceIcon(callManager.getCallAudio().getSelectedAudioDevice());

        //AM-484
        if (!returning) {
            SoundPoolManager.getInstance(VideoActivity.this).playJoin();
        }

        //AM-558
        callParticipantsLayout.update(callManager.getRemoteParticipants());

        if (!room.getRemoteParticipants().isEmpty()){
            reconnectingProgressBar.setVisibility(View.GONE);
        }
        //AM-594
        if(!primaryTitle.getText().toString().startsWith("#")){
            setTitle(callManager.getRoomTitle());
            primaryTitle.setText(callManager.getRoomTitle());
        }
        primaryTitle.setVisibility(View.VISIBLE);

    }

    public void handleReconnecting(boolean reconnecting){
        if (reconnecting) {
            reconnectingProgressBar.setVisibility(View.VISIBLE);
        } else {
            reconnectingProgressBar.setVisibility(View.GONE);
        }
    }

    public void handleConnectFailure(){
        callManager.stopCallAudio();
        intializeUI();
    }

    public void endListening(){
        reconnectingProgressBar.setVisibility(View.GONE);
        callManager.stopCallAudio(); //AM-440
        if (callManager != null) { //AM-469
            callManager.setCallListener(null);
            //callManager = null;
        }
        finish();
    }

    public LocalAudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    public LocalVideoTrack getLocalVideoTrack() {
        return localVideoTrack;
    }
}