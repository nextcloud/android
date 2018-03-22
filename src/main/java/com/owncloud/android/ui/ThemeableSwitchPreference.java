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
import android.os.Build;
import android.preference.SwitchPreference;
import android.support.annotation.RequiresApi;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.owncloud.android.utils.ThemeUtils;


/**
 * Themeable switch preference
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

        if (view instanceof ViewGroup && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            findSwitch((ViewGroup) view);
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private void findSwitch(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            if (child instanceof Switch) {
                Switch switchView = (Switch) child;

                int color = ThemeUtils.primaryAccentColor(getContext());
                int trackColor = Color.argb(77, Color.red(color), Color.green(color), Color.blue(color));

                // setting the thumb color
                DrawableCompat.setTintList(switchView.getThumbDrawable(), new ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                        new int[]{color, Color.WHITE}));

                // setting the track color
                DrawableCompat.setTintList(switchView.getTrackDrawable(), new ColorStateList(
                        new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                        new int[]{trackColor, Color.parseColor("#4D000000")}));

                break;
            } else if (child instanceof ViewGroup) {
                findSwitch((ViewGroup) child);
            }
        }
    }
}
