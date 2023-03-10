package com.glaciersecurity.glaciermessenger.entities;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import com.glaciersecurity.glaciermessenger.utils.Log;

import com.glaciersecurity.glaciermessenger.Config;

//AM-487 and all encryption related functions below
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

//ALF AM-388
public class CognitoAccount extends AbstractEntity {
    public static final String TABLENAME = "cognito_account";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ORGANIZATION = "organization"; //added throughout for AM#53
    public static final String ACCOUNT = "account";

    //AM-487
    final private static String GLACIER_KEY_ALIAS = "_androidx_security_master_key_";
    final private static int KEY_SIZE = 256;

    protected String username;
    protected String password;
    protected String account;
    protected String organization;

    private Context cacontext;

    public CognitoAccount(final String name, final String password, final String organization, final String account, Context context) {
        this(java.util.UUID.randomUUID().toString(), name, password, organization, account, context);
    }

    private CognitoAccount(final String uuid, final String name,
                           final String password, final String organization, final String account) {
        this.uuid = uuid;
        this.username = name;
        this.password = password;
        this.organization = organization;
        this.account = account;
    }

    //AM-487
    private CognitoAccount(final String uuid, final String name,
                           final String password, final String organization, final String account, Context context) {
        this.cacontext = context;
        updateSharedPreferences(uuid, password, context);

        this.uuid = uuid;
        this.username = name;
        this.password = password;
        this.organization = organization;
        this.account = account;
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        values.put(USERNAME, this.username);
        values.put(PASSWORD, "gibberish"); //store this in database and use encryptedSharedPreferences
        values.put(ORGANIZATION, this.organization);
        values.put(ACCOUNT, this.account);
        values.put(UUID, uuid);
        return values;
    }

    public static CognitoAccount fromCursor(Cursor cursor) {
        return new CognitoAccount(cursor.getString(cursor.getColumnIndex(UUID)),
                cursor.getString(cursor.getColumnIndex(USERNAME)),
                cursor.getString(cursor.getColumnIndex(PASSWORD)),
                cursor.getString(cursor.getColumnIndex(ORGANIZATION)),
                cursor.getString(cursor.getColumnIndex(ACCOUNT)));
    }

    //AM-487
    public static CognitoAccount fromCursor(Cursor cursor, Context context) {
        String cauuid = cursor.getString(cursor.getColumnIndex(UUID));
        String capass = cursor.getString(cursor.getColumnIndex(PASSWORD));

        try {
            SharedPreferences spref = getEncryptedSharedPreferences(context);
            capass = spref.getString(cauuid, capass);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new CognitoAccount(cauuid,
                cursor.getString(cursor.getColumnIndex(USERNAME)),
                capass,
                cursor.getString(cursor.getColumnIndex(ORGANIZATION)),
                cursor.getString(cursor.getColumnIndex(ACCOUNT)));
    }

    public static void updateCognitoAccount(CognitoAccount account, Context context) {
        updateSharedPreferences(account.getUuid(), account.getPassword(), context);
    }

    private static void updateSharedPreferences(final String uuid,
                                                final String password, Context context) {

        try {
            SharedPreferences spref = getEncryptedSharedPreferences(context);
            SharedPreferences.Editor editor = spref.edit();
            editor.putString(uuid, password);
            editor.apply();
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteAccountInfo(Context context) {
        try {
            SharedPreferences spref = getEncryptedSharedPreferences(context);
            spref.edit().clear().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUserName() {
        return username;
    }

    public String getPassword() {
        if (password != null) {
            return password;
        }

        //AM-487
        String capassword = "gibberish";
        if (cacontext != null) {
            SharedPreferences spref = getEncryptedSharedPreferences(cacontext);
            capassword = spref.getString(uuid, password);
        }

        return capassword;
    }

    public String getOrganization() {
        return organization;
    }

    //AM-487 (next two)
    private static MasterKey getMasterKey(Context context) {
        try {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    GLACIER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE)
                    .build();

            return new MasterKey.Builder(context)
                    .setKeyGenParameterSpec(spec)
                    .build();
        } catch (Exception e) {
            Log.e(Config.LOGTAG, "Error on getting master key", e);
        }
        return null;
    }

    private static SharedPreferences getEncryptedSharedPreferences(Context context) {
        try {
            return EncryptedSharedPreferences.create(
                    context,
                    "secret_shared_prefs",
                    getMasterKey(context), // calling the method above for creating MasterKey
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(Config.LOGTAG, "Error on getting encrypted shared preferences", e);
        }

        return null;
    }
}
