package com.owncloud.android.ui;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.view.View;

import com.owncloud.android.R;

public class RadioButtonPreference extends CheckBoxPreference implements View.OnLongClickListener {
    
    public RadioButtonPreference(Context context) {
        super(context, null, android.R.attr.checkBoxPreferenceStyle);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }
  
    @Override
    public boolean onLongClick(View v) {
        return true;
    }
}
