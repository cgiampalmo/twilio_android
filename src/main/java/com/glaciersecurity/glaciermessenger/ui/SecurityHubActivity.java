package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;

import static com.glaciersecurity.glaciermessenger.ui.ActionBarActivity.configureActionBar;

public class SecurityHubActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(ThemeHelper.find(this));

        setContentView(R.layout.activity_security_hub);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
        setTitle(R.string.title_activity_security_hub);
    }
}
