package com.glaciersecurity.glaciermessenger.ui.widget;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MultiViewModel extends ViewModel {
    MutableLiveData<String> mutableLiveData = new MutableLiveData<>();
    public void setText(String s){
        mutableLiveData.setValue(s);
    }
    public MutableLiveData<String> getText(){
        return mutableLiveData;
    }
}
