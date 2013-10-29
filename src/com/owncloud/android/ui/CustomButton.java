package com.owncloud.android.ui;

import com.owncloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
/**
 * @author masensio
 *
 * Button for customizing the button background
 */

public class CustomButton extends Button {
    
    public CustomButton(Context context) {
        super(context);
        
        boolean customButtons = getResources().getBoolean(R.bool.custom_buttons);
        if (customButtons)
        {
            this.setBackgroundResource(R.drawable.btn_default);
        }
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        boolean customButtons = getResources().getBoolean(R.bool.custom_buttons);
        if (customButtons)
        {
            this.setBackgroundResource(R.drawable.btn_default);
        }
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        boolean customButtons = getResources().getBoolean(R.bool.custom_buttons);
        if (customButtons)
        {
            this.setBackgroundResource(R.drawable.btn_default);
        }
    }

}
