package com.glaciersecurity.glaciermessenger.ui;


import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.services.ConnectivityReceiver;

public class OpenVPNActivity extends XmppActivity {

    private ConnectivityReceiver connectivityReceiver; //CMG AM-41
    private OpenVPNFragment openVPNFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.openvpn_activity);
        Toolbar tb = findViewById(R.id.toolbar);
        //tb.setTitle(R.string.action_glaciervpn);
        setSupportActionBar(tb);
        configureActionBar(getSupportActionBar());
        if (savedInstanceState != null){
            Fragment frag = getSupportFragmentManager().findFragmentByTag("openVpnFragment");


        }
        if (savedInstanceState == null) {
            openVPNFragment = new OpenVPNFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container, openVPNFragment).commit();

        } else {
            openVPNFragment = (OpenVPNFragment) getSupportFragmentManager()
                    .findFragmentByTag("openVpnFragment");
        }
       // connectivityReceiver = new ConnectivityReceiver(this);

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.vpn_connection, menu);
//        return true;
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        getSupportFragmentManager().putFragment(outState, "openVpnFragment", openVPNFragment);
    }
@Override
protected void onStart() {
    super.onStart();
    //registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
}

    @Override
    protected void onStop() {
        super.onStop();
       //unregisterReceiver(connectivityReceiver);
    }

//    //CMG AM-41
//    @Override
//    public void onNetworkConnectionChanged(boolean isConnected) {
//        if (openVPNFragment != null){
//            if (isConnected) {
//                openVPNFragment.onConnected();
//            } else {
//                openVPNFragment.onDisconnected();
//            }
//        }
//    }
    @Override
    void onBackendConnected() {
        // nothing to do
    }

    public void refreshUiReal() {
    }

}
