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

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;

/**
 * Utility class with methods for client side checkable theming.
 */
public final class ThemeMenuUtils {
    /**
     * Will change a menu item text tint
     *
     * @param item  the menu item object
     * @param color the wanted color (as resource or color)
     */
    public static void tintMenuItemText(MenuItem item, int color) {
        SpannableString newItemTitle = new SpannableString(item.getTitle());
        newItemTitle.setSpan(new ForegroundColorSpan(color), 0, newItemTitle.length(),
                             Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        item.setTitle(newItemTitle);
    }
}
