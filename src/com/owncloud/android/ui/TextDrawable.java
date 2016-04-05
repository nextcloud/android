/**
 *   ownCloud Android client application
 *
 *   @author Andy Scherzinger
 *   @author Tobias Kaminsiky
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.owncloud.android.utils.BitmapUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * A Drawable object that draws text (1 letter) on top of a circular/filled background.
 */
public class TextDrawable extends Drawable {

    private String text;
    private Paint paint;
    private Paint bg;
    private float radius;
    private float textX;
    private float textY;

    /**
     * Create a TextDrawable with a standard radius.
     *
     * @param text the text to be rendered
     * @param r    rgb red value
     * @param g    rgb green value
     * @param b    rgb blue value
     */
    public TextDrawable(String text, int r, int g, int b) {
        init(text, r, g, b, 40);
    }

    /**
     * Create a TextDrawable with the given radius.
     *
     * @param text   the text to be rendered
     * @param r      rgb red value
     * @param g      rgb green value
     * @param b      rgb blue value
     * @param radius circle radius
     */
    public TextDrawable(String text, int r, int g, int b, float radius) {
        init(text, r, g, b, radius);
    }

    /**
     * initializes the TextDrawable.
     *
     * @param text   the text to be rendered
     * @param r      rgb red value
     * @param g      rgb green value
     * @param b      rgb blue value
     * @param radius circle radius
     */
    private void init(String text, int r, int g, int b, float radius) {
        this.radius = radius;
        this.text = text;
        this.textX = (float) (radius * 0.5);
        this.textY = (float) (radius * 1.5);

        bg = new Paint();
        bg.setStyle(Paint.Style.FILL);
        bg.setAntiAlias(true);
        bg.setColor(Color.rgb(r, g, b));

        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize((float) (radius * 1.5));
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(radius, radius, radius, bg);
        canvas.drawText(text, textX, textY, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * creates an avatar in form of  a TextDrawable with the first letter of the account name in a circle with the
     * given radius.
     *
     * @param accountName the account name
     * @param radiusInDp  the circle's radius
     * @return the avatar as a TextDrawable
     * @throws UnsupportedEncodingException if the charset is not supported when calculating the color values
     * @throws NoSuchAlgorithmException     if the specified algorithm is not available when calculating the color values
     */
    @NonNull
    public static TextDrawable createAvatar(String accountName, float radiusInDp)
            throws
            UnsupportedEncodingException,
            NoSuchAlgorithmException {
        int[] rgb = BitmapUtils.calculateRGB(accountName);
        TextDrawable avatar = new TextDrawable(
                accountName.substring(0, 1).toUpperCase(), rgb[0], rgb[1], rgb[2], radiusInDp);
        return avatar;
    }
}
