package com.owncloud.android.ui.components;

import androidx.annotation.DrawableRes;

public class UserInfoDetailsItem {
    @DrawableRes
    private int icon;
    private String text;
    private String iconContentDescription;
    private int tintColor;

    public UserInfoDetailsItem(@DrawableRes int icon, String text, String iconContentDescription, int tintColor) {
        this.icon = icon;
        this.text = text;
        this.iconContentDescription = iconContentDescription;
        this.tintColor = tintColor;
    }

    public int getIcon() {
        return icon;
    }

    public String getText() {
        return text;
    }

    public String getIconContentDescription() {
        return iconContentDescription;
    }

    public int getTintColor() {
        return tintColor;
    }
}
