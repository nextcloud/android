package com.owncloud.android.ui;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.view.View;

public class LongClickableCheckBoxPreference extends CheckBoxPreference implements View.OnLongClickListener {

    public LongClickableCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    public boolean onLongClick(View v) {
        return true;
    }
}
