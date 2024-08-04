/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.decoration;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;

/**
 * Decoration for media grid items.
 */
public class MediaGridItemDecoration extends ItemDecoration {
    private int space;

    public MediaGridItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.right = space;
        outRect.bottom = space;
        outRect.left = space;
        outRect.top = space;
    }
}
