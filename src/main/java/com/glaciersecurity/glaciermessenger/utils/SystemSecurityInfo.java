package com.glaciersecurity.glaciermessenger.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.Secure;

import androidx.biometric.BiometricManager;

import com.glaciersecurity.glaciermessenger.Config;

import java.io.File;



//AM#20, AM#21, AM#22, AM#23, AM#24
public class SystemSecurityInfo {

    private static final String GOLDFISH = "goldfish";
    private static final String RANCHU = "ranchu";
    private static final String SDK = "sdk";

    public static void checkCurrentSecurityInfo(Context context) {
        Log.d(Config.LOGTAG, "Device is rooted: " + isRooted(context));
        Log.d(Config.LOGTAG, "Current OS version: " + getCurrentOSVersion());
        Log.d(Config.LOGTAG, "Current security patch: " + getCurrentSecurityPatch());
        Log.d(Config.LOGTAG, "Developer tools is enabled: " + isDeveloperToolsEnabled(context));
        Log.d(Config.LOGTAG, "USB debugging is enabled: " + isUSBDebuggingEnabled(context));
        Log.d(Config.LOGTAG, "Biometric or PIN enabled: " + isBiometricReady(context));
    }

    /**
     * Checks via some common methods if we are running on an Android emulator.
     *
     * @return boolean value indicating that we are or are not running in an emulator.
     */
    public static boolean isEmulator(Context context) {
        @SuppressLint("HardwareIds")
        final String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        return Build.PRODUCT.contains(SDK)
                || Build.HARDWARE.contains(GOLDFISH)
                || Build.HARDWARE.contains(RANCHU)
                || androidId == null;
    }

    public static boolean isRooted(Context context) {
        // No reliable way to determine if an android phone is rooted, since a rooted phone could
        // always disguise itself as non-rooted. Some common approaches can be found on SO:
        //   http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
        //
        // http://stackoverflow.com/questions/3576989/how-can-you-detect-if-the-device-is-rooted-in-the-app
        //
        // http://stackoverflow.com/questions/7727021/how-can-androids-copy-protection-check-if-the-device-is-rooted
        final boolean isEmulator = isEmulator(context);
        final String buildTags = Build.TAGS;
        if (!isEmulator && buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        // Superuser.apk would only exist on a rooted device:
        File file = new File("/system/app/Superuser.apk");
        if (file.exists()) {
            return true;
        }

        // su is only available on a rooted device (or the emulator)
        // The user could rename or move to a non-standard location, but in that case they
        // probably don't want us to know they're root and they can pretty much subvert
        // any check anyway.
        file = new File("/system/xbin/su");
        if (!isEmulator && file.exists()) {
            return true;
        }
        return false;
    }

    public static int getCurrentOSVersion() {
        //int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        return android.os.Build.VERSION.SDK_INT;
    }

    public static String getCurrentSecurityPatch() {
        return android.os.Build.VERSION.SECURITY_PATCH;
    }

    public static boolean isDeveloperToolsEnabled(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    public static boolean isUSBDebuggingEnabled(Context context) {
        //android.provider.Settings.Global
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
    }

    /**
     * Indicate whether this device can authenticate the user with biometrics
     * @return true if there are any available biometric mechanisms and biometrics are enrolled on the device, if not, return false
     */
    public static boolean isBiometricReady(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
    }
}
