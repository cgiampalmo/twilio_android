package com.glaciersecurity.glaciermessenger.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.BoolRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;

import com.glaciersecurity.glaciermessenger.R;

public abstract class ActionBarActivity extends AppCompatActivity {
    public static void configureActionBar(ActionBar actionBar) {
        configureActionBar(actionBar, true);
    }

    public static void configureActionBar(ActionBar actionBar, boolean upNavigation) {
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(upNavigation);
            actionBar.setDisplayHomeAsUpEnabled(upNavigation);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //CMG AM-137
    protected void initializeScreenshotSecurity() {
        if (isScreenSecurityEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
    public boolean isScreenSecurityEnabled() {
        return getBooleanPreference("screen_security", R.bool.screen_security);
    }

    protected SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }
    protected boolean getBooleanPreference(String name, @BoolRes int res) {
        return getPreferences().getBoolean(name, getResources().getBoolean(res));
    }
}