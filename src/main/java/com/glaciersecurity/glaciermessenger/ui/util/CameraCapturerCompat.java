package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.twilio.video.Camera2Capturer;
import com.twilio.video.CameraCapturer;
import com.twilio.video.VideoCapturer;

import tvi.webrtc.Camera2Enumerator;

/*
 * Simple wrapper class that uses Camera2Capturer with supported devices.
 */
public class CameraCapturerCompat {
    private static final String TAG = "CameraCapturerCompat";

    private CameraCapturer camera1Capturer;
    private Camera2Capturer camera2Capturer;
    private String frontCamera;
    private String backCamera;
    private CameraManager cameraManager;
    //AM-650 changed all camera and cameraSource references

    public CameraCapturerCompat(Context context, String cameraSource) {
        if (Camera2Capturer.isSupported(context) && isLollipopApiSupported()) {
            cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            setCameras(context);
            Camera2Capturer.Listener camera2Listener =
                    new Camera2Capturer.Listener() {
                        @Override
                        public void onFirstFrameAvailable() {
                            Log.i(TAG,"onFirstFrameAvailable");
                        }

                        @Override
                        public void onCameraSwitched(@NonNull String newCameraId) {
                            Log.i(TAG, "onCameraSwitched: newCameraId = " + newCameraId);
                        }

                        @Override
                        public void onError(
                                @NonNull Camera2Capturer.Exception camera2CapturerException) {
                            Log.e(TAG, camera2CapturerException.toString());
                        }
                    };
            camera2Capturer =
                    new Camera2Capturer(context, cameraSource, camera2Listener);
        } else {
            camera1Capturer = new CameraCapturer(context, cameraSource);
        }
    }

    public String getCameraSource() {
        if (usingCamera1()) {
            return camera1Capturer.getCameraId();
        } else {
            return camera2Capturer.getCameraId();
        }
    }

    public void switchCamera() {
        if (usingCamera1()) {
            if (camera1Capturer.getCameraId() == frontCamera && backCamera != null) {
                camera1Capturer.switchCamera(backCamera);
            } else if (frontCamera != null) {
                camera1Capturer.switchCamera(frontCamera);
            }
        } else {
            try {
                if (camera2Capturer.getCameraId() == frontCamera && backCamera != null) {
                    camera2Capturer.switchCamera(backCamera);
                } else if (frontCamera != null){
                    camera2Capturer.switchCamera(frontCamera);
                }
            }
            catch(Exception e){

            }
        }
    }

    /*
     * This method is required because this class is not an implementation of VideoCapturer due to
     * a shortcoming in VideoCapturerDelegate where only instances of CameraCapturer,
     * Camera2Capturer, and ScreenCapturer are initialized correctly with a SurfaceTextureHelper.
     * Because capturing to a texture is not a part of the official public API we must expose
     * this method instead of writing a custom capturer so that camera capturers are properly
     * initialized.
     */
    public VideoCapturer getVideoCapturer() {
        if (usingCamera1()) {
            return camera1Capturer;
        } else {
            return camera2Capturer;
        }
    }

    private boolean usingCamera1() {
        return camera1Capturer != null;
    }

    private void setCameras(Context context) {
        Camera2Enumerator camera2Enumerator = new Camera2Enumerator(context);
        for (String cameraId : camera2Enumerator.getDeviceNames()) {
            if (isCameraIdSupported(cameraId)) {
                if (camera2Enumerator.isFrontFacing(cameraId)) {
                    frontCamera = cameraId;
                }
                if (camera2Enumerator.isBackFacing(cameraId)) {
                    backCamera = cameraId;
                }
            }
        }
    }

    private boolean isLollipopApiSupported() {
        return true;
    }

    private boolean isCameraIdSupported(String cameraId) {
        boolean isMonoChromeSupported = false;
        boolean isPrivateImageFormatSupported = false;
        CameraCharacteristics cameraCharacteristics;
        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        /*
         * This is a temporary work around for a RuntimeException that occurs on devices which contain cameras
         * that do not support ImageFormat.PRIVATE output formats. A long term fix is currently in development.
         * https://github.com/twilio/video-quickstart-android/issues/431
         */
        final StreamConfigurationMap streamMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (streamMap != null) {
            isPrivateImageFormatSupported = streamMap.isOutputSupportedFor(ImageFormat.PRIVATE);
        }

        /*
         * Read the color filter arrangements of the camera to filter out the ones that support
         * SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO or SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR.
         * Visit this link for details on supported values - https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#SENSOR_INFO_COLOR_FILTER_ARRANGEMENT
         */
        Integer colorFilterArrangement =
                cameraCharacteristics.get(
                        CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
        // Normalize the color filter arrangement
        colorFilterArrangement = colorFilterArrangement == null ? -1 : colorFilterArrangement;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isMonoChromeSupported =
                    colorFilterArrangement
                            == CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO
                            || colorFilterArrangement
                            == CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR;
        }
        return isPrivateImageFormatSupported && !isMonoChromeSupported;
    }
}
