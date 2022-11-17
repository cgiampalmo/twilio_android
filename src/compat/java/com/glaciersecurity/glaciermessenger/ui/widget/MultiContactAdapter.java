package com.glaciersecurity.glaciermessenger.ui.widget;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import com.glaciersecurity.glaciermessenger.utils.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.ui.ContactModel;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

public class MultiContactAdapter extends RecyclerView.Adapter<MultiContactAdapter.MultiViewHolder> {
    Activity activity;
    ArrayList<ContactModel> arrayList;
    TextView tvEmpty;
    MultiViewModel multiViewModel;
    boolean isEnable = false;
    boolean isSelectAll = false;
    ArrayList<String> selectList = new ArrayList<>();

    public MultiContactAdapter (Activity activity,ArrayList<ContactModel> arrayList,TextView tvEmpty){
        this.activity = activity;
        this.arrayList = arrayList;
        this.tvEmpty = tvEmpty;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public MultiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multi_contact,parent,false);
        multiViewModel = new ViewModelProvider((FragmentActivity) activity).get(MultiViewModel.class);
        return new MultiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MultiViewHolder holder, int position) {
        ContactModel model = arrayList.get(position);
        holder.tvName.setText(model.getName());
        holder.tvNumber.setText(model.getNumber());

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!isEnable){
                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                            MenuInflater menuInflater = actionMode.getMenuInflater();
                            menuInflater.inflate(R.menu.select_all,menu);
                            return true;
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                            isEnable = true;
                            ClickItem(holder);
                            multiViewModel.getText().observe((LifecycleOwner) activity, new Observer<String>() {
                                @Override
                                public void onChanged(String s) {
                                    actionMode.setTitle(String.format("%s Selected",s));
                                }
                            });
                            return false;
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                            switch (menuItem.getItemId()){
                                case R.id.menu_select_all:
                                    if (selectList.size() == arrayList.size()){
                                        isSelectAll = false;
                                        selectList.clear();
                                    }else{
                                        isSelectAll = true;
                                        selectList.clear();
                                        for (ContactModel s : arrayList){
                                            selectList.add(s.getNumber());
                                        }
                                    }
                                    multiViewModel.setText(String.valueOf(selectList.size()));
                                    tvEmpty.setText(String.join(",",selectList));
                                    notifyDataSetChanged();
                                    break;
                            }
                            return true;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode actionMode) {
                            isEnable = false;
                            isSelectAll = false;
                            selectList.clear();
                            notifyDataSetChanged();
                        }
                    };
                    ((AppCompatActivity) view.getContext()).startActionMode(callback);
                }else{
                    ClickItem(holder);
                }
                return true;
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isEnable){
                    ClickItem(holder);
                }else{
                    Toast.makeText(activity, "Long Press to select contacts ", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if(isSelectAll){
            holder.ivCheckBox.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        }else{
            holder.ivCheckBox.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void ClickItem(MultiViewHolder holder) {
        ContactModel model = arrayList.get(holder.getAdapterPosition());
        holder.tvName.setText(model.getName());
        holder.tvNumber.setText(model.getNumber());
        String s = model.getNumber();
        if(holder.ivCheckBox.getVisibility() == View.GONE){
            holder.ivCheckBox.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.LTGRAY);
            selectList.add(s);
        }else{
            holder.ivCheckBox.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            selectList.remove(s);
        }
        multiViewModel.setText(String.valueOf(selectList.size()));
        Log.d("Glacier","selectList "+String.join(",",selectList));
        tvEmpty.setText(String.join(",",selectList));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class MultiViewHolder extends RecyclerView.ViewHolder{
        private TextView tvNumber,tvName;
        ImageView ivCheckBox;
        public MultiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tv_number);
            tvName = itemView.findViewById(R.id.tv_name);
            ivCheckBox = itemView.findViewById(R.id.iv_check_box);
        }
        //void bind(final )
    }
}
