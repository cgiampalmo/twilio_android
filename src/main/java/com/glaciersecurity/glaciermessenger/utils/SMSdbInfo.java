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
    private SmsProfile smsInfo;
    private ArrayList<SmsProfile> dbProfs = new ArrayList<>();

    public SMSdbInfo(XmppConnectionService xmppConn) {
        xmppConnectionService = xmppConn;
        trySmsInfoUpload();
    }

    public ArrayList<SmsProfile> getExistingProfs(){
        return dbProfs;
    }

//    private GraphQLCall.Callback<GetGlacierUsersQuery.Data> getUserCallback = new GraphQLCall.Callback<GetGlacierUsersQuery.Data>() {
//        @Override
//        public void onResponse(@Nonnull Response<GetGlacierUsersQuery.Data> response) {
//            Log.i("Results", "RES...");
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (response != null) {
//                        if (response.data().getGlacierUsers() != null) {
//                            List<String> smsprofile = updateSmsDeviceList(response.data().getGlacierUsers().selected_twilionumber());
//
//                            }
//                        }
//
//                }
//            });
//
//
//
//
//        }
//
//        @Override
//        public void onFailure(@Nonnull ApolloException e) {
//            Log.i("Results", e.toString());
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    handleLoginFailure();
//                }
//            });
//
//
//
//        }
//    };




    private void trySmsInfoUpload() {

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
                    dbProfs.addAll(getSmsProfileList(response.data().getGlacierUsers().selected_twilionumber()));
//
                } else {
                    Log.i("SmsInfo", "No sms profiles in response from server");
                }
            }).start();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("SecurityInfo", "Error getting SecurityInfo");
        }
    };

    private ArrayList<SmsProfile> getSmsProfileList(List<String> smsInfoList) {
        ArrayList<SmsProfile> smsProfList = new ArrayList<>();
//        if (securityInfoList == null || securityInfoList.size() == 0) {
//            seclist.add(smsInfo.toJsonString());
//            return seclist;
//        }

        for (String deviceinfo : smsInfoList) {
            try {
                JSONObject jsonObject = null;
                    jsonObject = new JSONObject(deviceinfo);
                    String number = (String) jsonObject.get("text");
                    String id = (String) jsonObject.get("id");
                    SmsProfile prof = new SmsProfile(jsonObject);
                    smsProfList.add(prof);



                } catch (Exception e) {
                    e.printStackTrace();

                }
        }
        return smsProfList;
    }

//    private void updateSecurityHubInfo(GetGlacierUsersQuery.GetGlacierUsers gusers, List<String> seclist) {
//        UpdateGlacierUsersInput ginput = UpdateGlacierUsersInput.builder().organization(gusers.organization())
//                .username(gusers.username())
//                .securityInfo(seclist)
//                .build();
//        UpdateGlacierUsersMutation updateMutation = UpdateGlacierUsersMutation.builder().input(ginput).build();
//
//        appsyncclient.mutate(updateMutation).enqueue(updateUsersCallback);
//    }

//    private GraphQLCall.Callback<UpdateGlacierUsersMutation.Data> updateUsersCallback = new GraphQLCall.Callback<UpdateGlacierUsersMutation.Data>() {
//        @Override
//        public void onResponse(@Nonnull Response<UpdateGlacierUsersMutation.Data> response) {
//            if (response != null) {
//                if (response.data().updateGlacierUsers() != null) {
//                    Log.d("SecurityInfo", "SecurityInfo updated");
//                    needsUpdate = false;
//                } else {
//                    Log.i("SecurityInfo", "No SecurityInfo update response from server");
//                }
//            } else {
//                Log.i("SecurityInfo", "No SecurityInfo update response from server");
//            }
//        }
//
//        @Override
//        public void onFailure(@Nonnull ApolloException e) {
//            Log.e("SmsInfo", "Error updating SmsInfo");
//        }
//    };

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
