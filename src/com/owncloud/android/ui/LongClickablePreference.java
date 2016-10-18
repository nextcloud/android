package com.owncloud.android.ui;

import android.content.Context;
import android.preference.Preference;
import android.view.View;

public class LongClickablePreference extends Preference implements View.OnLongClickListener {
    
    public LongClickablePreference(Context context) {
        super(context);
    }

    @Override
    public boolean onLongClick(View v) {
        return true;
    }
}