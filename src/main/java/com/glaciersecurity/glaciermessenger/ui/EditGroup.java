package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import android.os.Bundle;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.widget.MultiContactAdapter;
import com.twilio.conversations.Attributes;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ErrorInfo;
import com.twilio.conversations.Participant;
import com.twilio.conversations.StatusListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EditGroup extends XmppActivity implements AddParticipantClickListener,RemoveParticipantClickListener{
    Toolbar toolbar;
    ConversationModel cModel;
    Conversation conversation;
    List<Participant> participants;
    ArrayList<ContactModel> arrayList;
    RecyclerView recyclerView1,recyclerView2;
    ParticipantAdapter adapter1;
    AddContactToGroupAdapter adapter2;
    Map<String, String> cList =new HashMap<>();
    ArrayList<ContactModel> groupArrayList = new ArrayList<ContactModel>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_group_sms);
        toolbar = (Toolbar) findViewById(R.id.aToolbar );
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        cModel = (ConversationModel) getApplicationContext();
        cList = cModel.getcList();
        conversation = cModel.getConversation();
        if(conversation == null){
            onBackPressed();
        }else {
            setTitle(conversation.getFriendlyName());
            participants = conversation.getParticipantsList();
            recyclerView1 = findViewById(R.id.recycler_view_existing);
            recyclerView1.setLayoutManager((new LinearLayoutManager(this)));
            adapter1 = new ParticipantAdapter(this, conversation, cList);
            recyclerView1.setAdapter(adapter1);
            cModel = (ConversationModel) getApplicationContext();
            arrayList = cModel.getArrayList();
            recyclerView2 = findViewById(R.id.recycler_view);
            recyclerView2.setLayoutManager((new LinearLayoutManager(this)));
            adapter2 = new AddContactToGroupAdapter(this, arrayList, cModel);
            recyclerView2.setAdapter(adapter2);
            getArrayList();
        }
    }
    public void getArrayList(){
        ArrayList<String> pList = new ArrayList<String>();
        groupArrayList.clear();
        participants = conversation.getParticipantsList();
        for (Participant participant : participants) {
            JSONObject participant_num = participant.getAttributes().getJSONObject();
            Log.d("Glacier",participant.getAttributes()+" participant_num "+participant_num);
            if(participant_num!=null) {
                try {
                    String participant_number = participant_num.getString("participant_number");
                    pList.add(participant_number);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(arrayList != null) {
            for (ContactModel conList : arrayList) {
                if (!pList.contains(conList.getNumber())) {
                    groupArrayList.add(conList);
                } else {
                    Log.d("Glacier", "pList contains " + conList.getNumber());
                }
            }
        }
        cModel.setGroupArrayList(groupArrayList);
        adapter1.notifyDataSetChanged();
        adapter2.notifyDataSetChanged();
    }
    @Override
    protected void refreshUiReal() {

    }

    @Override
    protected void onBackendConnected() {

    }

    @Override
    public void AddParticipant(String number,int position) {
        String proxyNumber = cModel.getProxyNumber();
        String phoneNumber = (number.length() == 10) ? "+1"+number : number;
        JSONObject part_json = new JSONObject();
        try {
            part_json.put("participant_number",number);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Attributes attributes = new Attributes(part_json);
        conversation.addParticipantByAddress(phoneNumber, proxyNumber, attributes, new StatusListener() {
            @Override
            public void onSuccess() {
                getArrayList();
                Log.d("Glacier","Participant added address "+phoneNumber);
            }
            public void onError(ErrorInfo errorInfo) {
                Log.e("Glacier", "Error on adding Participant: " + errorInfo.getMessage());
                Toast.makeText(EditGroup.this,"This user already exists in another conversation.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void RemoveParticipant(Participant participant,int position) {
        Log.d("Glacier","Remove participant "+participant.getIdentity().isEmpty() + " " + participant.getIdentity());
        if(participant.getIdentity().isEmpty()) {
            conversation.removeParticipant(participant, new StatusListener() {
                @Override
                public void onSuccess() {
                    Log.d("Glacier", "Participant deleted address " + participant.getAttributes() + " " + participant.getSid() + " " + participant.getIdentity());
                    getArrayList();

                /*adapter1.notifyDataSetChanged();
                adapter2.notifyDataSetChanged();*/
                    //adapter1.notifyItemRemoved(position);
                    //adapter2.notifyItemInserted();
                }
            });
        }
        else{
            Toast.makeText(this, "Admin participant cannot be deleted.", Toast.LENGTH_LONG).show();
        }
    }
}
interface AddParticipantClickListener {
    void AddParticipant(String number,int position);
}
interface RemoveParticipantClickListener {
    void RemoveParticipant(Participant participant,int position);
}