package com.glaciersecurity.glaciermessenger.ui;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.R;
import com.google.gson.JsonArray;
import com.twilio.conversations.CallbackListener;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.Participant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {
    Conversation conversation;
    RemoveParticipantClickListener listener;
    Map<String, String> cList;

    ParticipantAdapter(RemoveParticipantClickListener listener, Conversation conversation,Map cList){
        this.listener = listener;
        this.conversation = conversation;
        this.cList = cList;
    }

    @NonNull
    @Override
    public ParticipantAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View conView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_contacts, parent, false);
        return new ParticipantAdapter.ViewHolder(conView,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Participant participant = conversation.getParticipantsList().get(position);
        JSONObject participant_num = participant.getAttributes().getJSONObject();
        try {
            if(participant_num!=null) {
                String ContactName = cList.get(participant_num.getString("participant_number")) != null ? cList.get(participant_num.getString("participant_number")) : participant_num.getString("participant_number");
                holder.tvName.setText(ContactName);
                holder.tvNumber.setText(participant_num.getString("participant_number"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(!participant.getIdentity().equals("") && participant.getIdentity() != null){
            holder.tvName.setText(participant.getIdentity());
            holder.tvNumber.setText(participant.getIdentity());
        }
    }

    @Override
    public int getItemCount() {
        return conversation.getParticipantsList().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final View conView;
        private RemoveParticipantClickListener listener;
        public ImageView mDeletebutton;
        private TextView tvNumber,tvName;
        public ViewHolder(@NonNull View itemView,RemoveParticipantClickListener listener) {
            super(itemView);
            mDeletebutton = itemView.findViewById(R.id.delete_button);
            this.listener = listener;
            conView = itemView;
            mDeletebutton.setOnClickListener(this);
            tvNumber = itemView.findViewById(R.id.tv_number);
            tvName = itemView.findViewById(R.id.tv_name);
        }
        public void onClick(View view) {
            Participant participant = conversation.getParticipantsList().get(getAdapterPosition());
            listener.RemoveParticipant(participant,getAdapterPosition());
        }
    }
}
