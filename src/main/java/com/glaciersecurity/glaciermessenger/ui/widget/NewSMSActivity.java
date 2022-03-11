package com.glaciersecurity.glaciermessenger.ui.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.ConversationModel;
import com.glaciersecurity.glaciermessenger.ui.SMSActivity;
import com.glaciersecurity.glaciermessenger.ui.smsConvActivity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class NewSMSActivity extends AppCompatActivity {
    private String identity, convSid, Convtoken;
    private EditText writeMessageEditText,phoneNumber;
    private Context mContext = this;
    Toolbar toolbar;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_sms);
        setTitle("New SMS ");
        toolbar = (Toolbar) findViewById(R.id.aToolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        if (getIntent().hasExtra("conv_sid")) {
            convSid = getIntent().getExtras().getString("conv_sid");
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        writeMessageEditText = findViewById(R.id.edit_gchat_message);
        phoneNumber = findViewById(R.id.edit_gchat_number);

        Button sendChatMessageButton = findViewById(R.id.button_gchat_send);
        sendChatMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Glacier","Button clicked "+writeMessageEditText.getText().toString());
                String messageBody = writeMessageEditText.getText().toString();
                String toNumber = phoneNumber.getText().toString();
                if (messageBody.length() > 0) {
                    Intent intent = new Intent(mContext, smsConvActivity.class);
                    startActivity(intent.putExtra("conv_sid",toNumber).putExtra("identity",identity).putExtra("conversationToken", Convtoken).putExtra("phoneNumber",toNumber).putExtra("messageBody",messageBody).putExtra("title",toNumber));
                }
            }
        });
    }
    public void onBackPressed(){
        super.onBackPressed();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity).putExtra("token",Convtoken));
    }
}