package com.glaciersecurity.glaciermessenger.utils;

import android.os.Build;

import androidx.annotation.NonNull;

import com.amazonaws.amplify.generated.graphql.GetGlacierOrganizationQuery;
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
import com.glaciersecurity.glaciermessenger.services.XmppConnectionService;

import java.util.Locale;


public class OrgInfo {
    private XmppConnectionService xmppConnectionService;
    private AWSAppSyncClient appsyncclient;

    private boolean securityhub_data_enabled= false;
    private boolean sms_enabled= false;
    private boolean upload_enabled= false;

    public boolean isSms_enabled() {
        return sms_enabled;
    }

    public void setSms_enabled(boolean sms_enabled) {
        this.sms_enabled = sms_enabled;
    }

    public boolean isSecurityhub_data_enabled() {
        return securityhub_data_enabled;
    }

    public void setSecurityhub_data_enabled(boolean securityhub_data_enabled) {
        this.securityhub_data_enabled = securityhub_data_enabled;
    }

    public boolean isUpload_enabled() {
        return upload_enabled;
    }

    public void setUpload_enabled(boolean upload_enabled) {
        this.upload_enabled = upload_enabled;
    }

    public OrgInfo(XmppConnectionService xmppConn) {
        xmppConnectionService = xmppConn;
    }

    private GraphQLCall.Callback<GetGlacierOrganizationQuery.Data> getOrganizationCallback = new GraphQLCall.Callback<GetGlacierOrganizationQuery.Data>() {
        @Override
        public void onResponse(@NonNull Response<GetGlacierOrganizationQuery.Data> response) {

                    if (response != null) {
                        if (response.data().getGlacierOrganization() != null) {
                            if (response.data().getGlacierOrganization().securityhub_data_enabled() != null) {
                                securityhub_data_enabled = response.data().getGlacierOrganization().securityhub_data_enabled();
                            }
                            if (response.data().getGlacierOrganization().sms_enabled() != null) {
                                sms_enabled = response.data().getGlacierOrganization().sms_enabled();
                            }
                            if (response.data().getGlacierOrganization().upload_enabled() != null) {
                                upload_enabled = response.data().getGlacierOrganization().upload_enabled();
                            }
                            xmppConnectionService.setOrgInfo(OrgInfo.this);
                        }
                    }
        }

        @Override
        public void onFailure(@NonNull ApolloException e) {

        }
    };

    private void queryAppSync(CognitoAccount myCogAccount) {
        if (myCogAccount == null) {
            final Account account = xmppConnectionService.getAccounts().get(0);
            myCogAccount = xmppConnectionService.databaseBackend.getCognitoAccount(account);
        }

        appsyncclient = AWSAppSyncClient.builder()
                .context(xmppConnectionService.getApplicationContext())
                .awsConfiguration(new AWSConfiguration(xmppConnectionService.getApplicationContext()))
                .build();

        appsyncclient.query(GetGlacierOrganizationQuery.builder()
                        .organization(myCogAccount.getOrganization())
                        .build())
                .responseFetcher(AppSyncResponseFetchers.NETWORK_ONLY)
                .enqueue(getOrganizationCallback);

    }
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

    public void checkCurrentOrgInfo() {
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



}
