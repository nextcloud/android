/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

/**
 * UI POJO for the storage path list.
 */
public class StoragePathItem {
    private int icon;
    private String name;
    private String path;

    public StoragePathItem(int icon, String name, String path) {
        this.icon = icon;
        this.name = name;
        this.path = path;
    }

    public int getIcon() {
        return this.icon;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
