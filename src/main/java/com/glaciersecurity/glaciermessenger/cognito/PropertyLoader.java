package com.glaciersecurity.glaciermessenger.cognito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.content.Context;
import android.content.res.AssetManager;
import com.glaciersecurity.glaciermessenger.utils.Log;

public class PropertyLoader {
    private Context context;
    private Properties properties;

    public PropertyLoader(Context context) {
        this.context = context;
        properties = new Properties();
    }

    public Properties getProperties(String FileName) {

        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(FileName);
            properties.load(inputStream);

        } catch (IOException e) {
            Log.e("AssetsPropertyReader",e.toString());
        }
        return properties;

    }

}