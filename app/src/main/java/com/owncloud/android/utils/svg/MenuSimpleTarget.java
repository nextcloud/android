/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.svg;


import com.bumptech.glide.request.target.SimpleTarget;

/**
 * SimpleTarget for MenuItems.
 */
public abstract class MenuSimpleTarget<Z> extends SimpleTarget<Z>{

    private final int mIdMenuItem;

    public MenuSimpleTarget(int idMenuItem){
        super();
        this.mIdMenuItem=idMenuItem;
    }

    public int getIdMenuItem() {
        return mIdMenuItem;
    }
}
