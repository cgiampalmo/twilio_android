package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;

import java.util.ArrayList;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
public class NewSMSActivity extends AppCompatActivity implements OnSMSConversationClickListener {
    private String identity, convSid, Convtoken;
    private EditText writeMessageEditText,phoneNumber;
    private Context mContext = this;
    Toolbar toolbar;
    FilterAdapter adapter;
    RecyclerView recyclerView;
    ConversationModel cModel;
    Map<String, String> convContList;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_sms);
        setTitle("New SMS ");
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        cModel = (ConversationModel) getApplicationContext();
        convContList = cModel.getContConv();
        ArrayList aList = new ArrayList();
        if(convContList.size() > 0) {
            for (Map.Entry<String, String> numList : convContList.entrySet()) {
                aList.add(numList.getKey());
            }
        }
        Log.d("Glacier","NewSMS aList "+aList+ aList.size());
        recyclerView = findViewById(R.id.choose_conversation_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FilterAdapter((OnSMSConversationClickListener) this,aList);
        recyclerView.setAdapter(adapter);

        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        if (getIntent().hasExtra("conv_sid")) {
            convSid = getIntent().getExtras().getString("conv_sid");
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        writeMessageEditText = findViewById(R.id.edit_gchat_message);
        phoneNumber = findViewById(R.id.edit_gchat_number);
        phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d("Glacier","onTextChanged "+charSequence);
                adapter.getFilter().filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        Button sendChatMessageButton = findViewById(R.id.button_gchat_send);
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String toNumber = phoneNumber.getText().toString().replace("+1","").replace("(","").replace(")","").replace(" ","").replace("-","");
                if(toNumber.length() > 9)
                    OnSMSConversationClick("", toNumber);
                else
                    Toast.makeText(NewSMSActivity.this, "Please enter valid number", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity).putExtra("token",Convtoken));
    }

    @Override
    public void OnSMSConversationClick(String connv_sid, String conv_name) {
        if(convContList.size() > 0){
            String sid = convContList.get(conv_name);
            Log.d("Glacier","OnSMSConversationClick sid "+sid);
            Intent intent = new Intent(this,smsConvActivity.class);
            if(sid != null && !(sid.trim().equals("No sid"))){
                startActivity(intent.putExtra("conv_sid",sid).putExtra("identity",identity).putExtra("conversationToken", Convtoken).putExtra("title",conv_name).putExtra("title",conv_name));
            }else{
                startActivity(intent.putExtra("conv_sid",conv_name).putExtra("identity",identity).putExtra("conversationToken", Convtoken).putExtra("title",conv_name).putExtra("title",conv_name));
            }
        }
    }
}