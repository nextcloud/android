/*
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
import android.text.TextUtils;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.users.Status;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A Drawable object that draws a status
 */
@SuppressFBWarnings("PME_POOR_MANS_ENUM")
public class StatusDrawable extends Drawable {
    private String text;
    private @DrawableRes int icon = -1;
    private Paint textPaint;
    private Paint backgroundPaint;
    private final float radius;
    private Context context;
    private final static int whiteBackground = Color.argb(200, 255, 255, 255);
    private final static int onlineStatus = Color.argb(255, 73, 179, 130);

    public StatusDrawable(Status status, float statusSize, Context context) {
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        radius = statusSize;

        if (TextUtils.isEmpty(status.getIcon())) {
            switch (status.getStatus()) {
                case DND:
                    icon = R.drawable.ic_user_status_dnd;
                    backgroundPaint.setColor(whiteBackground);
                    this.context = context;
                    break;

                case ONLINE:
                    backgroundPaint.setColor(onlineStatus);
                    break;

                case AWAY:
                    icon = R.drawable.ic_user_status_away;
                    backgroundPaint.setColor(whiteBackground);
                    this.context = context;
                    break;

                default:
                    // do not show
                    backgroundPaint = null;
                    break;
            }
        } else {
            text = status.getIcon();

            backgroundPaint.setColor(whiteBackground);

            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(statusSize);
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }
    }

    /**
     * Draw in its bounds (set via setBounds) respecting optional effects such as alpha (set via setAlpha) and color
     * filter (set via setColorFilter) a circular background with a user's first character.
     *
     * @param canvas The canvas to draw into
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        if (backgroundPaint != null) {
            canvas.drawCircle(radius, radius, radius, backgroundPaint);
        }

        if (text != null) {
            textPaint.setTextSize(1.6f * radius);
            canvas.drawText(text, radius, radius - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint);
        }

        if (icon != -1) {
            Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), icon, null);

            if (drawable != null) {
                drawable.setBounds(0,
                                   0,
                                   (int) (2 * radius),
                                   (int) (2 * radius));
                drawable.draw(canvas);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
