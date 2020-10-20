/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.owncloud.android.utils.BitmapUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

/**
 * A Drawable object that draws a status
 */
public class StatusDrawable extends Drawable {
    private String mText = null;
    private @DrawableRes int icon = -1;
    private Paint mTextPaint;
    private final Paint mBackground;
    private final float radius;
    private Context context;

    public StatusDrawable(@DrawableRes int icon, float size, Context context) {
        radius = size;
        this.icon = icon;
        this.context = context;

        mBackground = new Paint();
        mBackground.setStyle(Paint.Style.FILL);
        mBackground.setAntiAlias(true);
        mBackground.setColor(Color.argb(200, 255, 255, 255));
    }

    public StatusDrawable(BitmapUtils.Color color, float size) {
        radius = size;

        mBackground = new Paint();
        mBackground.setStyle(Paint.Style.FILL);
        mBackground.setAntiAlias(true);
        mBackground.setColor(Color.argb(color.a, color.r, color.g, color.b));
    }

    public StatusDrawable(String icon, float size) {
        mText = icon;
        radius = size;

        mBackground = new Paint();
        mBackground.setStyle(Paint.Style.FILL);
        mBackground.setAntiAlias(true);
        mBackground.setColor(Color.argb(200, 255, 255, 255));

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(size);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Draw in its bounds (set via setBounds) respecting optional effects such as alpha (set via setAlpha) and color
     * filter (set via setColorFilter) a circular background with a user's first character.
     *
     * @param canvas The canvas to draw into
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mBackground != null) {
            canvas.drawCircle(radius, radius, radius, mBackground);
        }

        if (mText != null) {
            mTextPaint.setTextSize(1.6f * radius);
            canvas.drawText(mText, radius, radius - ((mTextPaint.descent() + mTextPaint.ascent()) / 2), mTextPaint);
        }

        if (icon != -1) {
            Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), icon, null);
            drawable.setBounds(0,
                               0,
                               (int) (2 * radius),
                               (int) (2 * radius));
            drawable.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mTextPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mTextPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
