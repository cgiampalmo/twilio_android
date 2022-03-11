package com.glaciersecurity.glaciermessenger.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.R;
import com.twilio.conversations.Conversation;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.viewHolder> {
    Activity activity;
    ArrayList<ContactModel> arrayList;
    private OnSMSConversationClickListener listener;

    public ContactAdapter(Activity activity,ArrayList<ContactModel> arrayList,OnSMSConversationClickListener listener){
        this.activity = activity;
        this.arrayList = arrayList;
        this.listener = listener;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact,parent,false);
        return new viewHolder(view,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        ContactModel model = arrayList.get(position);
        holder.tvName.setText(model.getName());
        holder.tvNumber.setText(model.getNumber());
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvName,tvNumber;
        private OnSMSConversationClickListener listener;

        viewHolder(@NonNull View itemView,OnSMSConversationClickListener listener) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvNumber = itemView.findViewById(R.id.tv_number);
            this.listener = listener;
            itemView.setOnClickListener(this);
        }
        public void onClick(View view) {
            int position = getAdapterPosition();
            ContactModel model = arrayList.get(position);
            listener.OnSMSConversationClick("",model.number);
        }
    }
}
