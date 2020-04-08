package com.glaciersecurity.glaciermessenger.entities;

import android.content.ContentValues;
import android.database.Cursor;

//ALF AM-388
public class CognitoAccount extends AbstractEntity {
    public static final String TABLENAME = "cognito_account";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ACCOUNT = "account";

    protected String username;
    protected String password;
    protected String account;

    public CognitoAccount(final String name, final String password, final String account) {
        this(java.util.UUID.randomUUID().toString(), name, password, account);
    }

    private CognitoAccount(final String uuid, final String name,
                    final String password, final String account) {
        this.uuid = uuid;
        this.username = name;
        this.password = password;
        this.account = account;
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(USERNAME, this.username);
        values.put(PASSWORD, this.password);
        values.put(ACCOUNT, this.account);
        values.put(UUID, uuid);
        return values;
    }

    public static CognitoAccount fromCursor(Cursor cursor) {
        return new CognitoAccount(cursor.getString(cursor.getColumnIndex(UUID)),
                cursor.getString(cursor.getColumnIndex(USERNAME)),
                cursor.getString(cursor.getColumnIndex(PASSWORD)),
                cursor.getString(cursor.getColumnIndex(ACCOUNT)));
    }

    public String getUserName() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
