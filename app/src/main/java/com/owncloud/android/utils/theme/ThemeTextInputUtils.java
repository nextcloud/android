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
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.owncloud.android.R;

import androidx.core.content.ContextCompat;

/**
 * Utility class with methods for client side text input theming.
 */
public final class ThemeTextInputUtils {
    /**
     * Sets the color of the (containerized) text input TextInputLayout to {@code color} for hint text, box stroke and
     * highlight color.
     *
     * @param textInputLayout   the TextInputLayout instance
     * @param textInputEditText the TextInputEditText child element
     * @param color             the color to be used for the hint text and box stroke
     */
    public static void colorTextInput(TextInputLayout textInputLayout, TextInputEditText textInputEditText, int color) {
        textInputEditText.setHighlightColor(color);
        colorTextInputLayout(textInputLayout, color);
    }

    /**
     * Sets the color of the  TextInputLayout to {@code color} for hint text and box stroke.
     *
     * @param textInputLayout the TextInputLayout instance
     * @param color           the color to be used for the hint text and box stroke
     */
    private static void colorTextInputLayout(TextInputLayout textInputLayout, int color) {
        textInputLayout.setBoxStrokeColor(color);
        textInputLayout.setDefaultHintTextColor(new ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_focused},
                new int[]{android.R.attr.state_focused},
            },
            new int[]{
                Color.GRAY,
                color
            }
        ));
    }

    public static void themeEditText(Context context, EditText editText, boolean themedBackground) {
        if (editText == null) {
            return;
        }

        int color = ContextCompat.getColor(context, R.color.text_color);

        if (themedBackground) {
            if (ThemeColorUtils.darkTheme(context)) {
                color = ContextCompat.getColor(context, R.color.themed_fg);
            } else {
                color = ContextCompat.getColor(context, R.color.themed_fg_inverse);
            }
        }

        setEditTextColor(context, editText, color);
    }

    public static void setEditTextColor(Context context, EditText editText, int color) {
        editText.setTextColor(color);
        editText.setHighlightColor(context.getResources().getColor(R.color.fg_contrast));
    }

    public static void colorEditText(EditText editText, int color) {
        if (editText != null) {
            editText.setTextColor(color);
            editText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }
}
