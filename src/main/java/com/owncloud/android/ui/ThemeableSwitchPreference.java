/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
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
package com.owncloud.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.drawable.DrawableCompat;


/**
 * Themeable switch preference
 * TODO Migrate to androidx
 */
public class ThemeableSwitchPreference extends SwitchPreference {

    public ThemeableSwitchPreference(Context context) {
        super(context);
    }

    public ThemeableSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ThemeableSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (view instanceof ViewGroup) {
            findSwitch((ViewGroup) view);
        }
    }

    private void findSwitch(ViewGroup viewGroup) {
        ColorStateList thumbColorStateList = null;
        ColorStateList trackColorStateList = null;

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            if (child instanceof Switch) {
                Switch switchView = (Switch) child;

                if(thumbColorStateList == null && trackColorStateList == null) {
                    int thumbColor = ThemeColorUtils.primaryAccentColor(getContext());
                    if (ThemeColorUtils.darkTheme(getContext()) &&
                        AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                        thumbColor = Color.WHITE;
                    }
                    int trackColor = Color.argb(77, Color.red(thumbColor), Color.green(thumbColor), Color.blue(thumbColor));
                    int trackColorUnchecked = getContext().getResources().getColor(R.color.switch_track_color_unchecked);
                    thumbColorStateList = new ColorStateList(
                            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                            new int[]{thumbColor, getContext().getResources().getColor(R.color.switch_thumb_color_unchecked)});
                    trackColorStateList = new ColorStateList(
                            new int[][]{new int[]{android.R.attr.state_checked},
                                new int[]{}},
                            new int[]{trackColor, trackColorUnchecked});
                }

                // setting the thumb color
                DrawableCompat.setTintList(switchView.getThumbDrawable(), thumbColorStateList);

                // setting the track color
                DrawableCompat.setTintList(switchView.getTrackDrawable(), trackColorStateList);

                break;
            } else if (child instanceof ViewGroup) {
                findSwitch((ViewGroup) child);
            }
        }
    }
}
