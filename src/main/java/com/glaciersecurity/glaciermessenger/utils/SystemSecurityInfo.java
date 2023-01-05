package com.glaciersecurity.glaciermessenger.utils;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;

import androidx.annotation.BoolRes;
import androidx.biometric.BiometricManager;

import com.amazonaws.amplify.generated.graphql.GetGlacierUsersQuery;
import com.amazonaws.amplify.generated.graphql.UpdateGlacierUsersMutation;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.glaciersecurity.glaciermessenger.BuildConfig;
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.entities.SecurityInfo;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.SettingsActivity;
import com.glaciersecurity.glaciermessenger.xml.Tag;
import com.glaciersecurity.glaciermessenger.xml.XmlReader;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import type.UpdateGlacierUsersInput;


//AM#20, AM#21, AM#22, AM#23, AM#24
public class SystemSecurityInfo {

    private static final String GOLDFISH = "goldfish";
    private static final String RANCHU = "ranchu";
    private static final String SDK = "sdk";

    private static final String VERSIONS_URL = "https://tiny.one/5967h5e2";
    private static final String USE_CORE_CONNECT = "use_core_connect";

    private XmlReader tagReader;
    private String latestOs;
    private String latestGlacier;
    private String latestAlt;
    private boolean hasAnomalies;
    private String anomalyString;

    private XmppConnectionService xmppConnectionService;
    private AWSAppSyncClient appsyncclient;
    private SecurityInfo securityInfo;
    private boolean needsUpdate;

    public SystemSecurityInfo(XmppConnectionService xmppConn) {
        xmppConnectionService = xmppConn;
        hasAnomalies = false;
    }

    public boolean isSecure() {
        return isLatestOS() && isLatestGlacier() && !hasAnomalies() && hasScreenLock();
    }

    public boolean hasScreenLock(){
        KeyguardManager myKM = (KeyguardManager) xmppConnectionService.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.isDeviceSecure();
    }
    public boolean hasBioLock(){
        KeyguardManager myKM = (KeyguardManager) xmppConnectionService.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        //return myKM.isDeviceSecure();
        BiometricManager myBM = BiometricManager.from(xmppConnectionService.getApplicationContext());
        return (myBM.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS);
    }

    public boolean isCoreEnabled(){
        return PreferenceManager.getDefaultSharedPreferences(xmppConnectionService).getBoolean(USE_CORE_CONNECT, false);
    }


