package com.glaciersecurity.glaciercore.api;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;

public class CoreInstallActivity extends Activity {

    //HONEYBADGER - AM_76 launch playstore if core is not found

    public void launchPlayStoreCore(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.glacier_core_https)));
        startActivity(intent);

        Toast.makeText(this, "glacier_core_install", Toast.LENGTH_SHORT).show();
    }
}