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

import android.content.res.ColorStateList;
import android.graphics.Color;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;

/**
 * Utility class with methods for client side checkable theming.
 */
public final class ThemeCheckableUtils {
    public static void tintCheckbox(int color, AppCompatCheckBox... checkBoxes) {
        if (checkBoxes != null) {
            for (AppCompatCheckBox checkBox : checkBoxes) {
                CompoundButtonCompat.setButtonTintList(checkBox, new ColorStateList(
                    new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked},
                    },
                    new int[]{
                        Color.GRAY,
                        color
                    }
                ));
            }
        }
    }

    public static void tintSwitch(SwitchCompat switchView, int color) {
        int trackColor = Color.argb(77, Color.red(color), Color.green(color), Color.blue(color));

        // setting the thumb color
        DrawableCompat.setTintList(switchView.getThumbDrawable(), new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{color, Color.WHITE}));

        // setting the track color
        DrawableCompat.setTintList(switchView.getTrackDrawable(), new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{trackColor, MainApp.getAppContext().getResources().getColor(R.color.switch_track_color_unchecked)}));
    }
}
