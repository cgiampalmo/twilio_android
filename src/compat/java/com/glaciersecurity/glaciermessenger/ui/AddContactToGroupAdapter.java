package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.R;
import com.twilio.conversations.Conversation;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AddContactToGroupAdapter extends RecyclerView.Adapter<AddContactToGroupAdapter.ViewHolder>{
    AddParticipantClickListener listener;
    ArrayList<ContactModel> arrayList;
    ConversationModel activity;

    public AddContactToGroupAdapter(AddParticipantClickListener listener, ArrayList arrayList, ConversationModel activity) {
        this.listener = listener;
        this.arrayList = arrayList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public AddContactToGroupAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //return null;
        View conView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_contact_group, parent, false);
        return new AddContactToGroupAdapter.ViewHolder(conView,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull AddContactToGroupAdapter.ViewHolder holder, int position) {
        ContactModel model = activity.getGroupArrayList().get(position);
        Log.d("Glacier","participant_num group array list"+model.getName());
        holder.tvName.setText(model.getName());
        holder.tvNumber.setText(model.getNumber());
    }

    @Override
    public int getItemCount() {
        return activity.getGroupArrayList().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final View conView;
        private AddParticipantClickListener listener;
        public ImageView mAddbutton;
        private TextView tvNumber,tvName;
        public ViewHolder(@NonNull View itemView,AddParticipantClickListener listener) {
            super(itemView);
            mAddbutton = itemView.findViewById(R.id.add_button);
            this.listener = listener;
            conView = itemView;
            mAddbutton.setOnClickListener(this);
            tvNumber = itemView.findViewById(R.id.tv_number);
            tvName = itemView.findViewById(R.id.tv_name);
        }
        public void onClick(View view) {
            ArrayList<ContactModel> aList = activity.getGroupArrayList();
            ContactModel cModel = aList.get(getAdapterPosition());
            String number = cModel.getNumber();
            listener.AddParticipant(number,getAdapterPosition());
        }
    }
}
