/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2018 Bartosz Przybylski
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.theme;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.utils.ThemeUtils;

import static com.owncloud.android.utils.ThemeUtils.adjustLightness;

/**
 * @author Bartosz Przybylski
 */
public class NCTheme {

    private @ColorInt int mPrimaryColor;
    private @ColorInt int mPrimaryAccentColor;
    private @ColorInt int mPrimaryDarkColor;
    private @ColorInt int mElementColor;

    static public NCTheme construct(Context context, Account account) {
        OCCapability capability = ThemeUtils.getCapability(account);

        @ColorInt int primaryColor = getPrimaryColor(context, capability);
        @ColorInt int primaryAccent = getPrimaryAccent(primaryColor, context, capability);
        @ColorInt int primaryDark = getPrimaryDark(context, capability);
        @ColorInt int elementColor = getElementColor(primaryColor, context, capability);

        return new NCTheme(primaryColor, primaryAccent, primaryDark, elementColor);
    }

    static private @ColorInt int getPrimaryColor(Context context, OCCapability capability) {
        try {
            return Color.parseColor(capability.getServerColor());
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary);
        }
    }

    static private @ColorInt int getPrimaryAccent(int primaryColor, Context context, OCCapability capability) {
        try {
            float adjust = isDarkTheme(primaryColor) ? 0.1f : -0.1f;
            return adjustLightness(adjust, Color.parseColor(capability.getServerColor()), 0.35f);
        } catch (Exception e) {
            return context.getResources().getColor(R.color.color_accent);
        }
    }

    static private @ColorInt int getPrimaryDark(Context context, OCCapability capability) {
        try {
            return adjustLightness(-0.2f, Color.parseColor(capability.getServerColor()), -1f);
        } catch (Exception e) {
            return context.getResources().getColor(R.color.primary_dark);
        }
    }

    static private @ColorInt int getElementColor(@ColorInt int primaryColor, Context context, OCCapability capability) {
        try {
            return Color.parseColor(capability.getServerElementColor());
        } catch (Exception e) {
            float[] hsl = ThemeUtils.colorToHSL(primaryColor);

            if (hsl[2] > 0.8) {
                return context.getResources().getColor(R.color.elementFallbackColor);
            }
            return primaryColor;
        }
    }

    static private boolean isDarkTheme(@ColorInt int primaryColor) {
        float hsl[] = ThemeUtils.colorToHSL(primaryColor);
        return hsl[2] <= 0.55;
    }





    private NCTheme(@ColorInt int primaryColor, @ColorInt int primaryAccentColor, @ColorInt int primaryDarkColor,
                    @ColorInt int elementColor) {
        mPrimaryColor = primaryColor;
        mPrimaryAccentColor = primaryAccentColor;
        mPrimaryDarkColor = primaryDarkColor;
        mElementColor = elementColor;
    }

    public int getPrimaryAccentColor() {
        return mPrimaryAccentColor;
    }

    public int getPrimaryDarkColor() {
        return mPrimaryDarkColor;
    }

    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    public int getElementColor() {
        return mElementColor;
    }
}
