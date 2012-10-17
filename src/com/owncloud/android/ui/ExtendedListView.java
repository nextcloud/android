package com.owncloud.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * ListView allowing to specify the position of an item that should be centered in the visible area, if possible.
 * 
 * The cleanest way I found to overcome the problem due to getHeight() returns 0 until the view is really drawn. 
 *  
 * @author David A. Velasco
 */
public class ExtendedListView extends ListView {

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
    protected void onDraw (Canvas canvas) {
        super.onDraw(canvas);
        if (mPositionToSetAndCenter > 0) {
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
