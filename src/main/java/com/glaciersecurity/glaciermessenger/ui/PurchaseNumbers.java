package com.glaciersecurity.glaciermessenger.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.entities.SmsProfile;
import com.glaciersecurity.glaciermessenger.ui.util.Tools;
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
    private String lastWaitMsg = null;
    private TextView waitTextField = null;
    private android.app.AlertDialog waitDialog = null;
    CardView area_code_view;
    Boolean numberPurchased = false;
    ArrayList<phone_num_details> availablePhoneNumbers;
    TextView getAreaCode;
    protected class AvailableNumberResponse {
        public ArrayList<phone_num_details> available_phone_numbers;
    }
    NumberListAdapter numberListAdapter;
    protected class phone_num_details{
        String phoneNumber;
        public String getPhone_number() {
            return Tools.reformatNumber(phoneNumber);
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
            //xmppConnectionService.updateSmsInfo();
            try {
                if (numberPurchased) {
                    Thread thread = new Thread();
                    thread.sleep(500);
                    onBackPressed();

                }
            }catch (InterruptedException ex){

            }
        }
    }

    public void getPhoneNumberList(String countryCode, String area_code) {
        //Log.d("Glacier","getPhoneNumberList for "+countryCode +" areacode "+area_code);
        String getAvailableNumListUrl = this.getString(R.string.get_available_num_list_url);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("areaCode", area_code)
                .add("countryCode",countryCode)
                .build();
        //Log.d("Glacier","getPhoneNumberList for "+countryCode + "and its url "+getAvailableNumListUrl);
        Request request = new Request.Builder()
                .url(getAvailableNumListUrl)
                .post(requestBody)
                .addHeader("API-Key", xmppConnectionService.getApplicationContext().getResources().getString(R.string.twilio_token))
                .build();
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Log.d("Glacier", "Response from server: " + responseBody);
            Gson gson = new Gson();
            AvailableNumberResponse availableNumberResponse = gson.fromJson(responseBody, AvailableNumberResponse.class);

            if (availableNumberResponse == null){
                Toast.makeText(getApplicationContext(),"SMS not currently available" ,Toast.LENGTH_LONG).show();
                closeWaitDialog();
                return;
            }
            availablePhoneNumbers = availableNumberResponse.available_phone_numbers;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        TextView no_num = findViewById(R.id.no_num);
                        if(availablePhoneNumbers.size() > 0){
                            no_num.setVisibility(View.GONE);
                        }else {
                            no_num.setVisibility(View.VISIBLE);
                        }
                        //Log.d("Glacier","availablePhoneNumbers size "+availablePhoneNumbers.size()+" Visibility "+no_num.getVisibility());
                        numberListAdapter.notifyDataSetChanged();
                        closeWaitDialog();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"SMS not currently available" ,Toast.LENGTH_LONG).show();
                        closeWaitDialog();

                    }
                }
            });


        }catch (Exception ex){
            Log.d("Glacier", ex.getLocalizedMessage(), ex);
            closeWaitDialog();
        }
    }
    protected void OnNumberClick(String number){
        AlertDialog.Builder builder = new AlertDialog.Builder(PurchaseNumbers.this);
        builder.setMessage("Do you want to add number "+Tools.reformatNumber(number)+"?");
        builder.setTitle("Confirmation");
        builder.setCancelable(true);
        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showWaitDialog("Purchasing number");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            PurchaseNum(number);
                        } catch (Exception e) {
                            closeWaitDialog();

                        }
                        closeWaitDialog();


                    }
                }).start();
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void PurchaseNum(String number){
        String purchaseNumberUrl = this.getString(R.string.purchase_number_url);
        String identity = xmppConnectionService.getAccounts().get(0).getUsername();
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("purchaseNum", number)
                .add("username",identity)
                .build();
        Request request = new Request.Builder()
                .url(purchaseNumberUrl)
                .post(requestBody)
                .addHeader("API-Key", xmppConnectionService.getApplicationContext().getResources().getString(R.string.twilio_token))
                .build();
        //Log.d("Glacier", "request " + request);
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response != null && response.body() != null) {
                responseBody = response.body().string();
            }
            Gson gson = new Gson();
            PurchaseNumResponse purchaseNumResponse = gson.fromJson(responseBody, PurchaseNumResponse.class);
            if(purchaseNumResponse.message != null && purchaseNumResponse.message.equals("success")){
                runOnUiThread(() -> {
                    Toast.makeText(PurchaseNumbers.this,"Number added successfully",Toast.LENGTH_LONG).show();
                    closeWaitDialog();
                });
            }else{
                runOnUiThread(() -> {
                    Toast.makeText(PurchaseNumbers.this,"Failed to add. Please try again",Toast.LENGTH_LONG).show();
                    closeWaitDialog();
                });
            }
            numberPurchased = true;
            onBackendConnected();
            //Log.d("Glacier", "Response from server: " + responseBody);
        }catch (Exception ex){
            Log.e("Glacier", ex.getLocalizedMessage(), ex);
            closeWaitDialog();

        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_number);
        setTitle("Add number");
        model = (ConversationModel) getApplicationContext();
        String[] country_codes = getResources().getStringArray(R.array.country_codes);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.dropdown_cc, country_codes);
        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.countrycode);
        autoCompleteTextView.setOnItemSelectedListener(this);
        autoCompleteTextView.setOnItemClickListener(this);
        autoCompleteTextView.setAdapter(arrayAdapter);
        area_code_view = findViewById(R.id.area_code);
        getAreaCode = findViewById(R.id.edit_purchase_area_code);
        area_code_view.setVisibility(View.GONE);
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

        getAreaCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitAreaCode();
                    handled = true;
                }
                return handled;
            }
        });
        ImageView SubmitareaCode = findViewById(R.id.get_area_code_num);
        SubmitareaCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitAreaCode();
            }
        });
    }

    private void submitAreaCode(){
        String areaCode = getAreaCode.getText().toString().trim();
        Log.d("Glacier","Areacode entered : "+areaCode);
        //Toast.makeText(PurchaseNumbers.this,"Areacode entered : " + areaCode,Toast.LENGTH_LONG).show();
        TextView getcountrycode = findViewById(R.id.countrycode);
        String countryNamecode = getcountrycode.getText().toString().trim();
        String[] countrySplitCode = countryNamecode.split("-");
        String countryCode = countrySplitCode[1].trim();
        new Thread(new Runnable() {
            public void run() {
                getPhoneNumberList(countryCode,areaCode);
            }
        }).start();
    }
    public void onBackPressed(){
        super.onBackPressed();
        //onBackendConnected();
        finish();
        Intent intent = new Intent(this, SMSActivity.class);
        intent.putExtra("ProxyNum",model.getProxyNumber());
        startActivity(intent);
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //Toast.makeText(this, "Item Clicked " , Toast.LENGTH_SHORT).show();
        showWaitDialog("Searching...");
        TextView getcountrycode = findViewById(R.id.countrycode);
        String countryNamecode = getcountrycode.getText().toString().trim();
        String[] countrySplitCode = countryNamecode.split("-");
        String countryCode = countrySplitCode[1].trim();
        //Toast.makeText(this, "Item Clicked " + countryCode, Toast.LENGTH_SHORT).show();
        area_code_view.setVisibility(View.VISIBLE);
        String areacode = getAreaCode.getText().toString().trim();
        new Thread(new Runnable() {
            public void run() {
                getPhoneNumberList(countryCode,areacode);
                }
        }).start();

    }

    public void closeWaitDialog() {
        if (waitDialog != null) {
            waitDialog.dismiss();
            //ALF AM-190
            waitDialog = null;
            lastWaitMsg = null;
            waitTextField = null;
        }
    }

    public void showWaitDialog(String message) {
        //ALF AM-202 extended also check if Activity is finishing
        if (this.isFinishing()) {
            return;
        }

        //ALF AM-190
        if (lastWaitMsg != null && message.equalsIgnoreCase(lastWaitMsg)) {
            return;
        } else if (waitDialog != null && waitTextField != null) {
            waitTextField.setText(message);
            return;
        }

        lastWaitMsg = message;
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_wait, null);
        waitTextField = layout.findViewById(R.id.status_message);
        waitTextField.setText(message);

        //AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PurchaseNumbers.this);
        builder.setView(layout);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setTitle("Please Wait");

        waitDialog = builder.create();
        waitDialog.show();
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
