/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
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
package com.owncloud.android.utils.theme;

import android.content.Context;
import android.content.res.ColorStateList;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.DrawableRes;

/**
 * Utility class with methods for client side FAB theming.
 */
public final class ThemeFabUtils {
    public static void colorFloatingActionButton(FloatingActionButton button, @DrawableRes int drawable,
                                                 Context context) {
        int primaryColor = ThemeColorUtils.primaryColor(null, true, false, context);

        colorFloatingActionButton(button, context, primaryColor);
        button.setImageDrawable(ThemeDrawableUtils.tintDrawable(drawable,
                                                                ThemeColorUtils.getColorForPrimary(primaryColor,
                                                                                                   context)));
    }

    public static void colorFloatingActionButton(FloatingActionButton button, Context context) {
        colorFloatingActionButton(button, context, ThemeColorUtils.primaryColor(null, true, false, context));
    }

    public static void colorFloatingActionButton(FloatingActionButton button, Context context, int primaryColor) {
        colorFloatingActionButton(button, primaryColor, ThemeColorUtils.calculateDarkColor(primaryColor, context));
    }

    public static void colorFloatingActionButton(FloatingActionButton button, int backgroundColor, int rippleColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setRippleColor(rippleColor);
    }
}
