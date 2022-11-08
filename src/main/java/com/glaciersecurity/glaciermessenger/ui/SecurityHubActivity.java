package com.glaciersecurity.glaciermessenger.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.ExpandableListItem;
import com.glaciersecurity.glaciermessenger.ui.adapter.AdapterListExpand;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

public class SecurityHubActivity extends XmppActivity {

    private AdapterListExpand mAdapter;
    private RecyclerView recyclerView;
    private View parent_view;


    private TextView issuesTitle;
    private RoundedImageView issuesIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(ThemeHelper.find(this));

        setContentView(R.layout.activity_security_hub);
        setSupportActionBar(findViewById(R.id.toolbar));
        configureActionBar(getSupportActionBar());
        setTitle(R.string.title_activity_security_hub);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initComponent();
    }

    private void initComponent() {
        parent_view = findViewById(android.R.id.content);
        issuesTitle = (TextView) findViewById(R.id.issues_title);
        issuesIcon = (RoundedImageView) findViewById(R.id.issues_icon);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new LineItemDecoration(this, LinearLayout.VERTICAL));
        recyclerView.setHasFixedSize(true);

    }

    private void initSecurityComponents() {
        List<ExpandableListItem> items = new ArrayList<ExpandableListItem>(initSecurityCheck());

        //set data and list adapter
        mAdapter = new AdapterListExpand(this, items);
        recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterListExpand.OnItemClickListener() {
            @Override
            public void onItemClick(View view, ExpandableListItem obj, int position) {
                if (obj.isExpandable) {
                    switch (obj.name){
                        case ("Core Connection"):
                            Intent coreActivity = new Intent(getApplicationContext(), OpenVPNActivity.class);
                            startActivity(coreActivity);
                            break;
                    }
                }
            }
        });
    }

    private ArrayList<ExpandableListItem> initSecurityCheck() {
        ArrayList<ExpandableListItem> security_results = new ArrayList<>();
        security_results.add(anomaliesDetectedListItem());
        security_results.add(screenLockListItem());
        security_results.add(latestGlacierListItem());
        security_results.add(latestOsListItem());
        security_results.add(appBioLockListItem());
        security_results.add(deviceBioLockListItem());
        security_results.add(coreConnectionListItem());

        isSecure();

        return security_results;
    };

    private boolean isNoAnomaliesDetected(){
        return !xmppConnectionService.getSecurityInfo().hasAnomalies();
    }

    private boolean hasScreenLock(){
        return xmppConnectionService.getSecurityInfo().hasScreenLock();
    }

    private boolean isApplock() {
        return getBooleanPreference(SettingsActivity.USE_BIOMETRICS, R.bool.enable_biometrics);
    }

    private boolean isDevicelock() {
        return xmppConnectionService.getSecurityInfo().hasBioLock();
    }

    private boolean isCoreConnection() {
        return xmppConnectionService.getSecurityInfo().isCoreEnabled();
    }

    private ExpandableListItem anomaliesDetectedListItem(){
        if (isNoAnomaliesDetected()) {
            return new ExpandableListItem(R.drawable.shield_system_safe_128, getString(R.string.system_safe),getString(R.string.no_anomalies));
        } else {
            return new ExpandableListItem(R.drawable.shield_system_safe_disabled_128, getString(R.string.system_unsafe),getString(R.string.anomalies), true);
        }
    }

    private ExpandableListItem screenLockListItem(){
        if (hasScreenLock()) {
            return new ExpandableListItem(R.drawable.device_lock_enabled, getString(R.string.screen_lock),getString(R.string.screen_lock_enabled));
        } else {
            return new ExpandableListItem(R.drawable.device_lock_disabled, getString(R.string.screen_lock),getString(R.string.screen_lock_disabled), true);
        }
    }

    private ExpandableListItem latestGlacierListItem(){
        boolean latestGlacier = xmppConnectionService.getSecurityInfo().isLatestGlacier();
        if (latestGlacier) {
            return new ExpandableListItem(R.drawable.ic_gchat_icon_security, getString(R.string.latest_updates_app),getString(R.string.up_to_date_app));
        }
            return new ExpandableListItem(R.drawable.ic_gchat_icon, getString(R.string.latest_updates_app),getString(R.string.update_glacier), true);

    }

    private ExpandableListItem latestOsListItem(){
        boolean latestOs = xmppConnectionService.getSecurityInfo().isLatestOS();
        if (latestOs) {
            return new ExpandableListItem(R.drawable.os_up_to_date2, getString(R.string.latest_updates_os),getString(R.string.up_to_date_os));
        }
            return new ExpandableListItem(R.drawable.os_out_of_date3, getString(R.string.latest_updates_os),getString(R.string.update_os), true);
    }

    private ExpandableListItem appBioLockListItem(){
        if (isApplock()) {
            return new ExpandableListItem(R.drawable.glacier_app_lock_enabled, getString(R.string.biometrics_app), getString(R.string.app_lock_enabled));
        } else {
            return new ExpandableListItem(R.drawable.glacier_app_lock_disabled,  getString(R.string.biometrics_app), getString(R.string.app_lock_disabled));
        }
    }

    private ExpandableListItem deviceBioLockListItem(){
        if (isDevicelock()) {
            return new ExpandableListItem(R.drawable.fingerprint_biometric_lock_128, getString(R.string.biometrics_dev), getString(R.string.bio_lock_enabled));
        } else {
            return new ExpandableListItem(R.drawable.fingerprint_biometric_lock_disabled_128,  getString(R.string.biometrics_dev), getString(R.string.bio_lock_disabled));
        }
    }

    private ExpandableListItem coreConnectionListItem(){
        if (isCoreConnection()) {
            return new ExpandableListItem(R.drawable.global_core_connection_128,  getString(R.string.core_connect), getString(R.string.core_connect_enabled), true, false);
        } else {
            return new ExpandableListItem(R.drawable.global_core_connection_128_disabled, getString(R.string.core_connect), getString(R.string.core_connect_disabled), true, false);
        }
    }

    private boolean isSecure(){
        if (xmppConnectionService.getSecurityInfo().isSecure()){
            issuesTitle.setText(R.string.no_issues_found);
            issuesIcon.setImageResource(R.drawable.securityhub_safe2);
            return true;
        } else {
            issuesTitle.setText(R.string.issues_found);
            issuesIcon.setImageResource(R.drawable.securityhub_notsafe_gray);
            return false;
        }
    }

    @Override
    protected void refreshUiReal() {

    }

    @Override
    void onBackendConnected() {
        try {
            if (xmppConnectionService != null) {
                xmppConnectionService.getOrgInfo().checkCurrentOrgInfo();
                initSecurityComponents();
            }
        } catch (Exception e){

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else {
            Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }




}
