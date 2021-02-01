package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.content.Context;
import android.renderscript.ScriptGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.databinding.RadioBinding;
import com.glaciersecurity.glaciermessenger.ui.OpenVPNFragment;
import com.wefika.flowlayout.FlowLayout;

import java.util.Comparator;
import java.util.List;

public class ProfileSelectListAdapter<GlacierProfile> extends ArrayAdapter<GlacierProfile> {
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, OpenVPNFragment.GlacierProfile obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }



    public ProfileSelectListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, @NonNull GlacierProfile[] objects) {
        super(context, resource, objects);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull GlacierProfile[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, @NonNull List<GlacierProfile> objects) {
        super(context, resource, objects);
    }

    public ProfileSelectListAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<GlacierProfile> objects) {
        super(context, resource, textViewResourceId, objects);
    }


    @Override
    public void add(@Nullable GlacierProfile object) {
        super.add(object);
    }

    public void select(int position, @Nullable View view, @NonNull ViewGroup parent){
        for(int i = 0; i< parent.getChildCount();i++){
           View v =  parent.getChildAt(i);
               CheckedTextView checkedTextView = v.findViewById(R.id.profname);
               checkedTextView.setChecked(false);

        }
        CheckedTextView checkedTextView = view.findViewById(R.id.profname);
        checkedTextView.setChecked(true);
    }

    public void select(@Nullable View view){
        if (view != null) {
            CheckedTextView checkedTextView = view.findViewById(R.id.profname);
            checkedTextView.setChecked(true);
        }

    }





    @Override
    public void sort(@NonNull Comparator comparator) {
        super.sort(comparator);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
//        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

//        ViewHolder viewHolder;
//        RadioBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.radio, parent, false);
//        viewHolder = ViewHolder.get(binding);
//        viewHolder.checkedTextView = view.findViewById(R.id.profname);
//
//    return view;
        return super.getView(position, view, parent);
    }



    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
    }

    public void setParseProf(String str, int position, @Nullable View view, @NonNull ViewGroup parent) {
        CheckedTextView checkedTextView = view.findViewById(R.id.profname);
        checkedTextView.setText(str);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    private static class ViewHolder {
        private CheckedTextView checkedTextView;

        private ViewHolder() {

        }

        public static ViewHolder get(RadioBinding binding) {
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.checkedTextView = binding.getRoot().findViewById(R.id.profname);
            return viewHolder;
        }

    }

}
