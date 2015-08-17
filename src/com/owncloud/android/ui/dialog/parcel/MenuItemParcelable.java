package com.owncloud.android.ui.dialog.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.MenuItem;

public class MenuItemParcelable implements Parcelable {
    int menuItemId;

    String menuText;

    public MenuItemParcelable() {
    }

    public MenuItemParcelable(MenuItem menuItem) {
        menuItemId = menuItem.getItemId();
        menuText = menuItem.getTitle().toString();
        menuItem.getMenuInfo();
    }

    public MenuItemParcelable(Parcel read) {
        menuItemId = read.readInt();
    }

    public void setMenuItemId(int id) {
        menuItemId = id;
    }

    public int getMenuItemId() {
        return menuItemId;
    }

    public String getMenuText() {
        return menuText;
    }

    public void setMenuText(String menuText) {
        this.menuText = menuText;
    }

    public static final Parcelable.Creator<MenuItemParcelable> CREATOR =
            new Parcelable.Creator<MenuItemParcelable>() {

                @Override
                public MenuItemParcelable createFromParcel(Parcel source) {
                    return new MenuItemParcelable(source);
                }

                @Override
                public MenuItemParcelable[] newArray(int size) {
                    return new MenuItemParcelable[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(menuItemId);
    }
}
