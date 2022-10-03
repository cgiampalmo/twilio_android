package com.glaciersecurity.glaciermessenger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.NewSMSActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ContactListActivity extends XmppActivity implements OnSMSConversationClickListener{
    private String identity, convSid, Convtoken;
    RecyclerView recyclerView;
    ArrayList<ContactModel> arrayList = new ArrayList<ContactModel>();
    ContactAdapter adapter;
    Toolbar toolbar;
    private EditText phoneNumber;
    private Context mContext = this;
    ConversationModel cModel;
    FilterAdapter adapterSearch;
    RecyclerView recyclerViewSearch;
    Map<String, String> convContList;
    ImageView sendChatMessageButton;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contacts_list_view);
        recyclerView = findViewById(R.id.recycler_view);
        setTitle("New conversation");
        toolbar = (Toolbar) findViewById(R.id.aToolbar );
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        cModel = (ConversationModel) getApplicationContext();
        //checkPermission();
        arrayList = cModel.getArrayList();
        recyclerView.setLayoutManager((new LinearLayoutManager(this)));
        if(arrayList != null && !(arrayList.isEmpty())) {
            adapter = new ContactAdapter(this, arrayList, (OnSMSConversationClickListener) this);
            recyclerView.setAdapter(adapter);
        }
        if (getIntent().hasExtra("conv_sid")) {
            convSid = getIntent().getExtras().getString("conv_sid");
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        ArrayList aList = new ArrayList();
        convContList = cModel.getContConv();
        if(convContList != null && convContList.size() > 0) {
            for (Map.Entry<String, String> numList : convContList.entrySet()) {
                aList.add(numList.getKey());
            }
        }
        recyclerViewSearch = findViewById(R.id.choose_conversation_list);
        LinearLayoutManager layoutManagerSearch = new LinearLayoutManager(this);
        layoutManagerSearch .setStackFromEnd(true);
        layoutManagerSearch .setReverseLayout(true);
        recyclerViewSearch.setLayoutManager(layoutManagerSearch);
        adapterSearch = new FilterAdapter((OnSMSConversationClickListener) this,aList);
        recyclerViewSearch.setAdapter(adapterSearch);
        sendChatMessageButton = findViewById(R.id.button_submit_phonenumber);


        phoneNumber = findViewById(R.id.search_phone_number);
        phoneNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitEntry();
                    handled = true;
                }
                return handled;
            }
        });
        phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d("Glacier","onTextChanged "+charSequence);
                adapterSearch.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitEntry();
            }
        });
        TokenModel Atoken = new TokenModel();
        FloatingActionButton NewEnterNumber = findViewById(R.id.add_group_sms);
        NewEnterNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(mContext, NewSMSActivity.class);
//                String conv_Sid = "new";
//                startActivity(intent.putExtra("conv_sid",conv_Sid).putExtra("identity",identity).putExtra("conversationToken", Convtoken).putExtra("title","New message"));
                cModel.setConversationsClient(ConversationsManager.conversationsClient);
                Intent intent = new Intent(mContext, GroupSMS.class);
                String token = Atoken.getAccessToken();
                startActivity(intent.putExtra("identity",identity).putExtra("conversationToken",token));
            }
        });
    }

    private void submitEntry(){
        String toNumber = phoneNumber.getText().toString().replace("+1","").replace("(","").replace(")","").replace(" ","").replace("-","");
        if(toNumber.length() > 9)
            OnSMSConversationClick("", toNumber);
        else
            Toast.makeText(ContactListActivity.this, "Please enter valid number", Toast.LENGTH_SHORT).show();
    }
    /*private void checkPermission() {
        if (ContextCompat.checkSelfPermission(ContactListActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ContactListActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, 100);
        } else {
            Log.d("Glacier","getContactList ");
            getContactList();
        }
    }
    private void getContactList(){
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String sort = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC";
        Cursor cursor = getContentResolver().query(uri,null,null,null,sort);
        Log.d("Glacier","cursor "+cursor.getCount());
        Map<String, String> convContList = new HashMap<>();
        convContList = cModel.getContConv();
        if(cursor.getCount() > 0){
            while (cursor.moveToNext()){
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Uri uriPhone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                    String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?";
                    Cursor phoneCursor = getContentResolver().query(uriPhone, null, selection, new String[]{id}, null);
                    if (phoneCursor.moveToNext()) {
                        String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        ContactModel model = new ContactModel();
                        model.setName(name);

                        arrayList.add(model);
                        Log.d("Glacier","convContList "+convContList);

                        number = number.replaceAll(" ","");
                        number = number.replace("+1","");
                        number = number.replace("(","");
                        number = number.replace(")","");
                        number = number.replace("-","");
                        model.setNumber(number);
                        if(number.length() > 8 && ( convContList == null || !(convContList.size() > 0) || !convContList.containsKey(number))) {
                            if(convContList == null){
                                convContList = new HashMap<>();
                            }
                            convContList.put(number,"No sid");
                        }
                        phoneCursor.close();
                    }
                    Log.d("Glacier", "cursor arrayList " + arrayList.size());
                }
            }
            Log.d("Glacier","cursor final arrayList "+arrayList.size());
            cursor.close();
        }
        cModel.setContConv(convContList);
        recyclerView.setLayoutManager((new LinearLayoutManager(this)));
        adapter = new ContactAdapter(this,arrayList,(OnSMSConversationClickListener) this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.d("Glacier","getContactList ");
            getContactList();
        }else{
            //Toast.makeText(ContactListActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            checkPermission();
        }
    }*/
    public void OnSMSConversationClick(String conv_sid,String conv_name) {
        Log.d("Glacier","onConversationClick called ");
        String token = Convtoken;
        Intent intent = new Intent(this,smsConvActivity.class);
        startActivity(intent.putExtra("phoneNumber",conv_name).putExtra("identity",identity).putExtra("conversationToken", token).putExtra("title",conv_name).putExtra("conv_sid",conv_name));
    }
    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity));
    }
}
