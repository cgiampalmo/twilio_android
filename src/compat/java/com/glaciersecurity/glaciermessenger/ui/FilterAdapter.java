package com.glaciersecurity.glaciermessenger.ui;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.utils.Log;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.viewHolder> implements Filterable {
    private ArrayList aList;
    private ArrayList FullList;
    private OnSMSConversationClickListener listener;
    public FilterAdapter(OnSMSConversationClickListener Listener, ArrayList aList) {
        Log.d("Glacier","NewSMS FilterAdapter "+aList.size());
        this.listener = Listener;
        this.aList = aList;
        FullList = new ArrayList<>(aList);
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact,parent,false);
        return new FilterAdapter.viewHolder(view,listener);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        String number = aList.get(position).toString();
        Log.d("Glacier","NewSMS number"+number);
        holder.tvName.setText(number);
    }

    @Override
    public int getItemCount() {
        return aList.size();
    }

    @Override
    public Filter getFilter() {
        return Searched_Filter;
    }

    private Filter Searched_Filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList filteredList = new ArrayList();
            if(charSequence == null || charSequence.length() == 0) {
                filteredList.addAll(FullList);
            }
            else {
                String filterPattern = charSequence.toString().trim();
                for (Object number:FullList){
                    if(number.toString().contains(filterPattern)){
                        filteredList.add(number);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            aList.clear();
            aList.addAll((ArrayList) filterResults.values);
            notifyDataSetChanged();
        }
    };
    class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
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
            listener.OnSMSConversationClick("",aList.get(position).toString());
        }
    }
}
