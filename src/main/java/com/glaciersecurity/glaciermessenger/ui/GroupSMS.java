package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.XmppActivity;
import com.glaciersecurity.glaciermessenger.ui.widget.MultiContactAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GroupSMS  extends XmppActivity implements OnSMSConversationClickListener {
    private String identity, convSid, Convtoken;
    Toolbar toolbar;
    ConversationModel cModel;
    RecyclerView recyclerView;
    ArrayList<ContactModel> arrayList = new ArrayList<ContactModel>();
    MultiContactAdapter adapter;
    TextView tvEmpty;
    private Context mContext = this;

    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_sms);
        recyclerView = findViewById(R.id.recycler_view);
        toolbar = (Toolbar) findViewById(R.id.aToolbar );
        setSupportActionBar(toolbar);
        setTitle("Create Group SMS ");
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        cModel = (ConversationModel) getApplicationContext();
        arrayList = cModel.getArrayList();
        Log.d("Glacier","ArrayList size "+ arrayList.isEmpty() + arrayList);
        recyclerView.setLayoutManager((new LinearLayoutManager(this)));
        if (getIntent().hasExtra("identity")) {
            identity = getIntent().getExtras().getString("identity");
            Convtoken = getIntent().getExtras().getString("conversationToken");
        }
        tvEmpty = findViewById(R.id.tv_empty);
        if(!arrayList.isEmpty()) {
            adapter = new MultiContactAdapter(this, arrayList, tvEmpty);
            recyclerView.setAdapter(adapter);
        }
        FloatingActionButton StartGroupChat = findViewById(R.id.button_num_no_sms);
        EditText group_name = findViewById(R.id.edit_gchat_name);
        EditText gchat_number = findViewById(R.id.edit_gchat_number);


        StartGroupChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, smsConvActivity.class);
                String conv_Sid = "Group";
                String groupName = group_name.getText().toString();
                String gchat_numbers = gchat_number.getText().toString().replace("+1","").replace("(","").replace(")","").replace(" ","").replace("-","");
                if( tvEmpty.getText().equals("-") && gchat_numbers.isEmpty()){
                    Toast.makeText(mContext, "Please select Contacts by long press", Toast.LENGTH_SHORT).show();
                }else if(groupName.isEmpty()){
                    Toast.makeText(mContext,"Please enter valid group name", Toast.LENGTH_SHORT).show();
                }
                else {
                    String Contacts = "";
                    if(!(tvEmpty.getText().equals("-")))
                        Contacts = tvEmpty.getText().toString().replace("+1", "").replace("(", "").replace(")", "").replace(" ", "").replace("-", "");
                    if(!gchat_numbers.isEmpty()){
                        Contacts = Contacts.isEmpty() ? gchat_numbers : Contacts+","+gchat_numbers;
                    }
                    String[] Contact_list = Contacts.split(",");
                    startActivity(intent.putExtra("conv_sid", conv_Sid).putExtra("identity", identity).putExtra("conversationToken", Convtoken).putExtra("title", groupName).putExtra("groupName", groupName).putExtra("GroupContact",Contact_list).putExtra("conversationToken",Convtoken).putExtra("is_group",true));
                }
            }
        });
    }

    @Override
    public void OnSMSConversationClick(String connv_sid, String conv_name) {

    }
    public void onBackPressed(){
        super.onBackPressed();
        Toast.makeText(getApplicationContext(),"onBackPressed",Toast.LENGTH_SHORT).show();
        finish();
        Intent intent = new Intent(mContext, SMSActivity.class);
        startActivity(intent.putExtra("account",identity));
    }
}
