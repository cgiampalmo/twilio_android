package com.glaciersecurity.glaciermessenger.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.glaciersecurity.glaciermessenger.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NumberListAdapter extends RecyclerView.Adapter<NumberListAdapter.viewHolder> {
    PurchaseNumbers purchaseNumbers;
    public NumberListAdapter(PurchaseNumbers purchaseNumbers) {
        this.purchaseNumbers = purchaseNumbers;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contacts_list_item,parent,false);
        return new NumberListAdapter.viewHolder(view);
    }

    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        PurchaseNumbers.phone_num_details model = purchaseNumbers.availablePhoneNumbers.get(position);
        holder.tvNumber.setText(model.getPhone_number());
    }

    @Override
    public int getItemCount() {
        if(purchaseNumbers.availablePhoneNumbers != null)
            return purchaseNumbers.availablePhoneNumbers.size();
        return 0;
    }

    public class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvNumber;
        public viewHolder(View view) {
            super(view);
            tvNumber = view.findViewById(R.id.tv_number);
            view.setOnClickListener(this);
        }
        public void onClick(View view) {
            int position = getAdapterPosition();
            PurchaseNumbers.phone_num_details model = purchaseNumbers.availablePhoneNumbers.get(position);
            purchaseNumbers.OnNumberClick(model.getPhone_number());
        }
    }
}
