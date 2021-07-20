/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2012-2015 ownCloud Inc.
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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ListView;

import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * ListView allowing to specify the position of an item that should be centered in the visible area, if possible.
 *
 * The cleanest way I found to overcome the problem due to getHeight() returns 0 until the view is really drawn.
 */
public class ExtendedListView extends ListView {

    private static final String TAG = ExtendedListView.class.getSimpleName();

    private int mPositionToSetAndCenter;

    public ExtendedListView(Context context) {
        super(context);
    }

    public ExtendedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * {@inheritDoc}
     *
     *
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPositionToSetAndCenter > 0) {
            Log_OC.v(TAG, "Centering around position " + mPositionToSetAndCenter);
            this.setSelectionFromTop(mPositionToSetAndCenter, getHeight() / 2);
            mPositionToSetAndCenter = 0;
        }
    }

    /**
     * Public method to set the position of the item that should be centered in the visible area of the view.
     *
     * The position is saved here and checked in onDraw().
     *
     * @param position         Position (in the list of items) of the item to center in the visible area.     
     */
    public void setAndCenterSelection(int position) {
        mPositionToSetAndCenter = position;
    }

}