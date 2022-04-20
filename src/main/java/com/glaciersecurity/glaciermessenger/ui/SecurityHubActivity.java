package com.glaciersecurity.glaciermessenger.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.ExpandableListItem;
import com.glaciersecurity.glaciermessenger.ui.adapter.AdapterListExpand;
import com.glaciersecurity.glaciermessenger.utils.ThemeHelper;
import com.google.android.material.snackbar.Snackbar;
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.List;

import static com.glaciersecurity.glaciermessenger.ui.ActionBarActivity.configureActionBar;

public class SecurityHubActivity extends AppCompatActivity {

    private AdapterListExpand mAdapter;
    private RecyclerView recyclerView;

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

        initComponent();

    }

    private void initComponent() {
        issuesTitle = (TextView) findViewById(R.id.issues_title);
        issuesIcon = (RoundedImageView) findViewById(R.id.issues_icon);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new LineItemDecoration(this, LinearLayout.VERTICAL));
        recyclerView.setHasFixedSize(true);

        List<ExpandableListItem> items = new ArrayList<ExpandableListItem>(initSecurityCheck());


        //set data and list adapter
        mAdapter = new AdapterListExpand(this, items);
        recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterListExpand.OnItemClickListener() {
            @Override
            public void onItemClick(View view, ExpandableListItem obj, int position) {

            }
        });

    }

    private ArrayList<ExpandableListItem> initSecurityCheck() {
        ArrayList<ExpandableListItem> security_results = new ArrayList<>();
        security_results.add(isNoAnomoliesDetected());
        security_results.add(isScreenLock());
        security_results.add(isLatestOS());
        security_results.add(isFingerPrintEnabled());
        security_results.add(isCoreConnectionEnabled());

        isSecure();

        return security_results;
    };


    private ExpandableListItem isNoAnomoliesDetected(){
        if (true) {
            return new ExpandableListItem(R.drawable.ic_baseline_security_24, getString(R.string.system_safe),getString(R.string.no_anomalies));
        } else {
            return new ExpandableListItem(R.drawable.ic_baseline_gpp_bad_24, getString(R.string.system_unsafe),getString(R.string.anomalies));
        }
    }

    private ExpandableListItem isScreenLock(){
        if (true) {
            return new ExpandableListItem(R.drawable.ic_lock_white_18dp, getString(R.string.screen_lock),getString(R.string.screen_lock_enabled));
        } else {
            return new ExpandableListItem(R.drawable.ic_lock_open_white_24dp, getString(R.string.screen_lock),getString(R.string.screen_lock_disabled));
        }
    }

    private ExpandableListItem isLatestOS(){
        if (true) {
            return new ExpandableListItem(R.drawable.ic_baseline_security_update_good_24, getString(R.string.latest_updates),getString(R.string.up_to_date));
        } else {
            return new ExpandableListItem(R.drawable.ic_baseline_security_update_24, getString(R.string.latest_updates),getString(R.string.update_both));
        }
    }

    private ExpandableListItem isFingerPrintEnabled(){
        if (true) {
            return new ExpandableListItem(R.drawable.ic_baseline_fingerprint_24, getString(R.string.biometrics), getString(R.string.bio_lock_disabled));
        } else {
            return new ExpandableListItem(R.drawable.ic_baseline_security_update_warning_24,  getString(R.string.biometrics), getString(R.string.bio_lock_disabled));
        }
    }

    private ExpandableListItem isCoreConnectionEnabled(){
        if (true) {
            return new ExpandableListItem(R.drawable.ic_baseline_vpn_lock_24,  getString(R.string.core_connect), getString(R.string.core_connect_enabled));
        } else {
            return new ExpandableListItem(R.drawable.ic_baseline_vpn_key_off_24, getString(R.string.core_connect), getString(R.string.core_connect_disabled));
        }
    }

    private boolean isSecure(){
        // if (isLatestOS() && isNoAnomoliesDetected() && isScreenLock()){
        if (true){
            issuesTitle.setText(R.string.no_issues_found);
            issuesIcon.setImageResource(R.drawable.ic_baseline_check_circle_24);
            return true;
        } else {
            issuesTitle.setText(R.string.issues_found);
            issuesIcon.setImageResource(R.drawable.ic_baseline_warning_24);
            return false;
        }
    }




}
