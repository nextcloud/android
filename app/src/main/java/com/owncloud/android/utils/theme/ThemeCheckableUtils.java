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

import com.owncloud.android.R;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.CompoundButtonCompat;

/**
 * Utility class with methods for client side checkable theming.
 */
public final class ThemeCheckableUtils {
    public void tintCheckbox(int color, AppCompatCheckBox... checkBoxes) {
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

    public void tintSwitch(SwitchCompat switchView, ThemeColorUtils themeColorUtils) {
        int trackColor = switchView.getContext().getResources().getColor(R.color.grey_200);
        ColorStateList thumbColorStateList;
        ColorStateList trackColorStateList;
        int thumbColor = themeColorUtils.primaryAccentColor(switchView.getContext());
        if (themeColorUtils.darkTheme(switchView.getContext()) &&
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            thumbColor = Color.WHITE;
            trackColor = Color.DKGRAY;
        }
        thumbColorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{thumbColor, switchView.getContext().getResources().getColor(R.color.switch_thumb_color_unchecked)});
        trackColorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked},
                new int[]{}},
            new int[]{trackColor, trackColor});
        DrawableCompat.setTintList(switchView.getThumbDrawable(), thumbColorStateList);
        DrawableCompat.setTintList(switchView.getTrackDrawable(), trackColorStateList);
    }
}
