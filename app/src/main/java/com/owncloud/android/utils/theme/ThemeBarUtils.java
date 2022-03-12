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
import android.graphics.PorterDuff;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import androidx.annotation.ColorInt;

/**
 * Utility class with methods for client side button theming.
 */
public final class ThemeBarUtils {
    /**
     * sets the coloring of the given progress bar to given color.
     *
     * @param progressBar the progress bar to be colored
     * @param color       the color to be used
     */
    public static void colorHorizontalProgressBar(ProgressBar progressBar, @ColorInt int color) {
        if (progressBar != null) {
            progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * sets the coloring of the given progress bar's progress to given color.
     *
     * @param progressBar the progress bar to be colored
     * @param color       the color to be used
     */
    public static void colorProgressBar(ProgressBar progressBar, @ColorInt int color) {
        if (progressBar != null) {
            progressBar.setProgressTintList(ColorStateList.valueOf(color));
        }
    }

    /**
     * sets the coloring of the given seek bar to color_accent.
     *
     * @param seekBar the seek bar to be colored
     */
    public static void colorHorizontalSeekBar(SeekBar seekBar, Context context) {
        int color = ThemeColorUtils.primaryAccentColor(context);

        colorHorizontalProgressBar(seekBar, color);
        seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static void themeProgressBar(Context context, ProgressBar progressBar) {
        // TODO harmonize methods
        int color = ThemeColorUtils.primaryAccentColor(context);
        progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
