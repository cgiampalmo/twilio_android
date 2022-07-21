package com.glaciersecurity.glaciermessenger.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.utils.SMSdbInfo;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PurchaseNumbers extends XmppActivity  implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {
    @Override
    protected void refreshUiReal() {

    }
    CardView area_code;
    Boolean numberPurchased = false;
    ArrayList<phone_num_details> availablePhoneNumbers;
    protected class AvailableNumberResponse {
        public ArrayList<phone_num_details> available_phone_numbers;
    }
    NumberListAdapter numberListAdapter;
    protected class phone_num_details{
        String phoneNumber;
        public String getPhone_number() {
            return phoneNumber;
        }
    }
    private class PurchaseNumResponse{
        String message;
        String data;
    }
    ConversationModel model;
    @Override
    void onBackendConnected() {
        if(xmppConnectionService != null) {
            SMSdbInfo smsinfo = new SMSdbInfo(xmppConnectionService);
            xmppConnectionService.setSmsInfo(smsinfo);
            try {
                if (numberPurchased) {
                    Thread thread = new Thread();
                    thread.sleep(500);
                    SMSdbInfo info = xmppConnectionService.getSmsInfo();
                    ArrayList<SmsProfile> smSdbInfo = info.getExistingProfs();
                    if (smSdbInfo.size() > 0) {
                        onBackPressed();
                    } else {
                        Thread thread1 = new Thread();
                        thread1.sleep(500);
                        if (numberPurchased)
                            onBackPressed();
                    }
                }
            }catch (InterruptedException ex){

            }
        }
    }

    public void getPhoneNumberList(String countryCode, String area_code) {
        String search_area_code = "";
        if(!(area_code.isEmpty() || area_code.equals(""))){
            search_area_code = "&AreaCode="+area_code;
        }
        Log.d("Glacier","getPhoneNumberList for "+countryCode +" areacode "+area_code);
        String getAvailableNumListUrl = this.getString(R.string.get_available_num_list_url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("areacode", area_code)
                .add("countryCode",countryCode)
                .build();
        Log.d("Glacier","getPhoneNumberList for "+countryCode + "adnd its url "+getAvailableNumListUrl);
        Request request = new Request.Builder()
                .url(getAvailableNumListUrl)
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Log.d("Glacier", "Response from server: " + responseBody);
            Gson gson = new Gson();
            AvailableNumberResponse availableNumberResponse = gson.fromJson(responseBody, AvailableNumberResponse.class);
            availablePhoneNumbers = availableNumberResponse.available_phone_numbers;
            TextView no_num = findViewById(R.id.no_num);
            if(availablePhoneNumbers.size() > 0){
                no_num.setVisibility(View.GONE);
            }else {
                no_num.setVisibility(View.VISIBLE);
            }
            Log.d("Glacier","availablePhoneNumbers size "+availablePhoneNumbers.size()+" Visibility "+no_num.getVisibility());
            numberListAdapter.notifyDataSetChanged();
        }catch (IOException ex){
            Log.e("Glacier", ex.getLocalizedMessage(), ex);
        }
    }
    protected void OnNumberClick(String number){
        Toast.makeText(this,"Purchasing number "+number,Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(PurchaseNumbers.this);
        builder.setMessage("Do you want to purchase number ?");
        builder.setTitle("Confirmation");
        builder.setCancelable(true);
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                PurchaseNum(number);
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void PurchaseNum(String number){
        String purchaseNumberUrl = this.getString(R.string.purchase_number_url);
        String identity = model.getIdentity();
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("purchaseNum", number)
                .add("username",identity)
                .build();
        Request request = new Request.Builder()
                .url(purchaseNumberUrl)
                .post(requestBody)
                .build();
        Log.d("Glacier", "request " + request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Gson gson = new Gson();
            PurchaseNumResponse purchaseNumResponse = gson.fromJson(responseBody, PurchaseNumResponse.class);
            if(purchaseNumResponse.message.equals("success")){
                Toast.makeText(PurchaseNumbers.this,"Number purchased successfully",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(PurchaseNumbers.this,"Failed to purchase. Please try again",Toast.LENGTH_LONG).show();
            }
            numberPurchased = true;
            onBackendConnected();
            Log.d("Glacier", "Response from server: " + responseBody);
        }catch (IOException ex){
            Log.e("Glacier", ex.getLocalizedMessage(), ex);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_number);
        setTitle("Purchase Twilio number");
        model = (ConversationModel) getApplicationContext();
        String[] country_codes = getResources().getStringArray(R.array.country_codes);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.dropdown_cc, country_codes);
        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.countrycode);
        autoCompleteTextView.setOnItemSelectedListener(this);
        autoCompleteTextView.setOnItemClickListener(this);
        autoCompleteTextView.setAdapter(arrayAdapter);
        area_code = findViewById(R.id.area_code);
        area_code.setVisibility(View.GONE);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        numberListAdapter = new NumberListAdapter(this);
        recyclerView.setAdapter(numberListAdapter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.aToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        TextView getAreaCode = findViewById(R.id.edit_gchat_number);
        ImageView SubmitareaCode = findViewById(R.id.get_area_code_num);
        SubmitareaCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String areaCode = getAreaCode.getText().toString().trim();
                Log.d("Glacier","Areacode entered : "+areaCode);
                //Toast.makeText(PurchaseNumbers.this,"Areacode entered : " + areaCode,Toast.LENGTH_LONG).show();
                TextView getcountrycode = findViewById(R.id.countrycode);
                String countryNamecode = getcountrycode.getText().toString().trim();
                String[] countrySplitCode = countryNamecode.split("-");
                String countryCode = countrySplitCode[1].trim();
                getPhoneNumberList(countryCode, areaCode);
            }
        });
    }
    public void onBackPressed(){
        super.onBackPressed();
        //onBackendConnected();
        finish();
        Intent intent = new Intent(this, SMSActivity.class);
        startActivity(intent);
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //Toast.makeText(this, "Item Clicked " , Toast.LENGTH_SHORT).show();
        TextView getcountrycode = findViewById(R.id.countrycode);
        String countryNamecode = getcountrycode.getText().toString().trim();
        String[] countrySplitCode = countryNamecode.split("-");
        String countryCode = countrySplitCode[1].trim();
        //Toast.makeText(this, "Item Clicked " + countryCode, Toast.LENGTH_SHORT).show();
        TextView getAreaCode = findViewById(R.id.edit_gchat_number);
        area_code.setVisibility(View.VISIBLE);
        String areacode = getAreaCode.getText().toString().trim();
        getPhoneNumberList(countryCode,areacode);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(this, "Item onItemSelected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Toast.makeText(this, "Item onNothingSelected", Toast.LENGTH_SHORT).show();
    }
}
