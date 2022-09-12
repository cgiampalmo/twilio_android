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
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;
import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.entities.CognitoAccount;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;


public class SMSdbInfo {

    private XmppConnectionService xmppConnectionService;
    private AWSAppSyncClient appsyncclient;
    //private SmsProfile smsInfo;
    private ArrayList<SmsProfile> dbProfs = new ArrayList<>();
    private boolean dbPurchaseNum;
    private boolean add_user_to_sms;
    private boolean isSMSEnabled;
    private static SMSdbInfo smSdbInfo;

    public SMSdbInfo(XmppConnectionService xmppConn) {
        xmppConnectionService = xmppConn;
        trySmsInfoUpload();
    }

    public ArrayList<SmsProfile> getExistingProfs(){
        return dbProfs;
    }
    public Boolean getUserPurchasePermission(){
        return dbPurchaseNum;
    }

    public Boolean getUserHasSMS(){
        return add_user_to_sms;
    }

    public Boolean isSMSEnabled(){
        return isSMSEnabled;
    }

    public void trySmsInfoUpload() {

        if (xmppConnectionService == null || xmppConnectionService.getAccounts() == null || !(xmppConnectionService.getAccounts().size() > 0)){
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
        } else {
            queryAppSync(myCogAccount);
        }
        xmppConnectionService.setSmsInfo(this);
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
                    if (response.data().getGlacierUsers().selected_twilionumber() != null) {
                        dbProfs = (getSmsProfileList(response.data().getGlacierUsers().selected_twilionumber()));
                    }
                    if (response.data().getGlacierUsers().add_user_to_purchase_numbers() != null) {
                        dbPurchaseNum = response.data().getGlacierUsers().add_user_to_purchase_numbers();
                    }
                    if (response.data().getGlacierUsers().add_user_to_SMS() != null){
                        add_user_to_sms = response.data().getGlacierUsers().add_user_to_SMS();
                  }
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

    private ArrayList<SmsProfile> getSmsProfileList(List<String> smsInfoList) {
        ArrayList<SmsProfile> smsProfList = new ArrayList<>();

        for (String deviceinfo : smsInfoList) {
            try {
                JSONObject jsonObject = new JSONObject(deviceinfo);

                SmsProfile prof = new SmsProfile(jsonObject);
                smsProfList.add(prof);



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
