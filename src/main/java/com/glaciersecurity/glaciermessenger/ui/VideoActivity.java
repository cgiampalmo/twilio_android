package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.glaciersecurity.glaciermessenger.services.PhonecallReceiver;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
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


public class VideoActivity extends XmppActivity implements SensorEventListener, PhonecallReceiver.PhonecallReceiverListener {
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "VideoActivity";

    //CMG AM-419
    private SensorManager sensorManager;
    private Sensor proximity;

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
     * Access token used to connect. This field will be set either from the console generated token
     * or the request to the token server.
     */
    private String accessToken;

    /*
     * A Room represents communication between a local participant and one or more participants.
     */
    private Room room;
    private LocalParticipant localParticipant;
    private AudioManager audioManager;
    //    private MenuItem turnSpeakerOnMenuItem;
//    private MenuItem turnSpeakerOffMenuItem;
    private int previousAudioMode;
    private boolean previousMicrophoneMute;
    private boolean isSpeakerPhoneEnabled = false;
    private Boolean isAudioMuted = false;
    private Boolean isVideoMuted = true;


    /*
     * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
     * video.
     */
    private AudioCodec audioCodec;
    private VideoCodec videoCodec;

    /*
     * Encoding parameters represent the sender side bandwidth constraints.
     */
    private EncodingParameters encodingParameters;

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
    private AlertDialog connectDialog;
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
    private AudioDeviceSelector audioDeviceSelector; //AM-440 Audio device mgmt

    private AudioFocusRequest focusRequest; //ALF AM-446

    private VideoRenderer localVideoView;
    private boolean disconnectedFromOnDestroy;
    private boolean enableAutomaticSubscription;

    private PhonecallReceiver phonecallReceiver; //ALF AM-474

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

        phonecallReceiver = new PhonecallReceiver(this); //ALF AM-474


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
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);
        savedVolumeControlStream = getVolumeControlStream();
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        //AM-440 Setup audio device management
        audioDeviceSelector = new AudioDeviceSelector(getApplicationContext());

        //CMG AM-419
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        reconnectingProgressBar.setVisibility(View.VISIBLE); //AM-478 moved from onStart

        createAudioAndVideoTracks();

        /*
         * Set the initial state of the UI
         */
        intializeUI();


    }


    private int callid;
    private String caller;
    private String roomname;
    private String receiver;


    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();

        if (CallActivity.ACTION_ACCEPTED_CALL.equals(action)) {
            caller = intent.getStringExtra("caller");
            callid = intent.getIntExtra("call_id", 0);
            accessToken = intent.getStringExtra("token");
            roomname = intent.getStringExtra("roomname");
            receiver = intent.getStringExtra("receiver");
        }


        audioCodec = getAudioCodecPreference(PREF_AUDIO_CODEC,
                PREF_AUDIO_CODEC_DEFAULT);
        videoCodec = getVideoCodecPreference(PREF_VIDEO_CODEC,
                PREF_VIDEO_CODEC_DEFAULT);
        enableAutomaticSubscription = getAutomaticSubscriptionPreference(PREF_ENABLE_AUTOMATIC_SUBSCRIPTION,
                PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT);
        /*
         * Get latest encoding parameters
         */
        final EncodingParameters newEncodingParameters = getEncodingParameters();

        /*
         * Update encoding parameters
         */
        encodingParameters = newEncodingParameters;

        /*
         * Route audio through cached value.
         */
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        /*
         * Update reconnecting UI
         */
