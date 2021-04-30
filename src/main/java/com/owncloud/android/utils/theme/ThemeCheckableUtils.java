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

import androidx.appcompat.app.AppCompatDelegate;
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

            int checkEnabled = MainApp.getAppContext().getResources().getColor(R.color.checkbox_checked_enabled);
            int checkDisabled = MainApp.getAppContext().getResources().getColor(R.color.checkbox_checked_disabled);
            int uncheckEnabled =
                MainApp.getAppContext().getResources().getColor(R.color.checkbox_unchecked_enabled);
            int uncheckDisabled =
                MainApp.getAppContext().getResources().getColor(R.color.checkbox_unchecked_disabled);

            if (ThemeColorUtils.darkTheme(MainApp.getAppContext()) &&
                AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                checkDisabled =
                    MainApp.getAppContext().getResources().getColor(R.color.checkbox_checked_disabled_dark);
                 uncheckDisabled =
                    MainApp.getAppContext().getResources().getColor(R.color.checkbox_unchecked_disabled_dark);
            }

            int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled, android.R.attr.state_checked}, // enabled and checked
                new int[]{-android.R.attr.state_enabled, android.R.attr.state_checked}, // disabled and checked
                new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked}, // enabled and unchecked
                new int[]{-android.R.attr.state_enabled, -android.R.attr.state_checked}  // disabled and unchecked
            };

            int[] colors = new int[]{
                checkEnabled,
                checkDisabled,
                uncheckEnabled,
                uncheckDisabled
            };

            ColorStateList checkColorStateList = new ColorStateList(states, colors);

            for (AppCompatCheckBox checkBox : checkBoxes) {
                CompoundButtonCompat.setButtonTintList(checkBox, checkColorStateList);
            }
        }
    }

    public static void tintSwitch(SwitchCompat switchView, int color) {

        int thumbColorCheckedEnabled = MainApp.getAppContext().getResources().getColor(R.color.switch_thumb_checked_enabled);
        int thumbColorUncheckedEnabled = MainApp.getAppContext().getResources().getColor(R.color.switch_thumb_unchecked_enabled);
        int thumbColorCheckedDisabled =
            MainApp.getAppContext().getResources().getColor(R.color.switch_thumb_checked_disabled);
        int thumbColorUncheckedDisabled =
            MainApp.getAppContext().getResources().getColor(R.color.switch_thumb_unchecked_disabled);

        if (ThemeColorUtils.darkTheme(MainApp.getAppContext()) &&
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            thumbColorCheckedDisabled =
                MainApp.getAppContext().getResources().getColor(R.color.switch_thumb_checked_disabled_dark);
            thumbColorUncheckedDisabled =
                MainApp.getAppContext().getResources().getColor(R.color.switch_thumb_unchecked_disabled_dark);
        }


        int[][] states = new int[][]{
            new int[]{android.R.attr.state_enabled, android.R.attr.state_checked}, // enabled and checked
            new int[]{-android.R.attr.state_enabled, android.R.attr.state_checked}, // disabled and checked
            new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked}, // enabled and unchecked
            new int[]{-android.R.attr.state_enabled, -android.R.attr.state_checked}  // disabled and unchecked
        };

        int[] thumbColors = new int[]{
            thumbColorCheckedEnabled,
            thumbColorCheckedDisabled,
            thumbColorUncheckedEnabled,
            thumbColorUncheckedDisabled
        };

        ColorStateList thumbColorStateList = new ColorStateList(states, thumbColors);

        int trackColorCheckedEnabled =
            MainApp.getAppContext().getResources().getColor(R.color.switch_track_checked_enabled);
        int trackColorUncheckedEnabled = MainApp.getAppContext().getResources().getColor(R.color.switch_track_unchecked_enabled);
        int trackColorCheckedDisabled =
            MainApp.getAppContext().getResources().getColor(R.color.switch_track_checked_disabled);
        int trackColorUncheckedDisabled =
            MainApp.getAppContext().getResources().getColor(R.color.switch_track_unchecked_disabled);

        if (ThemeColorUtils.darkTheme(MainApp.getAppContext()) &&
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            trackColorCheckedDisabled =
                MainApp.getAppContext().getResources().getColor(R.color.switch_track_checked_disabled_dark);
            trackColorUncheckedDisabled =
                MainApp.getAppContext().getResources().getColor(R.color.switch_track_unchecked_disabled_dark);
        }

        int[] trackColors = new int[]{
            trackColorCheckedEnabled,
            trackColorCheckedDisabled,
            trackColorUncheckedEnabled,
            trackColorUncheckedDisabled
        };
        ColorStateList trackColorStateList = new ColorStateList(states, trackColors);

        // setting the thumb color
        DrawableCompat.setTintList(switchView.getThumbDrawable(), thumbColorStateList);

        // setting the track color
        DrawableCompat.setTintList(switchView.getTrackDrawable(), trackColorStateList);
    }
}