    private void checkLatestVersions() {
        //TODO when Android Studio fully supports Java 11
        /*HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://webcode.me"))
                .GET() // GET is default
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());*/
        new Thread(() -> {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(VERSIONS_URL).openConnection();
                con.setRequestMethod("GET");
                int status = con.getResponseCode();
                System.out.println(status);

                if (status == 200) {
                    tagReader = new XmlReader();
                    InputStream is = con.getInputStream();
                    tagReader.setInputStream(con.getInputStream());
                    Tag nextTag = tagReader.readTag();
                    while (nextTag != null && !nextTag.isEnd("versions")) {
                        if (nextTag.isStart("glacier_android")) {
                            latestGlacier = tagReader.readElement(nextTag).getContent();
                        } else if (nextTag.isStart("android_os")) {
                            latestOs = tagReader.readElement(nextTag).getContent();
                        }  else if (nextTag.isStart("alt_os")) {
                            latestAlt = tagReader.readElement(nextTag).getContent();
                        }
                        nextTag = tagReader.readTag();
                    }
                    is.close();

                    boolean osOutdated = !isLatestOS();
                    boolean glacierOutdated = !isLatestGlacier();

                    if (osOutdated != securityInfo.getOsOutdated() ||
                            glacierOutdated != securityInfo.getGlacierOutdated()) {
                        needsUpdate = true;
                        securityInfo.setOsOutdated(osOutdated);
                        securityInfo.setGlacierOutdated(glacierOutdated);
                    }
                } else {
                    System.out.println("error GET info");
                }
            } catch (Exception e) {
                System.out.println("error couldn't fetch or parse data");
            }

            trySecurityInfoUpload();

            if (!isSecure()) {
                //notify user, some kind of listener
            }
        }).start();
    }

    /*public String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return "Android SDK: " + sdkVersion + " (" + release +")";
    }*/

    public boolean isLatestOS() {

        if(Build.MODEL.contains("Pixelbook")){
            return true;
        }

        if (latestAlt != null) {
            String myOsBuild= Build.DISPLAY;

            ComparableVersion latestCompAltOs = new ComparableVersion(latestAlt);
            ComparableVersion myOsB = new ComparableVersion(myOsBuild);
            if (myOsB.compareTo(latestCompAltOs) == 0){
                return true;
            }
        }
        if (latestOs != null) {
            ComparableVersion latestCompOs = new ComparableVersion(latestOs);
            String myOsVer = Build.VERSION.RELEASE;
            ComparableVersion myOs = new ComparableVersion(myOsVer);
            return myOs.compareTo(latestCompOs) >= 0;
        }
        return true;
    }

    public boolean isLatestGlacier() {
        if (latestGlacier != null) {
            ComparableVersion latestCompGlacier = new ComparableVersion(latestGlacier);
            String versionName = BuildConfig.VERSION_NAME;
            if (versionName.contains("-")) {
                versionName = versionName.split("-")[0];
            }
            ComparableVersion myVersion = new ComparableVersion(versionName);
            return myVersion.compareTo(latestCompGlacier) >= 0;
        }
        return true;
    }

    public boolean hasAnomalies() {
        return hasAnomalies;
    }

    public String getAnomaliesString() {
        return anomalyString;
    }

    public void checkCurrentSecurityInfo() {
        if (securityInfo == null) {
            final Account account = xmppConnectionService.getAccounts().get(0);
            CognitoAccount myCogAccount = xmppConnectionService.databaseBackend.getCognitoAccount(account);
            securityInfo = new SecurityInfo(PhoneHelper.getAndroidId(xmppConnectionService.getApplicationContext()));

            //fill the basics
            //device
            String deviceStr = Build.MANUFACTURER + " " + Build.MODEL;
            String patch = getCurrentSecurityPatch();
            securityInfo.setDevice(deviceStr);

            //versions and outdated check
            String osversion = Build.VERSION.RELEASE;
            securityInfo.setOsVersion(osversion);
            securityInfo.setOsPatch(patch);

            String gversionName = BuildConfig.VERSION_NAME;
            if (gversionName.contains("-")) {
                gversionName = gversionName.split("-")[0];
            }
            securityInfo.setGlacierVersion(gversionName);
            securityInfo.setCompromisedDetail("none");
            needsUpdate = true;
        }

        if (isRooted(xmppConnectionService.getApplicationContext())) {
            hasAnomalies = true;
            anomalyString = "Shows signs of being rooted";
        /*} else if (isDeveloperToolsEnabled(context)) {
            hasAnomalies = true;
            anomalyString = "Shows signs of developer tools enabled";
        } else if (isUSBDebuggingEnabled(context)) {
            hasAnomalies = true;
            anomalyString = "Shows signs of being debugged";*/
        } else {
            hasAnomalies = false;
            anomalyString = "Your system is safe";
        }

        if (hasAnomalies != securityInfo.getCompromised()) {
            needsUpdate = true;
            securityInfo.setCompromised(hasAnomalies);
            securityInfo.setCompromisedDetail(anomalyString);
        }

        boolean hasScreenLock = hasScreenLock();
        if (hasScreenLock != securityInfo.getScreenLock()) {
            securityInfo.setScreenLock(hasScreenLock);
            needsUpdate = true;
        }

        boolean hasBioLock = isBiometricPINOn(xmppConnectionService.getApplicationContext());
        if (hasBioLock != securityInfo.getApplicationLock()) {
            securityInfo.setApplicationLock(hasBioLock);
            needsUpdate = true;
        }

        boolean hasDeviceLock = hasBioLock();
        if (hasDeviceLock != securityInfo.getBiometricLock()) {
            securityInfo.setBiometricLock(hasDeviceLock);
            needsUpdate = true;
        }

        boolean hasCoreEnabled = isCoreEnabled();
        if (hasCoreEnabled != securityInfo.getCoreEnabled()) {
            securityInfo.setCoreEnabled(hasCoreEnabled);
            needsUpdate = true;
        }

        checkLatestVersions();

        /*Log.d(Config.LOGTAG, "Device is rooted: " + isRooted(context));
        Log.d(Config.LOGTAG, "Current OS version: " + getCurrentOSVersion());
        Log.d(Config.LOGTAG, "Current security patch: " + getCurrentSecurityPatch());
        Log.d(Config.LOGTAG, "Developer tools is enabled: " + isDeveloperToolsEnabled(context));
        Log.d(Config.LOGTAG, "USB debugging is enabled: " + isUSBDebuggingEnabled(context));
        Log.d(Config.LOGTAG, "Biometric or PIN enabled: " + isBiometricPINReady(context));
        Log.d(Config.LOGTAG, "Biometric or PIN turned on: " + isBiometricPINOn(context));*/
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
    /*public static boolean isBiometricReady(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isPINReady(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
    }*/

    public static boolean isBiometricPINReady(Context context) {
        return BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isBiometricPINOn(Context context) {
        return getBooleanPreference(context, SettingsActivity.USE_BIOMETRICS, R.bool.enable_biometrics);
    }

    private static boolean getBooleanPreference(Context context, String name, @BoolRes int res) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(name, context.getResources().getBoolean(res));
    }

    private void trySecurityInfoUpload() {
//        if (!needsUpdate) {
//            return;
//        }

        final Account account = xmppConnectionService.getAccounts().get(0);
        CognitoAccount myCogAccount = xmppConnectionService.databaseBackend.getCognitoAccount(account);

        if (myCogAccount == null) {
            Log.i("Problem", "No Cognito account found");
            return;
        }

        if (myCogAccount.getOrganization() == null) {
            AppHelper.init(xmppConnectionService.getApplicationContext());
            AppHelper.setUser(myCogAccount.getUserName());
            AppHelper.getPool().getUser(myCogAccount.getUserName()).getSessionInBackground(orgauthHandler);
        } else {
            queryAppSync(myCogAccount);
        }
    }

    private void queryAppSync(CognitoAccount myCogAccount) {
        if (myCogAccount == null) {
            final Account account = xmppConnectionService.getAccounts().get(0);
            myCogAccount = xmppConnectionService.databaseBackend.getCognitoAccount(account);
        }

        appsyncclient = AWSAppSyncClient.builder()
                .context(xmppConnectionService.getApplicationContext())
                .awsConfiguration(new AWSConfiguration(xmppConnectionService.getApplicationContext()))
                .build();


        appsyncclient.query(GetGlacierUsersQuery.builder()
                .organization(myCogAccount.getOrganization())
                .username(myCogAccount.getUserName())
                .build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getUserCallback);
    }

    private GraphQLCall.Callback<GetGlacierUsersQuery.Data> getUserCallback = new GraphQLCall.Callback<GetGlacierUsersQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<GetGlacierUsersQuery.Data> response) {
            new Thread(() -> {
                if (response.data().getGlacierUsers() != null) {
                    List<String> secinfo = updateSecInfoList (response.data().getGlacierUsers().securityInfo());
                    if (secinfo != null) {
                        updateSecurityHubInfo(response.data().getGlacierUsers(), secinfo);
                    }
                } else {
                    Log.i("SecurityInfo", "No SecurityInfo in response from server");
                }
            }).start();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("SecurityInfo", "Error getting SecurityInfo");
        }
    };

    private List<String> updateSecInfoList(List<String> securityInfoList) {
        List<String> seclist = new ArrayList<>();
        if (securityInfoList == null || securityInfoList.size() == 0) {
            seclist.add(securityInfo.toJsonString());
            return seclist;
        }

        String mydeviceid = securityInfo.getDeviceId();
        boolean foundme = false;

        for (String deviceinfo : securityInfoList) {
            try {
                JSONObject jsonObject = new JSONObject(deviceinfo);
                String deviceid = (String)jsonObject.get("deviceid");
                if (mydeviceid.equals(deviceid)) {
                    SecurityInfo listdevice = new SecurityInfo(jsonObject);
//                    if (listdevice.equals(securityInfo)) {
//                        needsUpdate = false;
//                        Log.d("SecurityInfo", "No need to update SecurityInfo, data hasn't changed");
//                        return null; //no update needed
//                    }
                    seclist.add(securityInfo.toJsonString());
                    foundme = true;
                } else {
                    seclist.add(deviceinfo);
                }
            } catch (JSONException je) {
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        if (!foundme) {
            seclist.add(securityInfo.toJsonString());
        }

        return seclist;
    }

    private void updateSecurityHubInfo(GetGlacierUsersQuery.GetGlacierUsers gusers, List<String> seclist) {
        //String secinfoString = "{\"glacier_version_outdated\":false,\"os_version\":\"12\",\"biometric_lock\":true,\"deviceid\":\"12345\",\"core_enabled\":false,\"screen_lock\":true,\"organization\":\"glacierEast\",\"compromised\":false,\"compromised_detail\":false,\"os_version_outdated\":false,\"device\":\"Pixel 3a\",\"glacier_version\":\"3.4.1-RC11-dev\",\"username\":\"alexsuperadmin\"}";
        UpdateGlacierUsersInput ginput = UpdateGlacierUsersInput.builder().organization(gusers.organization())
                .username(gusers.username())
                .securityInfo(seclist)
                .build();
        UpdateGlacierUsersMutation updateMutation = UpdateGlacierUsersMutation.builder().input(ginput).build();

        appsyncclient.mutate(updateMutation).enqueue(updateUsersCallback);
    }

    private GraphQLCall.Callback<UpdateGlacierUsersMutation.Data> updateUsersCallback = new GraphQLCall.Callback<UpdateGlacierUsersMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateGlacierUsersMutation.Data> response) {
            if (response != null  && response.data() != null) {
                if (response.data().updateGlacierUsers() != null) {
                    Log.d("SecurityInfo", "SecurityInfo updated");
                    needsUpdate = false;
                } else {
                    Log.i("SecurityInfo", "No SecurityInfo update response from server");
                }
            } else {
                Log.i("SecurityInfo", "No SecurityInfo update response from server");
            }
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("SecurityInfo", "Error updating SecurityInfo");
        }
    };

    /**
     * Callbacks
     */
    final AuthenticationHandler orgauthHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, CognitoDevice device) {
            Log.d(Config.LOGTAG, " -- Auth Success");
            AppHelper.setCurrSession(cognitoUserSession);
            AppHelper.newDevice(device);

            CognitoUserPool userPool = AppHelper.getPool();
            if (userPool != null) {
                CognitoUser user = userPool.getCurrentUser();
                getUserDetails(user);
            }
        }

        private void getUserDetails(CognitoUser cuser) {
            new Thread(() -> {
                cuser.getDetails(new GetDetailsHandler() {
                    @Override
                    public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                        CognitoUserAttributes cognitoUserAttributes = cognitoUserDetails.getAttributes();
                        String org = null;
                        if (cognitoUserAttributes.getAttributes().containsKey("custom:organization")) {
                            org = cognitoUserAttributes.getAttributes().get("custom:organization");
                        }
                        if (org == null) {
                            return;
                        }

                        if (xmppConnectionService != null) {
                            final Account account = xmppConnectionService.getAccounts().get(0);
                            xmppConnectionService.databaseBackend.updateCognitoAccountOrg(account, org);
                            account.setOrg(org);
                        }

                        queryAppSync(null);
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        //
                    }
                });
            }).start();
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String cogusername) {
            Locale.setDefault(Locale.US);
            getUserAuthy(authenticationContinuation, cogusername);
        }

        private void getUserAuthy(AuthenticationContinuation continuation, String cogusername) {
            final Account account = xmppConnectionService.getAccounts().get(0);
            CognitoAccount myCogAccount = xmppConnectionService.databaseBackend.getCognitoAccount(account);
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(cogusername, myCogAccount.getPassword(), null);
            continuation.setAuthenticationDetails(authenticationDetails);
            continuation.continueTask();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
        }

        @Override
        public void onFailure(Exception e) {
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
        }
    };
}
