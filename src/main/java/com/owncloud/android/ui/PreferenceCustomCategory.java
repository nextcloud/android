package com.owncloud.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.owncloud.android.R;

public class PreferenceCustomCategory  extends PreferenceCategory {
    public PreferenceCustomCategory(Context context) {
        super(context);
    }

    public PreferenceCustomCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceCustomCategory(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(getContext().getResources().getColor(R.color.text_color));
        titleView.setTypeface(null, Typeface.BOLD);
    }
}