//        if (room != null) {
//            reconnectingProgressBar.setVisibility((room.getState() != Room.State.RECONNECTING) ? View.GONE :
//                    View.VISIBLE);
//        }
//        reconnectingProgressBar.setVisibility(View.VISIBLE);

        if (room == null || room.getState() == Room.State.DISCONNECTED) {
            connectToRoom(roomname);
        }

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

        /*
         * Update preferred audio and video codec in case changed in settings
         */
        audioCodec = getAudioCodecPreference(PREF_AUDIO_CODEC,
                PREF_AUDIO_CODEC_DEFAULT);
        videoCodec = getVideoCodecPreference(PREF_VIDEO_CODEC,
                PREF_VIDEO_CODEC_DEFAULT);
        enableAutomaticSubscription = getAutomaticSubscriptionPreference(PREF_ENABLE_AUTOMATIC_SUBSCRIPTION,
                PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT);

        // AM-507
        recreateVideoTrackIfNeeded();
        if(!isVideoMuted) {
            localVideoTrack.enable(true);
        }

        /*
         * Route audio through cached value.
         */
        audioManager.setSpeakerphoneOn(isSpeakerPhoneEnabled);

        int muteIcon = !audioManager.isMicrophoneMute() ?
                R.drawable.ic_mic_white_24dp : R.drawable.ic_mic_off_gray_24dp;
        if (!audioManager.isMicrophoneMute()){
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
        } else {
            muteActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
        }
        muteActionFab.setImageDrawable(ContextCompat.getDrawable(
                VideoActivity.this, muteIcon));



//        int speakerIcon = isSpeakerPhoneEnabled ?
//                R.drawable.ic_volume_up_white_24dp : R.drawable.ic_volume_off_gray_24dp;
//        if (isSpeakerPhoneEnabled) {
//            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.lobbyMediaControls)));
//
//        } else {
//            speakerPhoneActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
//        }
//        speakerPhoneActionFab.setImageDrawable(ContextCompat.getDrawable(
//                VideoActivity.this, speakerIcon));

        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);

        /*
         * Update reconnecting UI
         */
        if (!room.getRemoteParticipants().isEmpty()){
            reconnectingProgressBar.setVisibility(View.GONE);
        }
