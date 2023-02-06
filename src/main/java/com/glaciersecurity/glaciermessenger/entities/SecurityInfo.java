package com.glaciersecurity.glaciermessenger.entities;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class SecurityInfo {
    private static final String DEVICE_ID = "device";
    private static final String DEVICE = "deviceid";
    private static final String GLACIER_VERSION = "glacier_version";
    private static final String GLACIER_VERSION_OUTDATED = "glacier_version_outdated";
    private static final String OS_VERSION = "os_version";
    private static final String OS_VERSION_OUTDATED = "os_version_outdated";
    private static final String OS_PATCH = "os_patch";
    private static final String COMPROMISED = "compromised";
    private static final String COMPROMISED_DETAIL = "compromised_detail";
    private static final String SCREEN_LOCK = "screen_lock";
    private static final String BIOMETRIC_LOCK = "biometric_lock";
    private static final String CORE_ENABLED = "core_enabled";
    private static final String LAST_UPDATED = "last_updated";

    protected String sDeviceId;
    protected String sDevice;
    protected String sGlacierVersion;
    protected boolean sGlacierOutdated;
    protected String sOsVersion;
    protected boolean sOsOutdated;
    protected String sOsPatch;
    protected boolean sCompromised;
    protected String sCompromisedDetail;
    protected boolean sScreenLock;
    protected boolean sApplicationLock;
    protected boolean sBiometricLock;
    protected boolean sCoreEnabled;
    protected String sLastUpdated;


    public SecurityInfo(String deviceid) {
        sDeviceId = deviceid;
    }

    public SecurityInfo(JSONObject jsecinfo) throws JSONException, Exception {
        sDeviceId = (String) jsecinfo.get("deviceid");
        sDevice = (String) jsecinfo.get("device");
        sGlacierVersion = (String) jsecinfo.get("glacier_version");
        sGlacierOutdated = Boolean.parseBoolean(String.valueOf(jsecinfo.get("glacier_version_outdated")));
        sOsVersion = (String) jsecinfo.get("os_version");
        sOsOutdated = Boolean.parseBoolean(String.valueOf(jsecinfo.get("os_version_outdated")));
        try {
            sOsPatch = (String) jsecinfo.get("os_patch");
        } catch (Exception ex) {
            sOsPatch = "none";
        }
        sCompromised = Boolean.parseBoolean(String.valueOf(jsecinfo.get("compromised")));
        sCompromisedDetail = (String) jsecinfo.get("compromised_detail");
        sScreenLock = Boolean.parseBoolean(String.valueOf(jsecinfo.get("screen_lock")));
        try {
            sApplicationLock = Boolean.parseBoolean(String.valueOf(jsecinfo.get("app_lock")));
        } catch (Exception ex) {
            sApplicationLock = Boolean.parseBoolean(String.valueOf(jsecinfo.get("device_lock")));
        }
        sBiometricLock = Boolean.parseBoolean(String.valueOf(jsecinfo.get("biometric_lock")));
        sCoreEnabled = Boolean.parseBoolean(String.valueOf(jsecinfo.get("core_enabled")));
    }

    public String toJsonString() {
        return "{\"deviceid\":\"" + getDeviceId() + "\"," +
                "\"device\":\"" + getDevice() + "\"," +
                "\"glacier_version\":\"" + getGlacierVersion() + "\"," +
                "\"glacier_version_outdated\":" + getGlacierOutdated() + "," +
                "\"os_version\":\"" + getOsVersion() + "\"," +
                "\"os_version_outdated\":" + getOsOutdated() + "," +
                "\"os_patch\":\"" + getOsPatch() + "\"," +
                "\"compromised\":" + getCompromised() + "," +
                "\"compromised_detail\":\"" + getCompromisedDetail() + "\"," +
                "\"screen_lock\":" + getScreenLock() + "," +
                "\"biometric_lock\":" + getBiometricLock() + "," +
                "\"app_lock\":" + getApplicationLock() + "," +
                "\"core_enabled\":" + getCoreEnabled() + "," +
                "\"last_updated\":\"" + setLastUpdated() +  "\"}";
    }

    public String getDeviceId() {
        return sDeviceId;
    }

    public String getDevice() {
        return sDevice;
    }

    public void setDevice(String device) {
        sDevice = device;
    }

    public String getGlacierVersion() {
        return sGlacierVersion;
    }

    public void setGlacierVersion(String gversion) {
        sGlacierVersion = gversion;
    }

    public boolean getGlacierOutdated() {
        return sGlacierOutdated;
    }

    public void setGlacierOutdated(boolean outdated) {
        sGlacierOutdated = outdated;
    }

    public String getOsVersion() {
        return sOsVersion;
    }

    public void setOsVersion(String version) {
        sOsVersion = version;
    }

    public boolean getOsOutdated() {
        return sOsOutdated;
    }

    public void setOsOutdated(boolean outdated) {
        sOsOutdated = outdated;
    }

    public String getOsPatch() {
        return sOsPatch;
    }

    public void setOsPatch(String patch) {
        sOsPatch = patch;
    }

    public boolean getCompromised() {
        return sCompromised;
    }

    public void setCompromised(boolean compromised) {
        sCompromised = compromised;
    }

    public String getCompromisedDetail() {
        return sCompromisedDetail;
    }

    public void setCompromisedDetail(String detail) {
        sCompromisedDetail = detail;
    }

    public boolean getScreenLock() {
        return sScreenLock;
    }

    public void setScreenLock(boolean screenlock) {
        sScreenLock = screenlock;
    }

    public boolean getApplicationLock() {
        return sApplicationLock;
    }

    public void setApplicationLock(boolean applock) {
        sApplicationLock = applock;
    }

    public boolean getBiometricLock() {
        return sBiometricLock;
    }

    public void setBiometricLock(boolean biolock) {
        sBiometricLock = biolock;
    }

    public boolean getCoreEnabled() {
        return sCoreEnabled;
    }

    public String setLastUpdated() {
        sLastUpdated = Calendar.getInstance().getTime().toString();
        return sLastUpdated;
    }

    public void setCoreEnabled(boolean coreenabled) {
        sCoreEnabled = coreenabled;
    }

    public SecurityInfo getCopy() {
        SecurityInfo newSecinfo = new SecurityInfo(getDeviceId());
        newSecinfo.setDevice(getDevice());
        newSecinfo.setGlacierVersion(getGlacierVersion());
        newSecinfo.setGlacierOutdated(getGlacierOutdated());
        newSecinfo.setOsVersion(getOsVersion());
        newSecinfo.setOsOutdated(getOsOutdated());
        newSecinfo.setOsPatch(getOsPatch());
        newSecinfo.setCompromised(getCompromised());
        newSecinfo.setCompromisedDetail(getCompromisedDetail());
        newSecinfo.setScreenLock(getScreenLock());
        newSecinfo.setApplicationLock(getApplicationLock());
        newSecinfo.setBiometricLock(getBiometricLock());
        newSecinfo.setCoreEnabled(getCoreEnabled());
        newSecinfo.setLastUpdated();
        return newSecinfo;
    }

    public boolean deviceIdEquals (SecurityInfo sinfo) {
        return sinfo.getDeviceId().equals(this.sDeviceId);
    }

    public boolean equals (SecurityInfo sinfo) {
        return sinfo.getDeviceId().equals(this.sDeviceId) &&
                sinfo.getDevice().equals(this.sDevice) &&
                sinfo.getGlacierVersion().equals(this.sGlacierVersion) &&
                sinfo.getGlacierOutdated() == this.sGlacierOutdated &&
                sinfo.getOsVersion().equals(this.sOsVersion) &&
                sinfo.getOsOutdated() == this.sOsOutdated &&
                sinfo.getOsPatch() != null && sinfo.getOsPatch().equals(this.sOsPatch) &&
                sinfo.getCompromised() == this.sCompromised &&
                sinfo.getCompromisedDetail().equals(this.sCompromisedDetail) &&
                sinfo.getScreenLock() == this.sScreenLock &&
                sinfo.getApplicationLock() == this.sApplicationLock &&
                sinfo.getBiometricLock() == this.sBiometricLock &&
                sinfo.getCoreEnabled() == this.sCoreEnabled;
    }
}
