package com.glaciersecurity.glaciermessenger.utils;

import com.amazonaws.amplify.generated.graphql.GetGlacierUsersQuery;
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
import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class SMSdbInfo {

    private XmppConnectionService xmppConnectionService;
    private AWSAppSyncClient appsyncclient;
    //private SmsProfile smsInfo;
    private ArrayList<SmsProfile> dbProfs = new ArrayList<>();
    public Object[] selected_twilionumber;
    private boolean dbPurchaseNum = false;
    private boolean add_user_to_sms = false;
    private boolean isSMSEnabled = false;
    private String org;

    protected class SmsResponse {
        String message;
        SMS_Twilio_info data;
    }
    protected class SMS_Twilio_info {
        public Object[] selected_twilionumber;
        Boolean isSMSEnabled;
        Boolean allow_user_to_purchase_numbers;
    }

    public SMSdbInfo(XmppConnectionService xmppConn) {
        xmppConnectionService = xmppConn;
        trySmsInfoUpload();
    }

    public ArrayList<SmsProfile> getExistingProfs(){
        return dbProfs;
    }

    public boolean isNumberActive(String number){
        for (SmsProfile existingProfs : dbProfs){
            if (existingProfs.getNumber().equals(number)){
                return true;
            }
            if (existingProfs.getNumber().equals(Tools.reformatNumber(number))) {
                return true;
            }
        }
        return false;
    }
    public Boolean getUserPurchasePermission(){
        return dbPurchaseNum;
    }

    public Boolean getUserHasSMS(){
        return add_user_to_sms;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public Boolean isSMSEnabled(){
        return isSMSEnabled;
    }



    public void trySmsInfoUpload() {

        new Thread(() -> {

            if (xmppConnectionService == null || xmppConnectionService.getAccounts() == null || !(xmppConnectionService.getAccounts().size() > 0)) {
                return;
            }
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

            }
            getSelectedTwilioNumber(myCogAccount.getUserName(), myCogAccount.getOrganization());
            xmppConnectionService.setSmsInfo(this);
        }).start();
    }


    public void getSelectedTwilioNumber(String username, String org) {
        //Log.d("Glacier","getPhoneNumberList for "+countryCode +" areacode "+area_code);
        String getUserTwilioNumListUrl = xmppConnectionService.getApplicationContext().getResources().getString(R.string.selected_twilio_numbers_url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("organization", org)
                .add("username",username)
                .build();
        //Log.d("Glacier","getPhoneNumberList for "+countryCode + "and its url "+getAvailableNumListUrl);
        Request request = new Request.Builder()
                .url(getUserTwilioNumListUrl)
                .addHeader("API-Key", xmppConnectionService.getApplicationContext().getResources().getString(R.string.twilio_token))
                .post(requestBody)
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            android.util.Log.d("Glacier", "Response from server: " + responseBody);
            Gson gson = new Gson();
            SmsResponse twilio_info = gson.fromJson(responseBody, SmsResponse.class);
            dbPurchaseNum = twilio_info.data.allow_user_to_purchase_numbers;
            isSMSEnabled = twilio_info.data.isSMSEnabled;
            dbProfs = getSmsProfileList(twilio_info.data.selected_twilionumber);


        }catch (IOException ex){
            android.util.Log.d("Glacier", ex.getLocalizedMessage(), ex);
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


//        appsyncclient.query(GetGlacierUsersQuery.builder()
//                .organization(myCogAccount.getOrganization())
//                .username(myCogAccount.getUserName())
//                .build())
//                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
//                .enqueue(getUserCallback);
    }

    /*private GraphQLCall.Callback<GetGlacierUsersQuery.Data> getUserCallback = new GraphQLCall.Callback<GetGlacierUsersQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<GetGlacierUsersQuery.Data> response) {
            new Thread(() -> {
                if (response.data().getGlacierUsers() != null) {
                    if (response.data().getGlacierUsers().selected_twilionumber() != null) {
                        dbProfs = (getSmsProfileList(response.data().getGlacierUsers().selected_twilionumber()));
                    }
                    if (response.data().getGlacierUsers().add_user_to_purchase_numbers() != null) {
                        dbPurchaseNum = response.data().getGlacierUsers().add_user_to_purchase_numbers();
                    }
                    add_user_to_sms = false;
                    if (response.data().getGlacierUsers().add_user_to_SMS() != null){
                        add_user_to_sms = response.data().getGlacierUsers().add_user_to_SMS();
                  }
                    isSMSEnabled = false;
                    if (response.data().getGlacierUsers().isSMSEnabled() != null){
                        isSMSEnabled = response.data().getGlacierUsers().isSMSEnabled();
                    }


                } else {
                    Log.i("SmsInfo", "No sms profiles in response from server");
                }
            }).start();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("SecurityInfo", "Error getting SMSInfo");
        }
    };

     */

    private ArrayList<SmsProfile> getSmsProfileList(Object[] smsInfoList) {
        ArrayList<SmsProfile> smsProfList = new ArrayList<>();

        for (Object deviceinfo : smsInfoList) {
            try {
                if (deviceinfo instanceof LinkedTreeMap){
                    LinkedTreeMap<String, String> treeMap = (LinkedTreeMap<String, String>) deviceinfo;
                    SmsProfile prof = new SmsProfile(treeMap);
                    smsProfList.add(prof);
                }

                } catch (Exception e) {
                    e.printStackTrace();

                }
        }
        return smsProfList;
    }

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
                            account.setOrg(org);
                            xmppConnectionService.databaseBackend.updateCognitoAccountOrg(account, org);
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
