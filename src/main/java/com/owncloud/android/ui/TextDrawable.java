/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Tobias Kaminsiky
 * Copyright (C) 2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.utils.BitmapUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * A Drawable object that draws text (1 character) on top of a circular/filled background.
 */
public class TextDrawable extends Drawable {
    /**
     * the text to be rendered.
     */
    private String mText;

    /**
     * the text paint to be rendered.
     */
    private Paint mTextPaint;

    /**
     * the background to be rendered.
     */
    private Paint mBackground;

    /**
     * the radius of the circular background to be rendered.
     */
    private float mRadius;

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
        mRadius = radius;
        mText = text;

        mBackground = new Paint();
        mBackground.setStyle(Paint.Style.FILL);
        mBackground.setAntiAlias(true);
        mBackground.setColor(Color.rgb(r, g, b));

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(radius);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * creates an avatar in form of a TextDrawable with the first letter of the account name in a circle with the
     * given radius.
     *
     * @param accountName the account name
     * @param radiusInDp  the circle's radius
     * @return the avatar as a TextDrawable
     * @throws UnsupportedEncodingException if the charset is not supported when calculating the color values
     * @throws NoSuchAlgorithmException     if the specified algorithm is not available when calculating the color values
     */
    @NonNull
    public static TextDrawable createAvatar(String accountName, float radiusInDp) throws
            UnsupportedEncodingException, NoSuchAlgorithmException {
        String username = AccountUtils.getAccountUsername(accountName);
        return createNamedAvatar(username, radiusInDp);
    }

    /**
     * creates an avatar in form of a TextDrawable with the first letter of a name in a circle with the
     * given radius.
     *
     * @param name       the name
     * @param radiusInDp the circle's radius
     * @return the avatar as a TextDrawable
     * @throws UnsupportedEncodingException if the charset is not supported when calculating the color values
     * @throws NoSuchAlgorithmException     if the specified algorithm is not available when calculating the color values
     */
    @NonNull
    public static TextDrawable createNamedAvatar(String name, float radiusInDp) throws
            UnsupportedEncodingException, NoSuchAlgorithmException {
        int[] rgb = BitmapUtils.calculateRGB(name);
        TextDrawable avatar = new TextDrawable(
                name.substring(0, 1).toUpperCase(), rgb[0], rgb[1], rgb[2], radiusInDp);
        return avatar;
    }

    /**
     * Draw in its bounds (set via setBounds) respecting optional effects such as alpha (set via setAlpha) and color
     * filter (set via setColorFilter) a circular background with a user's first character.
     *
     * @param canvas The canvas to draw into
     */
    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(mRadius, mRadius, mRadius, mBackground);
        canvas.drawText(mText, mRadius, mRadius - ((mTextPaint.descent() + mTextPaint.ascent()) / 2), mTextPaint);
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
