/**
 * ownCloud Android client application
 *
 * @author Lorensius. W. T
 * Copyright (C) 2011  Bartek Przybylski
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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.PopupWindow;

/**
 * Represents a custom PopupWindows
 */
public class CustomPopup {
    protected final View mAnchor;
    protected final PopupWindow mWindow;
    private View root;
    private Drawable background = null;
    protected final WindowManager mWManager;

    public CustomPopup(View anchor) {
        mAnchor = anchor;
        mWindow = new PopupWindow(anchor.getContext());

        mWindow.setTouchInterceptor(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    CustomPopup.this.dismiss();
                    return true;
                }
                return false;
            }
        });

        mWManager = (WindowManager) anchor.getContext().getSystemService(
                Context.WINDOW_SERVICE);
        onCreate();
    }

    public void onCreate() {
        // not used at the moment
    }

    public void onShow() {
        // not used at the moment
    }

    public void preShow() {
        if (root == null) {
            throw new IllegalStateException(
                    "setContentView called with a view to display");
        }

        onShow();

        if (background == null) {
            mWindow.setBackgroundDrawable(new BitmapDrawable());
        } else {
            mWindow.setBackgroundDrawable(background);
        }

        mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mWindow.setTouchable(true);
        mWindow.setFocusable(true);
        mWindow.setOutsideTouchable(true);

        mWindow.setContentView(root);
    }

    public void setBackgroundDrawable(Drawable background) {
        this.background = background;
    }

    public void setContentView(View root) {
        this.root = root;
        mWindow.setContentView(root);
    }

    public void setContentView(int layoutResId) {
        LayoutInflater inflater = (LayoutInflater) mAnchor.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(inflater.inflate(layoutResId, null));
    }

    public void showDropDown() {
        showDropDown(0, 0);
    }

    public void showDropDown(int x, int y) {
        preShow();
        mWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        mWindow.showAsDropDown(mAnchor, x, y);
    }

    public void showLikeQuickAction() {
        showLikeQuickAction(0, 0);
    }

    public void showLikeQuickAction(int x, int y) {
        preShow();

        mWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        int[] location = new int[2];
        mAnchor.getLocationOnScreen(location);

        Rect anchorRect = new Rect(location[0], location[1], location[0]
                + mAnchor.getWidth(), location[1] + mAnchor.getHeight());

        root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        int rootW = root.getWidth(), rootH = root.getHeight();
        int screenW = mWManager.getDefaultDisplay().getWidth();

        int xpos = ((screenW - rootW) / 2) + x;
        int ypos = anchorRect.top - rootH + y;

        if (rootH > anchorRect.top) {
            ypos = anchorRect.bottom + y;
        }
        mWindow.showAtLocation(mAnchor, Gravity.NO_GRAVITY, xpos, ypos);
    }

    public void dismiss() {
        mWindow.dismiss();
    }

}
