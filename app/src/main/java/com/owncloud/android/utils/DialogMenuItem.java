/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2015 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

/**
 * Created by scherzia on 17.08.2015.
 */
public class DialogMenuItem implements MenuItem {
    int mItemId;
    CharSequence mTitle;

    public DialogMenuItem(int itemId) {
        this.mItemId = itemId;
    }

    @Override
    public int getItemId() {
        return mItemId;
    }

    @Override
    public int getGroupId() {
        return 0;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        this.mTitle = title;
        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        return this;
    }

    @Override
    public CharSequence getTitle() {
        return this.mTitle;
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        return null;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return null;
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        return null;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        return null;
    }

    @Override
    public Drawable getIcon() {
        return null;
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        return null;
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        return null;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        return null;
    }

    @Override
    public char getNumericShortcut() {
        return 0;
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        return null;
    }

    @Override
    public char getAlphabeticShortcut() {
        return 0;
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        return null;
    }

    @Override
    public boolean isCheckable() {
        return false;
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        return null;
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        return null;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean hasSubMenu() {
        return false;
    }

    @Override
    public SubMenu getSubMenu() {
        return null;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        return null;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return null;
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        // not used at the moment
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        return null;
    }

    @Override
    public MenuItem setActionView(View view) {
        return null;
    }

    @Override
    public MenuItem setActionView(int resId) {
        return null;
    }

    @Override
    public View getActionView() {
        return null;
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        return null;
    }

    @Override
    public ActionProvider getActionProvider() {
        return null;
    }

    @Override
    public boolean expandActionView() {
        return false;
    }

    @Override
    public boolean collapseActionView() {
        return false;
    }

    @Override
    public boolean isActionViewExpanded() {
        return false;
    }

    @Override
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        return null;
    }
}
