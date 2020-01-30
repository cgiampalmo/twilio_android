package com.glaciersecurity.glaciermessenger.entities;

public class LoginAccount {
    private static LoginAccount loginAccount = null;

    public enum LoginState {
        FORM_ENTRY, LOGIN_ATTEMPT, SIGN_IN_USER, RETRIEVE_VPN,
    }

    private String logUsername;
    private String logPassword;
    private String logOrgID;
    private LoginState loginState;


    private LoginAccount(){
        this.logUsername = null;
        this.logPassword = null;
        this.loginState = LoginState.FORM_ENTRY;
    }

    public static LoginAccount getInstance(){
        if (loginAccount == null){
            loginAccount = new LoginAccount();
        }
        return loginAccount;
    }
    public String getLogUsername() {
        return logUsername;
    }

    public void setLogUsername(String logUsername) {
        this.logUsername = logUsername;
    }

    public String getLogPassword() {
        return logPassword;
    }

    public void setLogPassword(String logPassword) {
        this.logPassword = logPassword;
    }

    public String getLogOrgID() {
        return logOrgID;
    }

    public void setLogOrgID(String logOrgID) {
        this.logOrgID = logOrgID;
    }

    public void wipeLoginAccount(){
        this.logUsername = "";
        this.logPassword = "";
        this.logOrgID = "";
    }

    public LoginState getLoginState() {
        return loginState;
    }

    public void setLoginState(LoginState loginState) {
        this.loginState = loginState;
    }
}
