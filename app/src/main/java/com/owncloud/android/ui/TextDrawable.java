/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.accounts.Account;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.utils.BitmapUtils;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

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

    private boolean bigText = false;

    /**
     * Create a TextDrawable with the given radius.
     *
     * @param text   the text to be rendered
     * @param color  color
     * @param radius circle radius
     */
    public TextDrawable(String text, BitmapUtils.Color color, float radius) {
        mRadius = radius;
        mText = text;

        mBackground = new Paint();
        mBackground.setStyle(Paint.Style.FILL);
        mBackground.setAntiAlias(true);
        mBackground.setColor(Color.argb(color.a, color.r, color.g, color.b));

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(radius);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        setBounds(0, 0, (int) radius * 2, (int) radius * 2);
    }

    /**
     * creates an avatar in form of a TextDrawable with the first letter of the account name in a circle with the given
     * radius.
     *
     * @param user        user account
     * @param radiusInDp  the circle's radius
     * @return the avatar as a TextDrawable
     */
    @NonNull
    public static TextDrawable createAvatar(User user, float radiusInDp) {
        String username = UserAccountManager.getDisplayName(user.toPlatformAccount());
        return createNamedAvatar(username, radiusInDp);
    }

    /**
     * creates an avatar in form of a TextDrawable with the first letter of the account name in a circle with the given
     * radius.
     *
     * @param userId     userId to use
     * @param radiusInDp the circle's radius
     * @return the avatar as a TextDrawable
     */
    @NonNull
    public static TextDrawable createAvatarByUserId(String userId, float radiusInDp) {
        return createNamedAvatar(userId, radiusInDp);
    }

    /**
     * creates an avatar in form of a TextDrawable with the first letter of a name in a circle with the
     * given radius.
     *
     * @param name       the name
     * @param radiusInDp the circle's radius
     * @return the avatar as a TextDrawable
     */
    @NonNull
    public static TextDrawable createNamedAvatar(String name, float radiusInDp) {
        BitmapUtils.Color color = BitmapUtils.usernameToColor(name);
        return new TextDrawable(extractCharsFromDisplayName(name), color, radiusInDp);
    }

    @VisibleForTesting
    public static String extractCharsFromDisplayName(@NonNull String displayName) {
        if (displayName.isEmpty()) {
            return "";
        }

        String[] nameParts = displayName.split("\\s+");

        StringBuilder firstTwoLetters = new StringBuilder();
        for (int i = 0; i < Math.min(2, nameParts.length); i++) {
            firstTwoLetters.append(nameParts[i].substring(0, 1).toUpperCase(Locale.getDefault()));
        }

        return firstTwoLetters.toString();
    }

    /**
     * Draw in its bounds (set via setBounds) respecting optional effects such as alpha (set via setAlpha) and color
     * filter (set via setColorFilter) a circular background with a user's first character.
     *
     * @param canvas The canvas to draw into
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawCircle(mRadius, mRadius, mRadius, mBackground);

        if (bigText) {
            mTextPaint.setTextSize(1.8f * mRadius);
        }

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
