package com.owncloud.android.ui.dialog.parcel;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.MenuItem;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class MenuItemParcelable implements Parcelable {
    @Getter @Setter int menuItemId;
    @Getter @Setter String menuText;

    public MenuItemParcelable(MenuItem menuItem) {
        menuItemId = menuItem.getItemId();
        menuText = menuItem.getTitle().toString();
    }

    public MenuItemParcelable(Parcel read) {
        menuItemId = read.readInt();
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
