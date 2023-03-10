package com.glaciersecurity.glaciermessenger.ui;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.utils.PhoneHelper;

public class AboutPreference extends Preference {
	public AboutPreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		final String appName = context.getString(R.string.app_name);
		setSummary(appName +' '+ PhoneHelper.getVersionName(context));
//		setTitle(context.getString(R.string.title_activity_about_x, appName));
		setTitle(R.string.title_activity_about_x);
	}

	public AboutPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		final String appName = context.getString(R.string.app_name);
		setSummary(appName +' '+ PhoneHelper.getVersionName(context));
//		setTitle(context.getString(R.string.title_activity_about_x, appName));
		setTitle(R.string.title_activity_about_x);
	}

    @Override
    protected void onClick() {
        super.onClick();
        final Intent intent = new Intent(getContext(), AboutActivity.class);
        getContext().startActivity(intent);
    }
}

