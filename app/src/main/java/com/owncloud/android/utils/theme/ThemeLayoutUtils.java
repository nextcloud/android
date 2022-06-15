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

import com.google.android.material.tabs.TabLayout;
import com.owncloud.android.R;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Utility class with methods for client side checkable theming.
 */
public final class ThemeLayoutUtils {
    public static void colorSwipeRefreshLayout(Context context, SwipeRefreshLayout swipeRefreshLayout) {
        int primaryColor = ThemeColorUtils.primaryColor(context);
        int darkColor = ThemeColorUtils.primaryDarkColor(context);
        int accentColor = ThemeColorUtils.primaryAccentColor(context);

        swipeRefreshLayout.setColorSchemeColors(accentColor, primaryColor, darkColor);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.bg_elevation_one);
    }

    public static void colorTabLayout(Context context, TabLayout tabLayout) {
        int primaryColor = ThemeColorUtils.primaryColor(context, true);
        int textColor = context.getResources().getColor(R.color.text_color);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setSelectedTabIndicatorColor(primaryColor);
        tabLayout.setTabTextColors(textColor, primaryColor);
        tabLayout.setTabIconTint(new ColorStateList(
            new int[][]{
                new int[]{android.R.attr.state_selected},
                new int[]{android.R.attr.state_enabled},
                new int[]{-android.R.attr.state_enabled}
            },
            new int[]{
                primaryColor,
                textColor,
                Color.GRAY
            }
        ));
    }
}