//        if (room != null) {
//            reconnectingProgressBar.setVisibility((room.getState() != Room.State.RECONNECTING) ?
//                    View.GONE :
//                    View.VISIBLE);
//        }
    }

    private void recreateVideoTrackIfNeeded() {
        final EncodingParameters newEncodingParameters = getEncodingParameters();
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
            if (localParticipant != null) {
                localParticipant.publishTrack(localVideoTrack);

                /*
                 * Update encoding parameters if they have changed.
                 */
                if (!newEncodingParameters.equals(encodingParameters)) {
                    localParticipant.setEncodingParameters(newEncodingParameters);
                }
            }
        }

        /*
         * Update encoding parameters
         */
        encodingParameters = newEncodingParameters;
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
            if (localParticipant != null) {
                localParticipant.unpublishTrack(localVideoTrack);
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
         * Always disconnect from the room before leaving the Activity to
         * ensure any memory allocated to the Room resource is freed.
         */
        if (room != null && room.getState() != Room.State.DISCONNECTED) {
            room.disconnect();
            disconnectedFromOnDestroy = true;
        }

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

        audioDeviceSelector.stop(); //AM-440

        //ALF AM-446
        setVolumeControlStream(savedVolumeControlStream);
        //audioManager.setMode(previousAudioMode);

        //AM-441
        SoundPoolManager.getInstance(VideoActivity.this).setSpeakerOn(false);
        audioManager.setMode(SoundPoolManager.getInstance(VideoActivity.this).getPreviousAudioMode());

        unregisterReceiver(phonecallReceiver); //ALF AM-474

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
    }

    private CameraSource getAvailableCameraSource() {
        return (CameraCapturer.isSourceAvailable(CameraSource.FRONT_CAMERA)) ?
                (CameraSource.FRONT_CAMERA) :
                (CameraSource.BACK_CAMERA);
    }

    private void connectToRoom(String roomName) {
        configureAudio(true);
        ConnectOptions.Builder connectOptionsBuilder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);

        /*
         * Add local audio track to connect options to share with participants.
         */
        if (localAudioTrack != null) {
            connectOptionsBuilder
                    .audioTracks(Collections.singletonList(localAudioTrack));
        }

        /*
         * Add local video track to connect options to share with participants.
         */
        if (localVideoTrack != null) {
            connectOptionsBuilder.videoTracks(Collections.singletonList(localVideoTrack));
        }

        /*
         * Set the preferred audio and video codec for media.
         */
        connectOptionsBuilder.preferAudioCodecs(Collections.singletonList(audioCodec));
        connectOptionsBuilder.preferVideoCodecs(Collections.singletonList(videoCodec));
        connectOptionsBuilder.enableIceGatheringOnAnyAddressPorts(true);

        /*
         * Set the sender side encoding parameters.
         */
        encodingParameters = getEncodingParameters();
        connectOptionsBuilder.encodingParameters(encodingParameters);

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        connectOptionsBuilder.enableAutomaticSubscription(enableAutomaticSubscription);

        room = Video.connect(this, connectOptionsBuilder.build(), roomListener());
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


    /*
     * Get the preferred audio codec from shared preferences
     */
    private AudioCodec getAudioCodecPreference(String key, String defaultValue) {
        final String audioCodecName = preferences.getString(key, defaultValue);

        switch (audioCodecName) {
            case IsacCodec.NAME:
                return new IsacCodec();
            case OpusCodec.NAME:
                return new OpusCodec();
            case PcmaCodec.NAME:
                return new PcmaCodec();
            case PcmuCodec.NAME:
                return new PcmuCodec();
            case G722Codec.NAME:
                return new G722Codec();
            default:
                return new OpusCodec();
        }
    }

    /*
     * Get the preferred video codec from shared preferences
     */
    private VideoCodec getVideoCodecPreference(String key, String defaultValue) {
        final String videoCodecName = preferences.getString(key, defaultValue);
        if(Build.MODEL == "Pixel 3" || Build.MODEL == "Pixel 4" ){ //using a plugin to detect mobile model
            return new H264Codec();
        }
        switch (videoCodecName) {
            case Vp8Codec.NAME:
                boolean simulcast = preferences.getBoolean(PREF_VP8_SIMULCAST,
                        PREF_VP8_SIMULCAST_DEFAULT);
                return new Vp8Codec(simulcast);
            case H264Codec.NAME:
                return new H264Codec();
            case Vp9Codec.NAME:
                return new Vp9Codec();
            default:
                return new Vp8Codec();
        }
    }

    private boolean getAutomaticSubscriptionPreference(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    private EncodingParameters getEncodingParameters() {
        final int maxAudioBitrate = Integer.parseInt(
                preferences.getString(PREF_SENDER_MAX_AUDIO_BITRATE,
                        PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT));
        final int maxVideoBitrate = Integer.parseInt(
                preferences.getString(PREF_SENDER_MAX_VIDEO_BITRATE,
                        PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT));

        return new EncodingParameters(maxAudioBitrate, maxVideoBitrate);
    }

    /*
     * Called when remote participant joins the room
     */
    @SuppressLint("SetTextI18n")
    private void addRemoteParticipant(RemoteParticipant remoteParticipant) {
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

        /*
         * Add remote participant renderer
         */
        if (remoteParticipant.getRemoteVideoTracks().size() > 0) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Only render video tracks that are subscribed to
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                addRemoteParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }

        /*
         * Start listening for participant events
         */
        remoteParticipant.setListener(remoteParticipantListener());
    }

    /*
     * Set primary view as renderer for participant video track
     */
    private void addRemoteParticipantVideo(VideoTrack videoTrack) {
        //moveLocalVideoToThumbnailView(); //AM-450
        primaryVideoView.setMirror(false);
        videoTrack.addRenderer(primaryVideoView);
    }

    //AM-450 commented out
    /*private void moveLocalVideoToThumbnailView() {
        if (thumbnailVideoView.getVisibility() == View.GONE) {
            thumbnailVideoView.setVisibility(View.VISIBLE);
            recreateVideoTrackIfNeeded();
            localVideoTrack.removeRenderer(primaryVideoView);
            localVideoTrack.addRenderer(thumbnailVideoView);
            localVideoView = thumbnailVideoView;
            thumbnailVideoView.setMirror(cameraCapturerCompat.getCameraSource() ==
                    CameraSource.FRONT_CAMERA);
        }
    }*/

    /*
     * Called when remote participant leaves the room
     */
    @SuppressLint("SetTextI18n")
    private void removeRemoteParticipant(RemoteParticipant remoteParticipant) {
        if (!remoteParticipant.getIdentity().equals(remoteParticipantIdentity)) {
            return;
        }

        /*
         * Remove remote participant renderer
         */
        if (!remoteParticipant.getRemoteVideoTracks().isEmpty()) {
            RemoteVideoTrackPublication remoteVideoTrackPublication =
                    remoteParticipant.getRemoteVideoTracks().get(0);

            /*
             * Remove video only if subscribed to participant track
             */
            if (remoteVideoTrackPublication.isTrackSubscribed()) {
                removeParticipantVideo(remoteVideoTrackPublication.getRemoteVideoTrack());
            }
        }
        //moveLocalVideoToPrimaryView(); //AM-450
    }

    private void removeParticipantVideo(VideoTrack videoTrack) {
        videoTrack.removeRenderer(primaryVideoView);
    }

    //AM-450 commented out for now
    /*private void moveLocalVideoToPrimaryView() {
        if (thumbnailVideoView.getVisibility() == View.VISIBLE) {
            thumbnailVideoView.setVisibility(View.GONE);
            if (localVideoTrack != null) {
                localVideoTrack.removeRenderer(thumbnailVideoView);
                localVideoTrack.addRenderer(primaryVideoView);
            }
            localVideoView = primaryVideoView;
            primaryVideoView.setMirror(cameraCapturerCompat.getCameraSource() ==
                    CameraSource.FRONT_CAMERA);
        }
    }*/

    /*
     * Room events listener
     */
    @SuppressLint("SetTextI18n")
    private Room.Listener roomListener() {
        return new Room.Listener() {
            @Override
            public void onConnected(Room room) {
                localParticipant = room.getLocalParticipant();
                setTitle(room.getName());
                audioDeviceSelector.activate(); //AM-440
                updateAudioDeviceIcon(audioDeviceSelector.getSelectedAudioDevice());

                for (RemoteParticipant remoteParticipant : room.getRemoteParticipants()) {
                    addRemoteParticipant(remoteParticipant);
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

            @Override
            public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
                reconnectingProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReconnected(@NonNull Room room) {
                if (!room.getRemoteParticipants().isEmpty()){
                    reconnectingProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onConnectFailure(Room room, TwilioException e) {
                configureAudio(false);
                audioDeviceSelector.deactivate(); //AM-440
                intializeUI();
            }

            @Override
            public void onDisconnected(Room room, TwilioException e) {
                localParticipant = null;
                VideoActivity.this.room = null;
                configureAudio(false);
                audioDeviceSelector.deactivate(); //AM-440
                // Only reinitialize the UI if disconnect was not called from onDestroy()
                if (!disconnectedFromOnDestroy) {
                    intializeUI();
                    //moveLocalVideoToPrimaryView(); //AM-450
                }
            }

            @Override
            public void onParticipantConnected(Room room, RemoteParticipant remoteParticipant) {
                addRemoteParticipant(remoteParticipant);
                reconnectingProgressBar.setVisibility(View.GONE);

            }

            @Override
            public void onParticipantDisconnected(Room room, RemoteParticipant remoteParticipant) {
                removeRemoteParticipant(remoteParticipant);
                room.disconnect();
                //CMG disconnect when remote leaves
                localParticipant = null;
                VideoActivity.this.room = null;
                handleDisconnect(); //ALF AM-420
                finish();
            }

            @Override
            public void onRecordingStarted(Room room) {
                /*
                 * Indicates when media shared to a Room is being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStarted");
            }

            @Override
            public void onRecordingStopped(Room room) {
                /*
                 * Indicates when media shared to a Room is no longer being recorded. Note that
                 * recording is only available in our Group Rooms developer preview.
                 */
                Log.d(TAG, "onRecordingStopped");
            }
        };
    }

    private void configureAudio(boolean enable) {
        if (enable) {
            previousAudioMode = audioManager.getMode();
            // Request audio focus before making any device switch
            requestAudioFocus();
            /*
             * Use MODE_IN_COMMUNICATION as the default audio mode. It is required
             * to be in this mode when playout and/or recording starts for the best
             * possible VoIP performance. Some devices have difficulties with
             * speaker mode if this is not set.
             */
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            /*
             * Always disable microphone mute during a WebRTC call.
             */
            previousMicrophoneMute = audioManager.isMicrophoneMute();
            audioManager.setMicrophoneMute(false);

            audioDeviceSelector.start((audioDevices, audioDevice) -> Unit.INSTANCE); //AM-440

            //AM-441
            if (SoundPoolManager.getInstance(VideoActivity.this).getSpeakerOn()) {
                List<AudioDevice> availableAudioDevices = audioDeviceSelector.getAvailableAudioDevices();
                for (AudioDevice a : availableAudioDevices) {
                    if (a instanceof AudioDevice.Speakerphone) {
                        audioDeviceSelector.selectDevice(a);
                    }
                }
            }

            updateAudioDeviceIcon(audioDeviceSelector.getSelectedAudioDevice());
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //ALF AM-446
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
            audioManager.setMicrophoneMute(previousMicrophoneMute);
            audioManager.setMode(previousAudioMode);
        }
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) { //ALF AM-446
                AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                focusRequest =
                        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                                .setAudioAttributes(playbackAttributes)
                                .setAcceptsDelayedFocusGain(true)
                                .setOnAudioFocusChangeListener(
                                        i -> {
                                        })
                                .build();
            }
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
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



    @SuppressLint("SetTextI18n")
    private RemoteParticipant.Listener remoteParticipantListener() {
        return new RemoteParticipant.Listener() {
            @Override
            public void onAudioTrackPublished(RemoteParticipant remoteParticipant,
                                              RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.isTrackEnabled(),
                        remoteAudioTrackPublication.isTrackSubscribed(),
                        remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackUnpublished(RemoteParticipant remoteParticipant,
                                                RemoteAudioTrackPublication remoteAudioTrackPublication) {
                Log.i(TAG, String.format("onAudioTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.isTrackEnabled(),
                        remoteAudioTrackPublication.isTrackSubscribed(),
                        remoteAudioTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackPublished(RemoteParticipant remoteParticipant,
                                             RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.isTrackEnabled(),
                        remoteDataTrackPublication.isTrackSubscribed(),
                        remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onDataTrackUnpublished(RemoteParticipant remoteParticipant,
                                               RemoteDataTrackPublication remoteDataTrackPublication) {
                Log.i(TAG, String.format("onDataTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.isTrackEnabled(),
                        remoteDataTrackPublication.isTrackSubscribed(),
                        remoteDataTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackPublished(RemoteParticipant remoteParticipant,
                                              RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackPublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.isTrackEnabled(),
                        remoteVideoTrackPublication.isTrackSubscribed(),
                        remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onVideoTrackUnpublished(RemoteParticipant remoteParticipant,
                                                RemoteVideoTrackPublication remoteVideoTrackPublication) {
                Log.i(TAG, String.format("onVideoTrackUnpublished: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%s, enabled=%b, " +
                                "subscribed=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.isTrackEnabled(),
                        remoteVideoTrackPublication.isTrackSubscribed(),
                        remoteVideoTrackPublication.getTrackName()));
            }

            @Override
            public void onAudioTrackSubscribed(RemoteParticipant remoteParticipant,
                                               RemoteAudioTrackPublication remoteAudioTrackPublication,
                                               RemoteAudioTrack remoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrack.isEnabled(),
                        remoteAudioTrack.isPlaybackEnabled(),
                        remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                 RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                 RemoteAudioTrack remoteAudioTrack) {
                Log.i(TAG, String.format("onAudioTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrack: enabled=%b, playbackEnabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrack.isEnabled(),
                        remoteAudioTrack.isPlaybackEnabled(),
                        remoteAudioTrack.getName()));
            }

            @Override
            public void onAudioTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                       RemoteAudioTrackPublication remoteAudioTrackPublication,
                                                       TwilioException twilioException) {
                Log.i(TAG, String.format("onAudioTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteAudioTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteAudioTrackPublication.getTrackSid(),
                        remoteAudioTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
            }

            @Override
            public void onDataTrackSubscribed(RemoteParticipant remoteParticipant,
                                              RemoteDataTrackPublication remoteDataTrackPublication,
                                              RemoteDataTrack remoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrack.isEnabled(),
                        remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                RemoteDataTrackPublication remoteDataTrackPublication,
                                                RemoteDataTrack remoteDataTrack) {
                Log.i(TAG, String.format("onDataTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrack.isEnabled(),
                        remoteDataTrack.getName()));
            }

            @Override
            public void onDataTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                      RemoteDataTrackPublication remoteDataTrackPublication,
                                                      TwilioException twilioException) {
                Log.i(TAG, String.format("onDataTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteDataTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteDataTrackPublication.getTrackSid(),
                        remoteDataTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
            }

            @Override
            public void onVideoTrackSubscribed(RemoteParticipant remoteParticipant,
                                               RemoteVideoTrackPublication remoteVideoTrackPublication,
                                               RemoteVideoTrack remoteVideoTrack) {
                Log.i(TAG, String.format("onVideoTrackSubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack.isEnabled(),
                        remoteVideoTrack.getName()));
                addRemoteParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackUnsubscribed(RemoteParticipant remoteParticipant,
                                                 RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                 RemoteVideoTrack remoteVideoTrack) {
                Log.i(TAG, String.format("onVideoTrackUnsubscribed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrack: enabled=%b, name=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrack.isEnabled(),
                        remoteVideoTrack.getName()));
                removeParticipantVideo(remoteVideoTrack);
            }

            @Override
            public void onVideoTrackSubscriptionFailed(RemoteParticipant remoteParticipant,
                                                       RemoteVideoTrackPublication remoteVideoTrackPublication,
                                                       TwilioException twilioException) {
                Log.i(TAG, String.format("onVideoTrackSubscriptionFailed: " +
                                "[RemoteParticipant: identity=%s], " +
                                "[RemoteVideoTrackPublication: sid=%b, name=%s]" +
                                "[TwilioException: code=%d, message=%s]",
                        remoteParticipant.getIdentity(),
                        remoteVideoTrackPublication.getTrackSid(),
                        remoteVideoTrackPublication.getTrackName(),
                        twilioException.getCode(),
                        twilioException.getMessage()));
                Snackbar.make(connectActionFab,
                        String.format("Failed to subscribe to %s video track",
                                remoteParticipant.getIdentity()),
                        Snackbar.LENGTH_LONG)
                        .show();
            }

            @Override
            public void onAudioTrackEnabled(RemoteParticipant remoteParticipant,
                                            RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onAudioTrackDisabled(RemoteParticipant remoteParticipant,
                                             RemoteAudioTrackPublication remoteAudioTrackPublication) {

            }

            @Override
            public void onVideoTrackEnabled(RemoteParticipant remoteParticipant,
                                            RemoteVideoTrackPublication remoteVideoTrackPublication) {
                noVideoView.setVisibility(View.GONE);
                primaryVideoView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onVideoTrackDisabled(RemoteParticipant remoteParticipant,
                                             RemoteVideoTrackPublication remoteVideoTrackPublication) {
                noVideoView.setVisibility(View.VISIBLE);
                primaryVideoView.setVisibility(View.GONE);
            }
        };
    }

    private View.OnClickListener disconnectClickListener() {
        return v -> {
            /*
             * Disconnect from room
             */
            if (room != null) {
                room.disconnect();
            }
            handleDisconnect(); //ALF AM-420
            finish();
        };
    }

    //ALF AM-420
    private void handleDisconnect() {
        SoundPoolManager.getInstance(VideoActivity.this).playDisconnect();
        final Intent intent = new Intent(this, XmppConnectionService.class);
        intent.setAction(XmppConnectionService.ACTION_FINISH_CALL);
        Compatibility.startService(this, intent);
    }

//    private View.OnClickListener connectActionClickListener() {
//        return v -> showConnectDialog();
//    }
//
//    private DialogInterface.OnClickListener cancelConnectDialogClickListener() {
//        return (dialog, which) -> {
//            intializeUI();
//            connectDialog.dismiss();
//        };
//    }

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
            List<AudioDevice> availableAudioDevices = audioDeviceSelector.getAvailableAudioDevices();
            if (availableAudioDevices.size()>2){
            showAudioDevices();
            }else{
                boolean expectedSpeakerPhoneState = !audioManager.isSpeakerphoneOn();

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
            /*
             * Enable/disable the speakerphone
             */
            /*if (audioManager != null) {
                boolean expectedSpeakerPhoneState = !audioManager.isSpeakerphoneOn();

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
            }*/
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
    private void disableSpeakerPhone(){

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
            }
        } catch (Exception e){

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
        //cancel any current call
        if (room != null) {
            room.disconnect();
        }
        handleDisconnect();
        finish();
    }
}
