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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.widget.Button;
import android.widget.ImageButton;

import com.owncloud.android.R;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

/**
 * Utility class with methods for client side button theming.
 */
public final class ThemeButtonUtils {
    /**
     * sets the tinting of the given ImageButton's icon to color_accent.
     *
     * @param imageButton the image button who's icon should be colored
     */
    public static void colorImageButton(ImageButton imageButton, @ColorInt int color) {
        if (imageButton != null) {
            imageButton.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    public static void colorPrimaryButton(Button button, Context context) {
        int primaryColor = ThemeColorUtils.primaryColor(null, true, false, context);
        int fontColor = ThemeColorUtils.fontColor(context, false);

        button.setBackgroundColor(primaryColor);

        if (Color.BLACK == primaryColor) {
            button.setTextColor(Color.WHITE);
        } else if (Color.WHITE == primaryColor) {
            button.setTextColor(Color.BLACK);
        } else {
            button.setTextColor(fontColor);
        }
    }

    public static void themeBorderlessButton(Button button, int primaryColor) {
        if (button == null) {
            return;
        }

        Context context = button.getContext();
        int disabledColor = ContextCompat.getColor(context, R.color.disabled_text);
        button.setTextColor(new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
            },
            new int[]{
                primaryColor,
                disabledColor
            }
        ));
    }
}
