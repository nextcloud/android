/**
 * ownCloud Android client application
 *
 * @author masensio
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

public class NavigationDrawerItem {

    private String mTitle;
    private String mContentDescription;
    private int mIcon;

    // Constructors
    public NavigationDrawerItem() {
    }

    public NavigationDrawerItem(String title) {
        mTitle = title;
    }

    public NavigationDrawerItem(String title, String contentDescription, int icon) {
        mTitle = title;
        mContentDescription = contentDescription;
        mIcon = icon;
    }

    // Getters and Setters
    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getContentDescription() {
        return mContentDescription;
    }

    public void setContentDescription(String contentDescription) {
        this.mContentDescription = contentDescription;
    }

    public int getIcon() {
        return mIcon;
    }

    public void setIcon(int icon) {
        this.mIcon = icon;
    }
}
