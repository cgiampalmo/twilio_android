package com.glaciersecurity.glaciermessenger.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class DownloadImageTask extends AsyncTask<Void,Void,Void>
{
    ImageView sentImg;
    String imageUrl;
    URL url = null;
    HttpURLConnection con = null;
    BufferedReader reader = null;
    Bitmap image;
    DownloadImageTask(ImageView sentImg,String url){
        this.sentImg = sentImg;
        this.imageUrl = url;
    }
    protected void onPreExecute() {
        //display progress dialog.

    }
    protected Void doInBackground(Void... params) {

        try {
            url = new URL(imageUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setConnectTimeout(30000);
            con.setReadTimeout(30000);
            String responseMsg = con.getResponseMessage();
            int response = con.getResponseCode();
            Log.d("Glacier","responseMsg "+responseMsg+ " response "+response);
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if (con != null) {
                con.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.d("GLACIER", "Error closing stream", e);
                }
            }
        }
        return null;
    }



    protected void onPostExecute(Void result) {
        // dismiss progress dialog and update ui
        //sentImg.setImageBitmap(result);
        InputStream is = null;
        try {
            is = con.getInputStream();
            image = BitmapFactory.decodeStream(is);
            sentImg.setImageBitmap(image);
        } catch (IOException e) {
            Log.d("Glacier",e.getMessage());
            e.printStackTrace();
        }
    }
}
