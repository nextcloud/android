/**
 * ownCloud Android client application
 *
 * Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ActionEditText extends AppCompatEditText {
    private String s;
    private String optionOneString;
    private int optionOneColor;
    private String optionTwoString;
    private int optionTwoColor;
    private Rect mTextBounds, mButtonRect;

    private String badgeClickCallback;
    private Rect btn_rect;
    private static final String TAG = ActionEditText.class.getSimpleName();

    public ActionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        getAttrs(attrs);
        s = optionOneString;
        mTextBounds = new Rect();
        mButtonRect = new Rect();
    }

    public ActionEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getAttrs(attrs);
        s = optionOneString;
        mTextBounds = new Rect();
        mButtonRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint p = getPaint();

        p.getTextBounds(s, 0, s.length(), mTextBounds);

        getDrawingRect(mButtonRect);
        mButtonRect.top += 10;
        mButtonRect.bottom -= 10;
        mButtonRect.left = (int) (getWidth() - mTextBounds.width() - 18);
        mButtonRect.right = getWidth() - 10;
        btn_rect = mButtonRect;

        if (s.equals(optionOneString)) {
            p.setColor(optionOneColor);
        } else {
            p.setColor(optionTwoColor);
        }
        canvas.drawRect(mButtonRect, p);
        p.setColor(Color.GRAY);

        canvas.drawText(s, mButtonRect.left + 3, mButtonRect.bottom
                - (mTextBounds.height() / 2), p);

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int touchX = (int) event.getX();
        int touchY = (int) event.getY();
        boolean r = super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP && btn_rect.contains(touchX, touchY)) {
            if (s.equals(optionTwoString)) {
                s = optionOneString;
            } else {
                s = optionTwoString;
            }
            if (badgeClickCallback != null) {
                @SuppressWarnings("rawtypes")
                Class[] paramtypes = new Class[2];
                paramtypes[0] = android.view.View.class;
                paramtypes[1] = String.class;
                Method method;
                try {

                    method = getContext().getClass().getMethod(
                            badgeClickCallback, paramtypes);
                    method.invoke(getContext(), this, s);

                } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                    Log_OC.d(TAG, e.getMessage(), e);
                }

                invalidate();
            }
        }
        return r;
    }

    private void getAttrs(AttributeSet attr) {
        TypedArray a = getContext().obtainStyledAttributes(attr,
                R.styleable.ActionEditText);
        optionOneString = a
                .getString(R.styleable.ActionEditText_optionOneString);
        optionTwoString = a
                .getString(R.styleable.ActionEditText_optionTwoString);
        optionOneColor = a.getColor(R.styleable.ActionEditText_optionOneColor,
                0x00ff00);
        optionTwoColor = a.getColor(R.styleable.ActionEditText_optionTwoColor,
                0xff0000);
        badgeClickCallback = a
                .getString(R.styleable.ActionEditText_onBadgeClick);
        a.recycle();
    }

}
